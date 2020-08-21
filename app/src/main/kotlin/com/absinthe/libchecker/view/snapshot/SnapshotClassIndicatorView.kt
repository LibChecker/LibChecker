package com.absinthe.libchecker.view.snapshot

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import coil.load
import com.absinthe.libchecker.R
import com.zhangyue.we.x2c.X2C
import com.zhangyue.we.x2c.ano.Xml

@Xml(layouts = ["layout_snapshot_indicator"])
class SnapshotClassIndicatorView : LinearLayout {

    constructor(context: Context?) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        X2C.inflate(context, R.layout.layout_snapshot_indicator, this)
        context.obtainStyledAttributes(attrs, R.styleable.SnapshotClassIndicatorView).apply {
            findViewById<ImageView>(R.id.icon)?.load(getResourceId(R.styleable.SnapshotClassIndicatorView_icon, 0))
            findViewById<TextView>(R.id.text)?.text = getString(R.styleable.SnapshotClassIndicatorView_text)
            findViewById<ImageView>(R.id.indicator)?.load(getResourceId(R.styleable.SnapshotClassIndicatorView_indicator, 0))
            recycle()
        }
    }

}