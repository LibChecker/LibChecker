package com.absinthe.libchecker.domain.snapshot.list.usecase

import android.content.Context
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.GetSnapshotSystemPropDiffsUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotSystemProp
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotSystemPropDisplayData

class BuildSnapshotSystemPropDisplayDataUseCase(
  private val context: Context,
  private val getSnapshotSystemPropDiffs: GetSnapshotSystemPropDiffsUseCase
) {

  suspend operator fun invoke(timestamp: Long): List<SnapshotSystemPropDisplayData> {
    return getSnapshotSystemPropDiffs(timestamp).map { diff ->
      SnapshotSystemPropDisplayData(
        label = getSystemPropLabel(diff.prop),
        displayValue = "${diff.previous} $ARROW ${diff.current}"
      )
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
