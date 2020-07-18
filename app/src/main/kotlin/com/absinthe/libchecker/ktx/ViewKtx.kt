package com.absinthe.libchecker.ktx

import android.view.View
import com.absinthe.libchecker.R
import com.blankj.utilcode.util.ToastUtils
import rikka.core.util.ClipboardUtils

fun View.setLongClickCopiedToClipboard(text: String) {
    setOnLongClickListener {
        ClipboardUtils.put(context, text)
        ToastUtils.showShort(R.string.toast_copied_to_clipboard)
        true
    }
}