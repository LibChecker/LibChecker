package com.absinthe.libchecker.domain.home.presentation

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.app.repository.AppListRepository
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.home.recent.RecentVisit
import com.absinthe.libchecker.domain.home.recent.RecentVisitsRepository
import com.absinthe.libchecker.domain.home.ui.view.RecentVisitGroup
import com.absinthe.libchecker.domain.home.ui.view.RecentVisitItem
import com.absinthe.libchecker.domain.home.ui.view.RecentVisitLists
import com.absinthe.libchecker.domain.statistics.reference.model.resolveReferenceIcon
import com.absinthe.libchecker.domain.statistics.reference.repository.PermissionLabelResolver
import com.absinthe.libchecker.utils.extensions.dpToDimension
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class RecentVisitsViewModel(
  private val context: Context,
  private val repository: RecentVisitsRepository,
  private val installedApps: InstalledAppRepository,
  private val appList: AppListRepository,
  private val permissionLabels: PermissionLabelResolver
) : ViewModel() {
  private val _items = MutableStateFlow<RecentVisitLists?>(null)
  val items = _items.asStateFlow()
  private var observation: Job? = null

  fun refresh() {
    observation?.cancel()
    observation = viewModelScope.launch {
      repository.revision.collectLatest {
        _items.value = withContext(Dispatchers.IO) {
          RecentVisitLists(loadGroup(false), loadGroup(true))
        }
      }
    }
  }

  fun record(visit: RecentVisit) {
    viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { repository.record(visit) }
  }

  fun remove(visit: RecentVisit) {
    viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { repository.remove(visit) }
  }

  fun pin(item: RecentVisitItem) {
    viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { repository.pin(item.visit) }
  }

  private suspend fun loadGroup(libraries: Boolean): RecentVisitGroup {
    val pinned = repository.pinned(libraries)
    val recent = repository.recent(libraries).filterNot { visit -> pinned.any(visit::sameDestination) }
    return RecentVisitGroup(loadItems(pinned), loadItems(recent))
  }

  private suspend fun loadItems(visits: List<RecentVisit>): List<RecentVisitItem> {
    val result = mutableListOf<RecentVisitItem>()
    for (visit in visits) {
      try {
        if (visit.isLibrary) {
          val type = requireNotNull(visit.type)
          val rule = RulesRepository.getRule(visit.name, type, true)
          val iconRes = resolveReferenceIcon(visit.name, type, rule)
          val icon = ContextCompat.getDrawable(context, iconRes) ?: continue
          val permissionLabel = if (type == PERMISSION) permissionLabels.resolve(visit.name) else null
          result += RecentVisitItem(
            visit,
            rule?.label?.takeIf { it.isNotBlank() } ?: permissionLabel ?: visit.label ?: visit.name,
            icon,
            tintIcon = rule?.isSimpleColorIcon == true || iconRes == R.drawable.ic_question
          )
        } else {
          val info = installedApps.getPackageInfo(visit.name)?.applicationInfo
          val app = appList.getItem(visit.name)
          if (info == null || app?.isArchived == true) {
            repository.remove(visit)
            continue
          }
          val size = context.dpToDimension(32f).roundToInt().coerceAtLeast(1)
          val icon = info.loadIcon(context.packageManager).toBitmap(size, size)
          result += RecentVisitItem(
            visit,
            app?.label ?: info.loadLabel(context.packageManager).toString(),
            BitmapDrawable(context.resources, icon),
            app
          )
        }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        Timber.w("Could not prepare a recent visit")
      }
    }
    return result
  }
}
