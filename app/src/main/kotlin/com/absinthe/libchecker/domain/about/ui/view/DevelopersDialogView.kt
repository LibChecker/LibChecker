package com.absinthe.libchecker.domain.about.ui.view

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isGone
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.about.model.DeveloperInfo
import com.absinthe.libchecker.domain.about.ui.adapter.DeveloperInfoAdapter
import com.absinthe.libchecker.ui.adapter.VerticalSpacesItemDecoration
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libchecker.view.app.ToolbarConnectionLoadingView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class DevelopersDialogView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.WRAP_CONTENT
    )
    title.text = "Developers"
  }

  private val loadingView = ToolbarConnectionLoadingView(context).apply {
    layoutParams = FrameLayout.LayoutParams(34.dp, 18.dp).apply {
      gravity = Gravity.CENTER
    }
    alpha = 0f
    scaleX = 0.78f
    scaleY = 0.78f
    contentDescription = context.getString(R.string.loading)
  }

  private val headerContainer = FrameLayout(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    addView(header)
  }

  private val loadingContainer = FrameLayout(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 288.dp)
    isGone = true
    addView(loadingView)
  }

  private val _adapter = DeveloperInfoAdapter()
  private val visibilityInterpolator = FastOutSlowInInterpolator()
  private var loadingRequested = false

  private val recyclerView = RecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    setPadding(16.dp, 16.dp, 16.dp, 16.dp)
    adapter = _adapter
    layoutManager = LinearLayoutManager(context)
    addItemDecoration(
      VerticalSpacesItemDecoration(4.dp)
    )
  }

  init {
    orientation = VERTICAL
    addView(headerContainer)
    addView(loadingContainer)
    addView(recyclerView)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }

  fun setItems(items: List<DeveloperInfo>) {
    _adapter.setList(items)
  }

  fun setLoading(loading: Boolean) {
    if (loadingRequested == loading) {
      return
    }
    loadingRequested = loading
    loadingView.animate().cancel()

    if (loading) {
      recyclerView.isGone = true
      loadingContainer.isGone = false
      loadingView.start()
      loadingView.animate()
        .alpha(1f)
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(180L)
        .setInterpolator(visibilityInterpolator)
        .start()
    } else {
      loadingView.animate()
        .alpha(0f)
        .scaleX(0.78f)
        .scaleY(0.78f)
        .setDuration(150L)
        .setInterpolator(visibilityInterpolator)
        .withEndAction {
          loadingView.stop()
          loadingContainer.isGone = true
          recyclerView.isGone = false
        }
        .start()
    }
  }

  override fun onDetachedFromWindow() {
    loadingView.stop()
    super.onDetachedFromWindow()
  }
}
