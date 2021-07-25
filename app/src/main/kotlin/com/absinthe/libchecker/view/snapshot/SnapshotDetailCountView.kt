package com.absinthe.libchecker.view.snapshot

import android.content.Context
import android.util.TypedValue
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dp

class SnapshotDetailCountView(context: Context) : AppCompatTextView(context) {

    init {
        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
            it.marginStart = 4.dp
            it.marginEnd = 4.dp
        }
        background = ContextCompat.getDrawable(context, R.drawable.bg_snapshot_detail_count)
        setPadding(8.dp, 2.dp, 8.dp, 2.dp)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(ContextCompat.getColor(context, R.color.grey_600))
    }

}
