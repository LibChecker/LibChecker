package com.absinthe.libchecker.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import com.absinthe.libchecker.view.app.ToastView
import java.lang.ref.WeakReference
import timber.log.Timber

/**
 * <pre>
 * author : Absinthe
 * time : 2020/08/12
 * </pre>
 */
object Toasty {
  private val handler = Handler(Looper.getMainLooper())
  private var toastRef: WeakReference<Toast>? = null

  @AnyThread
  fun showShort(context: Context, message: String) {
    if (Looper.getMainLooper() == Looper.myLooper()) {
      //noinspection WrongThread
      show(context, message, Toast.LENGTH_SHORT)
    } else {
      handler.post { show(context, message, Toast.LENGTH_SHORT) }
    }
  }

  @AnyThread
  fun showShort(context: Context, @StringRes res: Int) {
    showShort(context, context.getString(res))
  }

  @AnyThread
  fun showLong(context: Context, message: String) {
    if (Looper.getMainLooper() == Looper.myLooper()) {
      //noinspection WrongThread
      show(context, message, Toast.LENGTH_LONG)
    } else {
      handler.post { show(context, message, Toast.LENGTH_LONG) }
    }
  }

  @AnyThread
  fun showLong(context: Context, @StringRes res: Int) {
    showLong(context, context.getString(res))
  }

  @Suppress("deprecation")
  @MainThread
  private fun show(context: Context, message: String, duration: Int) {
    toastRef?.get()?.cancel()

    val toast = if (OsUtils.atLeastR() && context !is ContextThemeWrapper) {
      Toast.makeText(context, message, duration)
    } else {
      val view = ToastView(context).also {
        it.message.text = message
      }
      Toast(context).also {
        it.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 200)
        it.duration = duration
        it.view = view
      }
    }
    toastRef = WeakReference(toast)
    toast.show()
  }
}

@AnyThread
fun Context.showToast(message: String) {
  Toasty.showShort(this, message)
}

@AnyThread
fun Context.showToast(@StringRes res: Int) {
  Timber.d("showToast: ${getString(res)}")
  Toasty.showShort(this, res)
}

@AnyThread
fun Context.showLongToast(message: String) {
  Toasty.showLong(this, message)
}

@AnyThread
fun Context.showLongToast(@StringRes res: Int) {
  Toasty.showLong(this, res)
}
