package com.absinthe.libchecker.recyclerview.adapter.snapshot.provider

import android.animation.ObjectAnimator
import android.util.SparseArray
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.recyclerview.adapter.snapshot.SnapshotDetailCountAdapter
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.BaseSnapshotNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotDetailCountNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotTitleNode
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val SNAPSHOT_TITLE_PROVIDER = 1

class SnapshotTitleProvider(val lifecycleScope: LifecycleCoroutineScope) : BaseNodeProvider() {

    private val countMap = SparseArray<List<Int>>()
    override val itemViewType: Int = SNAPSHOT_TITLE_PROVIDER
    override val layoutId: Int = R.layout.item_snapshot_title

    override fun convert(helper: BaseViewHolder, item: BaseNode) {
        val node = item as SnapshotTitleNode
        val countAdapter = SnapshotDetailCountAdapter()
        val countList = mutableListOf(0, 0, 0, 0)
        val finalList = mutableListOf<SnapshotDetailCountNode>()
        val ivArrow = helper.getView<ImageView>(R.id.iv_arrow)

        val titleRes = when(node.type) {
            NATIVE -> R.string.ref_category_native
            SERVICE -> R.string.ref_category_service
            ACTIVITY -> R.string.ref_category_activity
            RECEIVER -> R.string.ref_category_br
            PROVIDER -> R.string.ref_category_cp
            else -> R.string.ref_category_perm
        }
        helper.setText(R.id.tv_title, context.getString(titleRes))
        helper.getView<RecyclerView>(R.id.rv_count).apply {
            adapter = countAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        countMap[node.type]?.let {
            for (i in it.indices) {
                if (it[i] != 0) {
                    finalList.add(SnapshotDetailCountNode(it[i], i))
                }
            }

            countAdapter.setList(finalList)
        } ?: let {
            lifecycleScope.launch(Dispatchers.IO) {
                @Suppress("UNCHECKED_CAST")
                (item.childNode as List<BaseSnapshotNode>).forEach { diffNode ->
                    countList[diffNode.item.diffType]++
                }

                countMap.put(node.type, countList)

                for (i in countList.indices) {
                    if (countList[i] != 0) {
                        finalList.add(SnapshotDetailCountNode(countList[i], i))
                    }
                }

                withContext(Dispatchers.Main) {
                    countAdapter.setList(finalList)
                }
            }
        }

        onExpansionToggled(ivArrow, node.isExpanded)
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