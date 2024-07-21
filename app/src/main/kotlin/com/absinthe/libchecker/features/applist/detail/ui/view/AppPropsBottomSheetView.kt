package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.features.applist.detail.ui.adapter.AppPropsAdapter
import com.absinthe.libchecker.ui.adapter.VerticalSpacesItemDecoration
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.paddingBottomCompat
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import timber.log.Timber

class AppPropsBottomSheetView(context: Context, packageInfo: PackageInfo) :
  LinearLayout(context),
  IHeaderView {

  val adapter by unsafeLazy {
    AppPropsAdapter(
      packageInfo,
      (context as FragmentActivity).supportFragmentManager
    )
  }

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.lib_detail_app_props_title)
  }

  private val tipView = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 4.dp
    }
    gravity = Gravity.CENTER
    R.drawable.ic_open_in_new.getDrawable(context)?.let {
      val tip = context.getString(R.string.lib_detail_app_props_tip)
      it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
      val span = CenterAlignImageSpan(it)
      val spannableString = SpannableString("$tip  ")
      spannableString.setSpan(span, tip.length, tip.length + 1, ImageSpan.ALIGN_BOTTOM)
      spannableString.setSpan(UnderlineSpan(), 0, tip.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
      text = spannableString
    }
    setOnClickListener {
      runCatching {
        CustomTabsIntent.Builder().build().apply {
          launchUrl(context, URLManager.ANDROID_DEV_MANIFEST_APPLICATION.toUri())
        }
      }.onFailure {
        Timber.e(it)
        runCatching {
          val intent = Intent(Intent.ACTION_VIEW)
            .setData(URLManager.ANDROID_DEV_MANIFEST_APPLICATION.toUri())
          context.startActivity(intent)
        }.onFailure { inner ->
          Timber.e(inner)
          Toasty.showShort(context, "No browser application")
        }
      }
    }
  }

  private val list = RecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 24.dp
    }
    overScrollMode = OVER_SCROLL_NEVER
    adapter = this@AppPropsBottomSheetView.adapter
    layoutManager = LinearLayoutManager(context)
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    isNestedScrollingEnabled = true
    setHasFixedSize(true)
    addItemDecoration(VerticalSpacesItemDecoration(4.dp))
    paddingBottomCompat = 16.dp
  }

  init {
    orientation = VERTICAL
    val padding = 16.dp
    setPadding(padding, padding, padding, 0)
    addView(header)
    addView(tipView)
    addView(list)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
