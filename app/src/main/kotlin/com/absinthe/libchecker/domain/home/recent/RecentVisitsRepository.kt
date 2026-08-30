package com.absinthe.libchecker.domain.home.recent

import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.StateFlow

@JsonClass(generateAdapter = true)
data class RecentVisit(
  val name: String,
  val type: Int? = null,
  val label: String? = null,
  val referredList: List<String>? = null
) {
  val isLibrary: Boolean get() = type != null

  fun sameDestination(other: RecentVisit): Boolean = name == other.name && type == other.type
}

interface RecentVisitsRepository {
  val revision: StateFlow<Int>
  suspend fun recent(libraries: Boolean): List<RecentVisit>
  suspend fun pinned(libraries: Boolean): List<RecentVisit>
  suspend fun pin(visit: RecentVisit)
  suspend fun record(visit: RecentVisit)
  suspend fun remove(visit: RecentVisit)
}
