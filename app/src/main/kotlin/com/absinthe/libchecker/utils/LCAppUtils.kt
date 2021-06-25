package com.absinthe.libchecker.utils

import SystemServices
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.MessageQueue
import androidx.annotation.ChecksSdkIntAtLeast
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.database.entity.RuleEntity
import timber.log.Timber
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
        val sb = StringBuilder(LibCheckerApp.context.getString(R.string.app_name))
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        when {
            date.endsWith("1225") -> {
                sb.append("\uD83C\uDF84")
            }
            date == "20210211" -> {
                sb.append("\uD83C\uDFEE")
            }
            date == "20210212" -> {
                sb.append("\uD83D\uDC2E")
            }
        }
        return sb.toString()
    }

    fun getAppIcon(packageName: String): Drawable {
        return try {
            val pi = SystemServices.packageManager.getPackageInfo(packageName, 0)
            pi?.applicationInfo?.loadIcon(SystemServices.packageManager)!!
        } catch (e: Exception) {
            ColorDrawable(Color.TRANSPARENT)
        }
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun atLeastR(): Boolean {
        return Build.VERSION.SDK_INT >= 30
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

    fun findRuleRegex(string: String, @LibType type: Int): RuleEntity? {
        val iterator = AppItemRepository.rulesRegexList.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key.matcher(string).matches() && entry.value.type == type) {
                return entry.value
            }
        }
        return null
    }

    suspend fun getRuleWithDexChecking(name: String, packageName: String? = null): RuleEntity? {
        val ruleEntity = LibCheckerApp.repository.getRule(name) ?: return null
        if (ruleEntity.type == NATIVE) {
            if (packageName == null) {
                return ruleEntity
            }
            val isApk = packageName.endsWith("/temp.apk")
            when(ruleEntity.name) {
                "libjiagu.so", "libjiagu_a64.so", "libjiagu_x86.so", "libjiagu_x64.so" -> {
                    return if (PackageUtils.hasDexClass(packageName, "com.qihoo.util", isApk)) {
                        ruleEntity
                    } else {
                        null
                    }
                }
                "libapp.so" -> {
                    return if (PackageUtils.hasDexClass(packageName, "io.flutter", isApk)) {
                        ruleEntity
                    } else {
                        null
                    }
                }
                else -> return ruleEntity
            }
        } else {
            return ruleEntity
        }
    }

    suspend fun getRuleWithRegex(name: String, @LibType type: Int, packageName: String? = null): RuleEntity? {
        return getRuleWithDexChecking(name, packageName) ?: findRuleRegex(name, type)
    }

    fun checkNativeLibValidation(packageName: String, nativeLib: String): Boolean {
        return when(nativeLib) {
            "libjiagu.so" -> { PackageUtils.hasDexClass(packageName, "com.qihoo.util", false) }
            "libapp.so" -> { PackageUtils.hasDexClass(packageName, "io.flutter", false) }
            else -> true
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
                    Timber.d("doOnMainThreadIdle: ${timeout}ms timeout!")
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