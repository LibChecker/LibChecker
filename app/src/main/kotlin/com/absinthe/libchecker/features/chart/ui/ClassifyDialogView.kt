package com.absinthe.libchecker.features.chart.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.features.applist.detail.ui.view.EmptyListView
import com.absinthe.libchecker.features.applist.ui.adapter.AppAdapter
import com.absinthe.libchecker.features.chart.ui.view.AndroidVersionLabelView
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import java.text.SimpleDateFormat
import java.util.Locale
import me.zhanghai.android.fastscroll.FastScrollerBuilder

@SuppressLint("ViewConstructor")
class ClassifyDialogView(context: Context, val lifecycleScope: LifecycleCoroutineScope) :
  LinearLayout(context),
  IHeaderView {

  val adapter = AppAdapter()

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
  }

  private val list = BottomSheetRecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.MATCH_PARENT,
      1.0f
    ).also {
      it.topMargin = 4.dp
    }
    layoutManager = LinearLayoutManager(context)
    adapter = this@ClassifyDialogView.adapter
    overScrollMode = OVER_SCROLL_NEVER
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    isNestedScrollingEnabled = false
    setHasFixedSize(true)
    FastScrollerBuilder(this).useMd2Style().build()
  }

  init {
    orientation = VERTICAL
    addPaddingTop(16.dp)
    adapter.apply {
      setOnItemClickListener { _, _, position ->
        (context as? FragmentActivity)?.launchDetailPage(adapter.getItem(position))
      }
      setEmptyView(
        EmptyListView(context).apply {
          layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, 500.dp)
        }
      )
    }
    addView(header)
    addView(list)
  }

  private val androidVersionView = AndroidVersionLabelView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    setPadding(0, 4.dp, 0, 4.dp)
  }

  fun addAndroidVersionView(node: AndroidVersions.Node?) {
    if (androidVersionView.parent == null && node != null) {
      val iconRes = if (node.version == Build.VERSION_CODES.CUR_DEVELOPMENT) {
        AndroidVersions.versions.getOrNull(Build.VERSION.SDK_INT + 1)?.iconRes
      } else {
        node.iconRes
      }
      androidVersionView.setIcon(iconRes)
      val text = StringBuilder(node.codeName)
      if (node.versionName.isNotEmpty()) {
        text.append(", ").append(node.versionName)
      }
      text.append(", ")
      text.append(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(node.releaseDate))
      androidVersionView.text.text = text
      addView(androidVersionView, 1)
    }
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
