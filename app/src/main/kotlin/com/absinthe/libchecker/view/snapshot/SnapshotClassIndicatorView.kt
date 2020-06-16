package com.absinthe.libchecker.view.snapshot

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.absinthe.libchecker.R

class SnapshotClassIndicatorView : LinearLayout {

    constructor(context: Context?) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        LayoutInflater.from(context).inflate(R.layout.layout_snapshot_indicator, this)
        context.obtainStyledAttributes(attrs, R.styleable.SnapshotClassIndicatorView).apply {
            findViewById<TextView>(R.id.text)?.text = getString(R.styleable.SnapshotClassIndicatorView_text)
            findViewById<ImageView>(R.id.indicator)?.setImageResource(getResourceId(R.styleable.SnapshotClassIndicatorView_indicator, 0))
            recycle()
        }
    }

}