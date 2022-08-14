package com.absinthe.libchecker.compat

import android.content.Context
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.showToast

object VersionCompat {

  fun showCopiedOnClipboardToast(context: Context) {
    // See also: https://developer.android.com/about/versions/13/features/copy-paste
    if (!OsUtils.atLeastT()) {
      context.showToast(R.string.toast_copied_to_clipboard)
    }
  }
}
