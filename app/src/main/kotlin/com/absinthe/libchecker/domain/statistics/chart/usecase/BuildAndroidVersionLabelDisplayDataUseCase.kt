package com.absinthe.libchecker.domain.statistics.chart.usecase

import android.os.Build
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.domain.statistics.chart.model.AndroidVersionLabelDisplayData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BuildAndroidVersionLabelDisplayDataUseCase(
  private val sdkInt: Int = Build.VERSION.SDK_INT,
  private val formatReleaseDate: (Date) -> String = { date ->
    SimpleDateFormat(RELEASE_DATE_PATTERN, Locale.getDefault()).format(date)
  }
) {

  operator fun invoke(node: AndroidVersions.Node?): AndroidVersionLabelDisplayData? {
    node ?: return null
    val iconRes = if (node.version == Build.VERSION_CODES.CUR_DEVELOPMENT) {
      AndroidVersions.versions.getOrNull(sdkInt + 1)?.iconRes
    } else {
      node.iconRes
    }
    val text = buildString {
      append(node.codeName)
      if (node.versionName.isNotEmpty()) {
        append(", ").append(node.versionName)
      }
      append(", ").append(formatReleaseDate(node.releaseDate))
    }
    return AndroidVersionLabelDisplayData(
      iconRes = iconRes,
      text = text
    )
  }

  private companion object {
    const val RELEASE_DATE_PATTERN = "yyyy-MM"
  }
}
