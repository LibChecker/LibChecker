package com.absinthe.libchecker.features.snapshot.detail.ui.adapter.provider

import android.animation.ObjectAnimator
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.SnapshotDetailCountAdapter
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.node.BaseSnapshotNode
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.node.SnapshotDetailCountNode
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.node.SnapshotTitleNode
import com.absinthe.libchecker.features.snapshot.detail.ui.view.SnapshotDetailTitleView
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val SNAPSHOT_TITLE_PROVIDER = 1

class SnapshotTitleProvider : BaseNodeProvider() {

  override val itemViewType: Int = SNAPSHOT_TITLE_PROVIDER
  override val layoutId: Int = 0

  private val countMap = SparseArray<List<Int>>()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(SnapshotDetailTitleView(context))
  }

  override fun convert(helper: BaseViewHolder, item: BaseNode) {
    val itemView = (helper.itemView as SnapshotDetailTitleView).container
    val node = item as SnapshotTitleNode
    val countAdapter = SnapshotDetailCountAdapter()
    val countList = mutableListOf(0, 0, 0, 0)
    val finalList = mutableListOf<SnapshotDetailCountNode>()

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

    countMap[node.type]?.let {
      for (i in it.indices) {
        if (it[i] != 0) {
          finalList.add(SnapshotDetailCountNode(it[i], i))
        }
      }

      countAdapter.setList(finalList)
    } ?: run {
      (context as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.Default) {
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
}
