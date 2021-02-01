package com.absinthe.libchecker.extensions

import android.content.res.Resources
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libraries.utils.utils.UiUtils
import rikka.core.util.ClipboardUtils

fun View.setLongClickCopiedToClipboard(text: String) {
    setOnLongClickListener {
        ClipboardUtils.put(context, text)
        Toasty.show(context, R.string.toast_copied_to_clipboard)
        true
    }
}

val Number.dp: Int get() = (toInt() * Resources.getSystem().displayMetrics.density).toInt()

var View.paddingStartCompat: Int
    set(value) {
        setPadding(value, paddingTop, paddingEnd, paddingBottom)
    }
    get() = paddingStart

fun View.addPaddingStart(padding: Int) {
    setPadding(paddingStart + padding, paddingTop, paddingEnd, paddingBottom)
}

var View.paddingTopCompat: Int
    set(value) {
        setPadding(paddingStart, value, paddingEnd, paddingBottom)
    }
    get() = paddingTop

fun View.addPaddingTop(padding: Int) {
    setPadding(paddingStart, paddingTop + padding, paddingEnd, paddingBottom)
}

var View.paddingEndCompat: Int
    set(value) {
        setPadding(paddingStart, paddingTop, value, paddingBottom)
    }
    get() = paddingEnd

fun View.addPaddingEnd(padding: Int) {
    setPadding(paddingStart, paddingTop, paddingEnd + padding, paddingBottom)
}

var View.paddingBottomCompat: Int
    set(value) {
        setPadding(paddingStart, paddingTop, paddingEnd, value)
    }
    get() = paddingBottom

fun View.addPaddingBottom(padding: Int) {
    setPadding(paddingStart, paddingTop, paddingEnd, paddingBottom + padding)
}

fun ViewGroup.setSystemPadding() {
    val isOrientationLandscape = context.isOrientationLandscape
    fitsSystemWindows = isOrientationLandscape
    setPadding(0, if (isOrientationLandscape) 0 else UiUtils.getStatusBarHeight(), 0, 0)
}

fun TextView.tintHighlightText(highlightText: String, rawText: String) {
    text = rawText
    if (text.contains(highlightText, true)) {
        val builder = SpannableStringBuilder()
        val spannableString = SpannableString(text.toString())
        val start = text.indexOf(highlightText, 0, true)
        spannableString.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorPrimary)),
            start, start + highlightText.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        builder.append(spannableString)
        text = builder
    }
}

fun DialogFragment.isShowing() = this.dialog != null && this.dialog!!.isShowing && !this.isRemoving