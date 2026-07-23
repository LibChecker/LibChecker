package com.absinthe.libchecker.domain.snapshot.list.usecase

import android.content.Context
import android.os.Build
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.list.model.buildSnapshotSystemPropDisplayData
import com.absinthe.libchecker.utils.fromJson

class BuildSnapshotSystemPropDisplayDataUseCase(
  private val context: Context,
  private val repository: SnapshotRepository,
  private val currentSystemProps: () -> Map<String, String> = ::getCurrentSystemProps
) {

  suspend operator fun invoke(timestamp: Long) = buildList {
    val snapshotSystemProps = runCatching {
      repository.getTimeStamp(timestamp)?.systemProps?.fromJson<Map<String, String>>()
    }.getOrNull().orEmpty()
    if (snapshotSystemProps.isEmpty()) return@buildList

    val currentProps = currentSystemProps()
    SnapshotSystemProp.entries.forEach { prop ->
      val previous = snapshotSystemProps[prop.key] ?: return@forEach
      val current = currentProps[prop.key] ?: return@forEach
      if (previous != current) {
        add(
          buildSnapshotSystemPropDisplayData(
            label = getSystemPropLabel(prop),
            displayValue = "$previous $ARROW $current"
          )
        )
      }
    }
  }

  private fun getSystemPropLabel(prop: SnapshotSystemProp): String {
    return when (prop) {
      SnapshotSystemProp.BUILD_ID -> context.getString(R.string.snapshot_build_id)
      SnapshotSystemProp.SECURITY_PATCH -> context.getString(R.string.snapshot_build_security_patch)
    }
  }

  private companion object {
    const val ARROW = "→"
  }
}

private fun getCurrentSystemProps(): Map<String, String> {
  return mapOf(
    Constants.SystemProps.RO_BUILD_ID to Build.ID,
    Constants.SystemProps.RO_BUILD_VERSION_SECURITY_PATCH to Build.VERSION.SECURITY_PATCH
  )
}

private enum class SnapshotSystemProp(val key: String) {
  BUILD_ID(Constants.SystemProps.RO_BUILD_ID),
  SECURITY_PATCH(Constants.SystemProps.RO_BUILD_VERSION_SECURITY_PATCH)
}
