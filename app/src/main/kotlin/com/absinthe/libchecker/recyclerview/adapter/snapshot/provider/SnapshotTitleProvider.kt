package com.absinthe.libchecker.recyclerview.adapter.snapshot.provider

import android.animation.ObjectAnimator
import android.view.View
import android.widget.ImageView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotTitleNode
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder

const val SNAPSHOT_TITLE_PROVIDER = 1

class SnapshotTitleProvider : BaseNodeProvider() {

    override val itemViewType: Int = SNAPSHOT_TITLE_PROVIDER
    override val layoutId: Int = R.layout.item_snapshot_title

    override fun convert(helper: BaseViewHolder, item: BaseNode) {
        val node = item as SnapshotTitleNode

        helper.setText(R.id.tv_title, node.title)

        val ivArrow = helper.getView<ImageView>(R.id.iv_arrow)
        if (node.isExpanded) {
            onExpansionToggled(ivArrow, true)
        } else {
            onExpansionToggled(ivArrow, false)
        }
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
}