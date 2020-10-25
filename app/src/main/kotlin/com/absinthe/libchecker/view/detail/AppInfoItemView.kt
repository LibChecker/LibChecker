package com.absinthe.libchecker.view.detail

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.absinthe.libchecker.R

/**
 * <pre>
 * author : Absinthe
 * time : 2020/10/25
 * </pre>
 */
class AppInfoItemView : LinearLayout {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        LayoutInflater.from(context).inflate(R.layout.layout_item_app_info, this)
        context.obtainStyledAttributes(attrs, R.styleable.AppInfoItemView).apply {
            findViewById<TextView>(R.id.tv_name)?.text = getString(R.styleable.AppInfoItemView_itemTitle)
            findViewById<ImageView>(R.id.icon)?.apply {
                setImageResource(getResourceId(R.styleable.AppInfoItemView_infoSrc, 0))
                imageTintList = ColorStateList.valueOf(Color.WHITE)
                backgroundTintList = getColorStateList(R.styleable.AppInfoItemView_infoBackgroundTint)
            }
            recycle()
        }
        isClickable = true
        isFocusable = true
    }

    fun setTitle(title: String) {
        findViewById<TextView>(R.id.tv_name)?.text = title
    }

    fun setInfoIconResource(@DrawableRes res: Int) {
        findViewById<ImageView>(R.id.icon)?.apply {
            setImageResource(res)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
        }
    }

    fun setInfoIconDrawable(drawable: Drawable) {
        findViewById<ImageView>(R.id.icon)?.apply {
            setImageDrawable(drawable)
        }
    }
}