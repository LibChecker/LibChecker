package com.absinthe.libchecker.recyclerview.adapter.snapshot

import androidx.lifecycle.LifecycleCoroutineScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotComponentNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotNativeNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotTitleNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.provider.*
import com.chad.library.adapter.base.BaseNodeAdapter
import com.chad.library.adapter.base.entity.node.BaseNode

class SnapshotDetailAdapter(val lifecycleScope: LifecycleCoroutineScope) : BaseNodeAdapter() {

    init {
        addNodeProvider(SnapshotTitleProvider(lifecycleScope))
        addNodeProvider(SnapshotNativeProvider(lifecycleScope))
        addNodeProvider(SnapshotComponentProvider(lifecycleScope))
        addChildClickViewIds(R.id.chip)
    }

    override fun getItemType(data: List<BaseNode>, position: Int): Int {
        return when (data[position]) {
            is SnapshotTitleNode -> SNAPSHOT_TITLE_PROVIDER
            is SnapshotNativeNode -> SNAPSHOT_NATIVE_PROVIDER
            is SnapshotComponentNode -> SNAPSHOT_COMPONENT_PROVIDER
            else -> throw IllegalArgumentException("wrong snapshot provider item type")
        }
    }

}