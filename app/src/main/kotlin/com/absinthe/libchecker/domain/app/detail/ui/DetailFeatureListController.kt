package com.absinthe.libchecker.domain.app.detail.ui

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.domain.app.detail.model.DetailFeatureItem
import com.absinthe.libchecker.domain.app.detail.ui.adapter.FeatureAdapter
import com.absinthe.libchecker.ui.adapter.HorizontalSpacesItemDecoration
import com.absinthe.libchecker.utils.extensions.dp

class DetailFeatureListController(
  private val headerContentLayout: ViewGroup
) {
  private val adapter = FeatureAdapter()
  private var featureListView: RecyclerView? = null

  val itemCount: Int
    get() = adapter.data.size

  val isInitialized: Boolean
    get() = featureListView != null

  fun addItem(featureItem: DetailFeatureItem) {
    ensureListView()
    val position = featureItem.position
    if (position == null) {
      adapter.addData(featureItem.item)
    } else {
      adapter.addData(position, featureItem.item)
    }
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

        if (valueAnimator.animatedFraction == 1f || adapter.data.isEmpty()) {
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
    featureListView?.let {
      if (it.parent != null) {
        (it.parent as? ViewGroup)?.removeView(it)
      }
    }
    featureListView = null
    adapter.setList(emptyList())
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
}
