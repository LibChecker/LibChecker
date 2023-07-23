package com.absinthe.libchecker.compat

import android.content.Context
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.toClassDefType
import com.absinthe.libchecker.utils.showToast
import java.io.File

object VersionCompat {

  private val hasClipboardOverlayView: Boolean by lazy {
    runCatching {
      val source =
        File(PackageUtils.getPackageInfo(Constants.PackageNames.SYSTEMUI).applicationInfo.sourceDir)
      PackageUtils.findDexClasses(
        source,
        listOf("com.android.systemui.clipboardoverlay.ClipboardOverlayView".toClassDefType())
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
