package com.absinthe.libchecker.features.snapshot.detail.ui.adapter

import com.absinthe.libchecker.domain.snapshot.GetSnapshotRuleUseCase
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.node.SnapshotComponentNode
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.node.SnapshotNativeNode
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.node.SnapshotTitleNode
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.provider.SNAPSHOT_COMPONENT_PROVIDER
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.provider.SNAPSHOT_NATIVE_PROVIDER
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.provider.SNAPSHOT_TITLE_PROVIDER
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.provider.SnapshotComponentProvider
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.provider.SnapshotNativeProvider
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.provider.SnapshotTitleProvider
import com.chad.library.adapter.base.BaseNodeAdapter
import com.chad.library.adapter.base.entity.node.BaseNode

class SnapshotDetailAdapter(
  private val colorfulRuleIcon: Boolean,
  private val getSnapshotRuleUseCase: GetSnapshotRuleUseCase
) : BaseNodeAdapter() {

  init {
    addNodeProvider(SnapshotTitleProvider())
    addNodeProvider(SnapshotNativeProvider(colorfulRuleIcon, getSnapshotRuleUseCase))
    addNodeProvider(SnapshotComponentProvider(colorfulRuleIcon, getSnapshotRuleUseCase))
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
