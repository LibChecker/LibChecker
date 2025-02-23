package com.absinthe.libchecker.compat

import android.content.Context
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.utils.DeviceUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.toClassDefType
import com.absinthe.libchecker.utils.showToast
import java.io.File

object VersionCompat {

  private const val CLASS_NAME_CLIPBOARD_OVERLAY_VIEW = "com.android.systemui.clipboardoverlay.ClipboardOverlayView"

  private val hasClipboardOverlayView: Boolean by lazy {
    runCatching {
      if (DeviceUtils.isOppoDomestic()) return@runCatching false

      val sourceDir =
        PackageUtils.getPackageInfo(Constants.PackageNames.SYSTEMUI).applicationInfo?.sourceDir
          ?: return@runCatching false
      val source = File(sourceDir)
      PackageUtils.findDexClasses(
        source,
        listOf(CLASS_NAME_CLIPBOARD_OVERLAY_VIEW.toClassDefType())
      ).isNotEmpty()
    }.getOrDefault(false)
  }

  fun showCopiedOnClipboardToast(context: Context) {
    // See also: https://developer.android.com/about/versions/13/features/copy-paste
    if (!OsUtils.atLeastT() || !hasClipboardOverlayView) {
      context.showToast(R.string.toast_copied_to_clipboard)
    }
  }
}
