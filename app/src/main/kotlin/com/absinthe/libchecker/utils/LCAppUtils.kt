package com.absinthe.libchecker.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.MessageQueue
import android.util.Log
import androidx.annotation.ChecksSdkIntAtLeast
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.AUTUMN
import com.absinthe.libchecker.annotation.SPRING
import com.absinthe.libchecker.annotation.SUMMER
import com.absinthe.libchecker.annotation.WINTER
import com.absinthe.libchecker.extensions.loge
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.Utils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

object LCAppUtils {

    fun getCurrentSeason(): Int {
        return when(Calendar.getInstance(Locale.getDefault()).get(Calendar.MONTH) + 1) {
            3, 4, 5 -> SPRING
            6, 7, 8 -> SUMMER
            9, 10, 11 -> AUTUMN
            12, 1, 2 -> WINTER
            else -> -1
        }
    }

    fun setTitle(): String {
        val sb = StringBuilder(Utils.getApp().getString(R.string.app_name))

        if (SimpleDateFormat("MMdd", Locale.getDefault()).format(Date()) == "1225") {
            sb.append("\uD83C\uDF84")
        }
        return sb.toString()
    }

    fun getAppIcon(packageName: String): Drawable {
        return try {
            AppUtils.getAppIcon(packageName)
        } catch (e: Exception) {
            ColorDrawable(Color.TRANSPARENT)
        }
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    fun atLeastP(): Boolean {
        return Build.VERSION.SDK_INT >= 28
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    fun atLeastO(): Boolean {
        return Build.VERSION.SDK_INT >= 26
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N_MR1)
    fun atLeastNMR1(): Boolean {
        return Build.VERSION.SDK_INT >= 25
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
    fun atLeastN(): Boolean {
        return Build.VERSION.SDK_INT >= 24
    }

    fun getFromAssets(context: Context, fileName: String): String? {
        try {
            val inputReader = InputStreamReader(context.resources.assets.open(fileName))
            val bufReader = BufferedReader(inputReader)
            val result = StringBuilder()
            var line: String

            while (bufReader.readLine().also { line = it } != null) {
                result.append(line)
            }
            return result.toString()
        } catch (e: Exception) {
            loge("getFromAssets", e)
            return null
        }
    }
}

/**
 * From drakeet
 */
fun doOnMainThreadIdle(action: () -> Unit, timeout: Long? = null) {
    val handler = Handler(Looper.getMainLooper())

    val idleHandler = MessageQueue.IdleHandler {
        handler.removeCallbacksAndMessages(null)
        action()
        return@IdleHandler false
    }

    fun setupIdleHandler(queue: MessageQueue) {
        if (timeout != null) {
            handler.postDelayed({
                queue.removeIdleHandler(idleHandler)
                action()
                if (BuildConfig.DEBUG) {
                    Log.d("doOnMainThreadIdle", "${timeout}ms timeout!")
                }
            }, timeout)
        }
        queue.addIdleHandler(idleHandler)
    }

    if (Looper.getMainLooper() == Looper.myLooper()) {
        setupIdleHandler(Looper.myQueue())
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setupIdleHandler(Looper.getMainLooper().queue)
        } else {
            handler.post { setupIdleHandler(Looper.myQueue()) }
        }
    }
}