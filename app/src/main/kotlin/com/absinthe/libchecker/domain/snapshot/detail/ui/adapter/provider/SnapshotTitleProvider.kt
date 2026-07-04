package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.provider

import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.SnapshotDetailCountAdapter
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotDetailCountNode
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotTitleNode
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotDetailTitleView
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder

const val SNAPSHOT_TITLE_PROVIDER = 1

class SnapshotTitleProvider : BaseNodeProvider() {

  override val itemViewType: Int = SNAPSHOT_TITLE_PROVIDER
  override val layoutId: Int = 0

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(SnapshotDetailTitleView(context))
  }

  override fun convert(helper: BaseViewHolder, item: BaseNode) {
    val itemView = (helper.itemView as SnapshotDetailTitleView).container
    val node = item as SnapshotTitleNode
    val countAdapter = SnapshotDetailCountAdapter()

    val titleRes = when (node.type) {
      NATIVE -> R.string.ref_category_native
      SERVICE -> R.string.ref_category_service
      ACTIVITY -> R.string.ref_category_activity
      RECEIVER -> R.string.ref_category_br
      PROVIDER -> R.string.ref_category_cp
      PERMISSION -> R.string.ref_category_perm
      METADATA -> R.string.ref_category_metadata
      else -> android.R.string.untitled
    }
    itemView.title.text = context.getString(titleRes)
    itemView.list.apply {
      adapter = countAdapter
      layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
      if (parent == null) {
        itemView.addView(this)
      }
    }

    countAdapter.setList(node.counts)
    helper.itemView.contentDescription = buildTitleDescription(
      itemView.title.text,
      node.counts,
      node.isExpanded
    )

    onExpansionToggled(itemView.arrow, node.isExpanded)
  }

  override fun onClick(helper: BaseViewHolder, view: View, data: BaseNode, position: Int) {
    getAdapter()?.expandOrCollapse(position)
  }

  private fun onExpansionToggled(arrow: ImageView, expanded: Boolean) {
    val start: Float
    val target: Float

    if (expanded) {
      start = 0f
      target = 90f
    } else {
      start = 90f
      target = 0f
    }

    ObjectAnimator.ofFloat(arrow, View.ROTATION, start, target).apply {
      duration = 200
      start()
    }
  }

  private fun buildTitleDescription(
    title: CharSequence,
    counts: List<SnapshotDetailCountNode>,
    expanded: Boolean
  ): String {
    return (
      listOf(title) +
        counts.map { "${context.getString(it.status.labelRes)} ${it.count}" } +
        listOf(
          context.getString(
            if (expanded) R.string.a11y_state_expanded else R.string.a11y_state_collapsed
          )
        )
      )
      .map { it.toString().trim() }
      .filter(String::isNotEmpty)
      .joinToString()
  }
}
