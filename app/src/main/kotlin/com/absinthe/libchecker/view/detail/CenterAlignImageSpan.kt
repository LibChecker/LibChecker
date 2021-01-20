package com.absinthe.libchecker.view.detail

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan

class CenterAlignImageSpan(drawable: Drawable) : ImageSpan(drawable) {

    override fun draw(
        canvas: Canvas, text: CharSequence?, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int,
        paint: Paint
    ) {
        val b = drawable
        val fm: Paint.FontMetricsInt = paint.fontMetricsInt
        val transY: Int = (y + fm.descent + y + fm.ascent) / 2 - b.bounds.bottom / 2
        canvas.save()
        canvas.translate(x, transY.toFloat())
        b.draw(canvas)
        canvas.restore()
    }
}