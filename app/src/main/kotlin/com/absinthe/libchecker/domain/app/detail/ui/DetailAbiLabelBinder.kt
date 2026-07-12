package com.absinthe.libchecker.domain.app.detail.ui

import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.domain.app.detail.abi.AppDetailAbiLabelData
import com.absinthe.libchecker.domain.app.detail.ui.adapter.node.AbiLabelNode
import com.absinthe.libchecker.domain.app.detail.ui.view.DetailsTitleView

class DetailAbiLabelBinder(
  private val activity: FragmentActivity,
  private val detailsTitleView: DetailsTitleView,
  private val tintAbiLabels: () -> Boolean
) {
  fun bind(abiLabelData: AppDetailAbiLabelData) {
    val abiLabelsList = abiLabelData.labels.map { label ->
      val action = if (label.opensMultiArchInfo) {
        { FeaturesDialog.showMultiArchDialog(activity) }
      } else {
        null
      }
      AbiLabelNode(
        abi = label.abi,
        active = label.isActive,
        contentDescription = label.contentDescription,
        is64Bit = label.is64Bit,
        action = action
      )
    }

    if (abiLabelsList.isNotEmpty()) {
      detailsTitleView.setAbiLabels(
        abiLabelsList,
        tintAbiLabels = tintAbiLabels()
      )
    }
  }
}
