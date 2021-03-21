package com.absinthe.libchecker.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.StringRes
import com.absinthe.libchecker.view.app.ToastView
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
        WeakReference(context).get()?.let {
            val view = ToastView(it).apply {
                this.message.text = message
            }

            Toast(it).apply {
                setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 200)
                this.duration = duration
                this.view = view
            }.show()
        }
    }
}