package com.absinthe.libchecker.ktx

import android.view.View
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.Toasty
import rikka.core.util.ClipboardUtils

fun View.setLongClickCopiedToClipboard(text: String) {
    setOnLongClickListener {
        ClipboardUtils.put(context, text)
        Toasty.show(context, R.string.toast_copied_to_clipboard)
        true
    }
}