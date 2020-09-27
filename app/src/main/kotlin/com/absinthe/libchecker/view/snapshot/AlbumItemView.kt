package com.absinthe.libchecker.view.snapshot

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.absinthe.libchecker.R
import com.google.android.material.card.MaterialCardView

/**
 * <pre>
 * author : Absinthe
 * time : 2020/09/22
 * </pre>
 */
class AlbumItemView : MaterialCardView {

    constructor(context: Context?) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        LayoutInflater.from(context).inflate(R.layout.layout_album_item, this)
        context.obtainStyledAttributes(attrs, R.styleable.AlbumItemView).apply {
            findViewById<TextView>(R.id.tv_title)?.text = getString(R.styleable.AlbumItemView_albumItemTitle)
            findViewById<TextView>(R.id.tv_subtitle)?.text = getString(R.styleable.AlbumItemView_albumItemSubtitle)
            findViewById<ImageView>(R.id.icon)?.apply {
                setImageResource(getResourceId(R.styleable.AlbumItemView_itemIcon, 0))
                backgroundTintList = getColorStateList(R.styleable.AlbumItemView_itemIconTint)
            }
            recycle()
        }
        background = null
        isClickable = true
        isFocusable = true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}