package com.absinthe.libchecker.view.app

import android.content.Context
import android.graphics.drawable.TransitionDrawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup

class BottomSheetHeaderView(context: Context) : AViewGroup(context) {

    private val handler = View(context).apply {
        layoutParams = LayoutParams(36.dp, 4.dp)
        setBackgroundResource(R.drawable.bg_dialog_handler)
        addView(this)
    }

    val title = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.topMargin = 16.dp
        }
        setPadding(16.dp, 0, 16.dp, 0)
        setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceHeadline5))
        text = context.getString(R.string.further_operation)
        gravity = Gravity.CENTER_HORIZONTAL
        addView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        handler.autoMeasure()
        title.autoMeasure()
        setMeasuredDimension(measuredWidth, handler.marginTop + handler.measuredHeight + title.marginTop + title.measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        handler.layout(handler.toHorizontalCenter(this), 0)
        title.layout(0, handler.bottom + title.marginTop)
    }

    fun onHandlerActivated(activated: Boolean) {
        val handlerArray = if (activated) {
            arrayOf(context.getDrawable(R.drawable.bg_dialog_handler), context.getDrawable(R.drawable.bg_dialog_handler_activated))
        } else {
            arrayOf(context.getDrawable(R.drawable.bg_dialog_handler_activated), context.getDrawable(R.drawable.bg_dialog_handler))
        }
        val transitionDrawable = TransitionDrawable(handlerArray)
        handler.background = transitionDrawable
        transitionDrawable.startTransition(150)
    }
}