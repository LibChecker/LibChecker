package com.absinthe.libchecker.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import com.absinthe.libchecker.R
import java.lang.ref.WeakReference

/**
 * <pre>
 * author : Absinthe
 * time : 2020/08/12
 * </pre>
 */
object Toasty {

    fun show(context: Context, message: String) {
        show(context, message, Toast.LENGTH_SHORT)
    }

    fun show(context: Context, @StringRes res: Int) {
        show(context, context.getString(res), Toast.LENGTH_SHORT)
    }

    fun showLong(context: Context, message: String) {
        show(context, message, Toast.LENGTH_LONG)
    }

    fun showLong(context: Context, @StringRes res: Int) {
        show(context, context.getString(res), Toast.LENGTH_LONG)
    }

    @SuppressLint("InflateParams")
    private fun show(context: Context, message: String, duration: Int) {
        val weakCtx = WeakReference(context)
        val view = LayoutInflater.from(weakCtx.get()).inflate(R.layout.layout_toast, null)

        view.findViewById<TextView>(R.id.message).apply {
            text = message
        }

        Toast(context).apply {
            setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 200)
            this.duration = duration
            this.view = view
        }.show()
    }
}