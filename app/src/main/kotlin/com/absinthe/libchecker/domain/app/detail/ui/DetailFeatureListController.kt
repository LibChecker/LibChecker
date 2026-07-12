package com.absinthe.libchecker.domain.app.detail.ui

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.DetailFeatureItem
import com.absinthe.libchecker.domain.app.detail.model.DetailFeatureListState
import com.absinthe.libchecker.domain.app.detail.ui.adapter.FeatureAdapter
import com.absinthe.libchecker.ui.adapter.HorizontalSpacesItemDecoration
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.ToolbarConnectionLoadingView

class DetailFeatureListController(
  private val headerContentLayout: ViewGroup
) {
  private val adapter = FeatureAdapter { it.action() }
  private var state = DetailFeatureListState()
  private var featureListView: RecyclerView? = null
  private var loadingView: ToolbarConnectionLoadingView? = null

  init {
    adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
  }

  val itemCount: Int
    get() = state.items.size

  val isInitialized: Boolean
    get() = featureListView != null

  fun addItem(featureItem: DetailFeatureItem) {
    val shouldAlignListStart = state.items.isEmpty() || featureItem.position == 0
    updateState { it.withItem(featureItem) }
    if (shouldAlignListStart) {
      alignListStart()
    }
  }

  fun setLoading(loading: Boolean) {
    updateState { it.copy(isLoading = loading) }
  }

  fun attachWithAnimation() {
    ensureListView()
    val view = featureListView ?: return
    if (view.parent != null) {
      return
    }

    val oldContainerHeight = headerContentLayout.height
    val newContainerHeight = oldContainerHeight + 40.dp
    val params = headerContentLayout.layoutParams

    headerContentLayout.addView(view)
    ValueAnimator.ofInt(oldContainerHeight, newContainerHeight).also { anim ->
      anim.addUpdateListener { valueAnimator ->
        val height = valueAnimator.animatedValue as Int

        if (valueAnimator.animatedFraction == 1f || (state.items.isEmpty() && !state.isLoading)) {
          params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        } else {
          params.height = height
        }
        headerContentLayout.layoutParams = params
      }
      anim.duration = 250
      anim.start()
    }
  }

  fun reset() {
    state = DetailFeatureListState()
    loadingView?.stop()
    adapter.removeAllFooterView()
    featureListView?.let {
      (it.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(0, 0)
      if (it.parent != null) {
        (it.parent as? ViewGroup)?.removeView(it)
      }
    }
    featureListView = null
    adapter.setList(state.items)
  }

  private fun updateState(
    transform: (DetailFeatureListState) -> DetailFeatureListState
  ) {
    val previousState = state
    val newState = transform(previousState)
    if (newState == previousState) return

    state = newState
    renderState(previousState)
  }

  private fun renderState(previousState: DetailFeatureListState) {
    if (state.items.isNotEmpty() || state.isLoading) {
      ensureListView()
    }
    if (state.items != previousState.items) {
      adapter.setList(state.items)
    }
    if (state.isLoading != previousState.isLoading) {
      renderLoading()
    }
  }

  private fun renderLoading() {
    if (state.isLoading) {
      val view = ensureLoadingView()
      adapter.setFooterView(view)
      view.start()
    } else {
      loadingView?.stop()
      adapter.removeAllFooterView()
      if (state.items.isNotEmpty()) {
        alignListStart()
      }
    }
  }

  private fun ensureListView() {
    if (featureListView != null) {
      return
    }

    featureListView = RecyclerView(headerContentLayout.context).also {
      it.layoutParams = ViewGroup.MarginLayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also { lp ->
        lp.topMargin = 4.dp
      }
      it.addItemDecoration(HorizontalSpacesItemDecoration(4.dp))
      it.layoutManager = LinearLayoutManager(headerContentLayout.context, LinearLayoutManager.HORIZONTAL, false)
      it.adapter = adapter
      it.itemAnimator = null
      it.clipChildren = false
      it.clipToPadding = false
      it.overScrollMode = View.OVER_SCROLL_NEVER
    }
  }

  private fun alignListStart() {
    featureListView?.let { view ->
      view.post {
        (view.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(0, 0)
        view.invalidateItemDecorations()
      }
    }
  }

  private fun ensureLoadingView(): ToolbarConnectionLoadingView {
    return loadingView ?: ToolbarConnectionLoadingView(headerContentLayout.context).apply {
      layoutParams = RecyclerView.LayoutParams(34.dp, 36.dp)
      contentDescription = context.getString(R.string.loading)
    }.also {
      loadingView = it
    }
  }
}
