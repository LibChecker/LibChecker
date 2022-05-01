package com.absinthe.libchecker.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.MessageQueue
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.text.toSpannable
import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.SystemServices
import com.absinthe.libchecker.annotation.AUTUMN
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.SPRING
import com.absinthe.libchecker.annotation.SUMMER
import com.absinthe.libchecker.annotation.WINTER
import com.absinthe.libchecker.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.bean.DetailExtraBean
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.Constants.OVERLAY
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.ui.detail.AppDetailActivity
import com.absinthe.libchecker.ui.detail.EXTRA_DETAIL_BEAN
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.fragment.detail.EXTRA_LC_ITEM
import com.absinthe.libchecker.ui.fragment.detail.OverlayDetailBottomSheetDialogFragment
import com.absinthe.libchecker.ui.main.EXTRA_REF_NAME
import com.absinthe.libchecker.ui.main.EXTRA_REF_TYPE
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.isTempApk
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.absinthe.rulesbundle.LCRules
import com.absinthe.rulesbundle.Rule
import com.google.android.material.progressindicator.LinearProgressIndicator
import rikka.material.app.DayNightDelegate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object LCAppUtils {

  fun getCurrentSeason(): Int {
    return when (Calendar.getInstance(Locale.getDefault()).get(Calendar.MONTH) + 1) {
      3, 4, 5 -> SPRING
      6, 7, 8 -> SUMMER
      9, 10, 11 -> AUTUMN
      12, 1, 2 -> WINTER
      else -> -1
    }
  }

  fun setTitle(context: Context): Spannable {
    val sb = SpannableStringBuilder(context.getString(R.string.app_name))
    val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

    when {
      date.endsWith("1225") -> {
        sb.append("\uD83C\uDF84")
      }
      date == "20220131" -> {
        sb.append("\uD83C\uDFEE")
      }
      date == "20220201" -> {
        sb.append("\uD83D\uDC2F")
      }
    }

    if (BuildConfig.IS_DEV_VERSION) {
      val spanString = SpannableString("   ")
      val span = CenterAlignImageSpan(
        R.drawable.ic_ci_label.getDrawable(context)!!
          .also { it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight) })
      spanString.setSpan(span, 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
      sb.append(spanString)
    }
    return sb.toSpannable()
  }

  fun getAppIcon(packageName: String): Drawable {
    return try {
      val pi = SystemServices.packageManager.getPackageInfo(packageName, 0)
      pi?.applicationInfo?.loadIcon(SystemServices.packageManager)!!
    } catch (e: Exception) {
      ColorDrawable(Color.TRANSPARENT)
    }
  }

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
  fun atLeastS(): Boolean {
    return Build.VERSION.SDK_INT >= 31
  }

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
  fun atLeastR(): Boolean {
    return Build.VERSION.SDK_INT >= 30
  }

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
  fun atLeastQ(): Boolean {
    return Build.VERSION.SDK_INT >= 29
  }

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
  fun atLeastP(): Boolean {
    return Build.VERSION.SDK_INT >= 28
  }

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
  fun atLeastO(): Boolean {
    return Build.VERSION.SDK_INT >= 26
  }

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
  fun atLeastN(): Boolean {
    return Build.VERSION.SDK_INT >= 24
  }

  suspend fun getRuleWithRegex(
    name: String,
    @LibType type: Int,
    packageName: String? = null,
    nativeLibs: List<LibStringItem>? = null
  ): Rule? {
    val ruleEntity = LCRules.getRule(name, type, true) ?: return null
    if (type == NATIVE) {
      if (packageName == null) {
        return ruleEntity
      }
      val isApk = packageName.isTempApk()
      when (name) {
        "libjiagu.so", "libjiagu_a64.so", "libjiagu_x86.so", "libjiagu_x64.so" -> {
          return if (PackageUtils.hasDexClass(packageName, "com.qihoo.util.QHClassLoader", isApk)) {
            ruleEntity
          } else {
            null
          }
        }
        "libapp.so" -> {
          return if (nativeLibs?.any { it.name == "libflutter.so" } == true || PackageUtils.hasDexClass(
              packageName,
              "io.flutter.FlutterInjector",
              isApk
            )
          ) {
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

  fun checkNativeLibValidation(packageName: String, nativeLib: String): Boolean {
    return when (nativeLib) {
      "libjiagu.so" -> {
        PackageUtils.hasDexClass(packageName, "com.qihoo.util.QHClassLoader", false)
      }
      "libapp.so" -> {
        PackageUtils.hasDexClass(packageName, "io.flutter.FlutterInjector", false)
      }
      else -> true
    }
  }

  fun getNightMode(nightModeString: String): Int {
    return when (nightModeString) {
      Constants.DARK_MODE_OFF -> DayNightDelegate.MODE_NIGHT_NO
      Constants.DARK_MODE_ON -> DayNightDelegate.MODE_NIGHT_YES
      Constants.DARK_MODE_FOLLOW_SYSTEM -> DayNightDelegate.MODE_NIGHT_FOLLOW_SYSTEM
      else -> DayNightDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
  }

  fun launchDetailPage(
    context: FragmentActivity,
    item: LCItem,
    refName: String? = null,
    refType: Int = NATIVE
  ) {
    if (item.abi.toInt() == OVERLAY) {
      OverlayDetailBottomSheetDialogFragment().apply {
        arguments = bundleOf(
          EXTRA_LC_ITEM to item
        )
        show(context.supportFragmentManager, tag)
      }
    } else {
      val intent = Intent(context, AppDetailActivity::class.java)
        .putExtras(
          bundleOf(
            EXTRA_PACKAGE_NAME to item.packageName,
            EXTRA_REF_NAME to refName,
            EXTRA_REF_TYPE to refType,
            EXTRA_DETAIL_BEAN to DetailExtraBean(
              item.isSplitApk,
              item.isKotlinUsed,
              item.variant
            )
          )
        )
      context.startActivity(intent)
    }
  }

  fun createLoadingDialog(context: ContextThemeWrapper): AlertDialog {
    return BaseAlertDialogBuilder(context)
      .setView(
        LinearProgressIndicator(context).apply {
          layoutParams = ViewGroup.LayoutParams(200.dp, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            setPadding(24.dp, 24.dp, 24.dp, 24.dp)
          }
          trackCornerRadius = 3.dp
          isIndeterminate = true
        }
      )
      .setCancelable(false)
      .create()
  }
}

/**
 * From drakeet
 */
fun doOnMainThreadIdle(action: () -> Unit) {
  val handler = Handler(Looper.getMainLooper())

  val idleHandler = MessageQueue.IdleHandler {
    handler.removeCallbacksAndMessages(null)
    action()
    return@IdleHandler false
  }

  fun setupIdleHandler(queue: MessageQueue) {
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
