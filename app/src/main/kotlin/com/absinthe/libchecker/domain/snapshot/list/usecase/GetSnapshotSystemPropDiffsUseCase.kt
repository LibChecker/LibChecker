package com.absinthe.libchecker.domain.snapshot.list.usecase

import android.os.Build
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.utils.fromJson

class GetSnapshotSystemPropDiffsUseCase(
  private val repository: SnapshotRepository,
  private val currentSystemProps: () -> Map<String, String> = ::getCurrentSystemProps
) {

  suspend operator fun invoke(timestamp: Long): List<SnapshotSystemPropDiff> {
    val snapshotSystemProps = runCatching {
      repository.getTimeStamp(timestamp)?.systemProps?.fromJson<Map<String, String>>()
    }.getOrNull().orEmpty()
    if (snapshotSystemProps.isEmpty()) {
      return emptyList()
    }

    val currentProps = currentSystemProps()
    return SnapshotSystemProp.entries.mapNotNull { prop ->
      val previous = snapshotSystemProps[prop.key] ?: return@mapNotNull null
      val current = currentProps[prop.key] ?: return@mapNotNull null
      if (previous == current) {
        null
      } else {
        SnapshotSystemPropDiff(prop, previous, current)
      }
    }
  }
}

private fun getCurrentSystemProps(): Map<String, String> {
  return mapOf(
    Constants.SystemProps.RO_BUILD_ID to Build.ID,
    Constants.SystemProps.RO_BUILD_VERSION_SECURITY_PATCH to Build.VERSION.SECURITY_PATCH
  )
}

data class SnapshotSystemPropDiff(
  val prop: SnapshotSystemProp,
  val previous: String,
  val current: String
)

enum class SnapshotSystemProp(
  val key: String
) {
  BUILD_ID(Constants.SystemProps.RO_BUILD_ID),
  SECURITY_PATCH(Constants.SystemProps.RO_BUILD_VERSION_SECURITY_PATCH)
}
