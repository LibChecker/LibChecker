package com.absinthe.libchecker.data.home

import android.content.SharedPreferences
import androidx.core.content.edit
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.domain.home.recent.RecentVisit
import com.absinthe.libchecker.domain.home.recent.RecentVisitsRepository
import com.absinthe.libchecker.utils.JsonUtil
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class LocalRecentVisitsRepository(private val preferences: () -> SharedPreferences) : RecentVisitsRepository {
  private val mutex = Mutex()
  private val adapter = JsonUtil.moshi.adapter<List<RecentVisit>>(
    Types.newParameterizedType(List::class.java, RecentVisit::class.java)
  )
  private val _revision = MutableStateFlow(0)
  override val revision = _revision.asStateFlow()

  override suspend fun recent(libraries: Boolean): List<RecentVisit> = withContext(Dispatchers.IO) {
    mutex.withLock { read(libraries) }
  }

  override suspend fun pinned(libraries: Boolean): List<RecentVisit> = withContext(Dispatchers.IO) {
    mutex.withLock { read(libraries, pinned = true) }
  }

  override suspend fun pin(visit: RecentVisit) {
    if (!visit.isValid()) return
    update(visit, true) { listOf(visit) + it.filterNot(visit::sameDestination) }
  }

  override suspend fun record(visit: RecentVisit) {
    if (!visit.isValid()) return
    update(visit, false) { listOf(visit) + it.filterNot(visit::sameDestination) }
  }

  override suspend fun remove(visit: RecentVisit) {
    update(visit, false, true) { it.filterNot(visit::sameDestination) }
  }

  private suspend fun update(visit: RecentVisit, vararg pinned: Boolean, transform: (List<RecentVisit>) -> List<RecentVisit>) {
    // Commit all affected sections together, even if the owning ViewModel is cleared.
    withContext(Dispatchers.IO + NonCancellable) {
      mutex.withLock {
        val changes = pinned.asIterable().mapNotNull { isPinned ->
          val previous = read(visit.isLibrary, isPinned)
          val next = transform(previous).let { if (isPinned) it else it.take(LIMIT) }
          if (next == previous) null else key(visit.isLibrary, isPinned) to adapter.toJson(next)
        }
        if (changes.isNotEmpty()) {
          preferences().edit { changes.forEach { (key, json) -> putString(key, json) } }
          _revision.value++
        }
      }
    }
  }

  private fun read(libraries: Boolean, pinned: Boolean = false): List<RecentVisit> {
    val json = preferences().getString(key(libraries, pinned), null) ?: return emptyList()
    return try {
      adapter.fromJson(json).orEmpty()
        .filter { it.isLibrary == libraries && it.isValid() }
        .distinctBy { it.type to it.name }
        .let { if (pinned) it else it.take(LIMIT) }
    } catch (_: Exception) {
      Timber.w("Ignoring invalid recent visits")
      emptyList()
    }
  }

  private fun RecentVisit.isValid(): Boolean = name.isNotBlank() && (type == null || type in NATIVE..ACTION)

  private fun key(libraries: Boolean, pinned: Boolean = false): String {
    val prefix = if (pinned) "pinned" else "recent"
    return if (libraries) "${prefix}_libraries_v1" else "${prefix}_apps_v1"
  }

  private companion object {
    const val LIMIT = 5
  }
}
