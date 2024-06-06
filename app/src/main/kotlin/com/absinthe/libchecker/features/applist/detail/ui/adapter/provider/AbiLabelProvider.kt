package com.absinthe.libchecker.features.applist.detail.ui.adapter.provider

import android.view.ViewGroup
import android.widget.ImageView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.features.applist.detail.ui.adapter.node.AbiLabelNode
import com.absinthe.libchecker.utils.extensions.dp
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import timber.log.Timber

const val ABI_LABEL_PROVIDER = 0

class AbiLabelProvider : BaseNodeProvider() {

  override val itemViewType: Int = ABI_LABEL_PROVIDER
  override val layoutId: Int = 0

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      ImageView(context).also {
        it.layoutParams = ViewGroup.LayoutParams(42.dp, 28.dp)
        it.scaleType = ImageView.ScaleType.CENTER_CROP
      }
    )
  }

  override fun convert(helper: BaseViewHolder, item: BaseNode) {
    val abi = (item as AbiLabelNode).abi
    Timber.d("abi=$abi")
    val res = when (abi) {
      Constants.ARMV8 -> R.drawable.ic_abi_label_arm64_v8a
      Constants.ARMV7 -> R.drawable.ic_abi_label_armeabi_v7a
      Constants.ARMV5 -> R.drawable.ic_abi_label_armeabi
      Constants.X86_64 -> R.drawable.ic_abi_label_x86_64
      Constants.X86 -> R.drawable.ic_abi_label_x86
      Constants.MIPS64 -> R.drawable.ic_abi_label_mips64
      Constants.MIPS -> R.drawable.ic_abi_label_mips
      Constants.MULTI_ARCH -> R.drawable.ic_abi_label_multi_arch
      else -> throw IllegalArgumentException("wrong abi label")
    }
    (helper.itemView as ImageView).also {
      it.setImageResource(res)
      it.alpha = if (item.active) 1f else 0.5f
    }
  }
}
