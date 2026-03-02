package com.absinthe.libchecker.features.snapshot.ui.adapter

import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.dp
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class TimeNodeItemAdapter : BaseQuickAdapter<String, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      AppCompatImageView(context).apply {
        layoutParams = ViewGroup.LayoutParams(24.dp, 24.dp)
        setPadding(0, 0, 4.dp, 0)
      }
    )
  }

  override fun convert(holder: BaseViewHolder, item: String) {
    (holder.itemView as AppCompatImageView).also { imageView ->
      runCatching {
        imageView.load(PackageUtils.getPackageInfo(item))
      }.onFailure {
        imageView.load(R.drawable.ic_icon_blueprint)
      }
    }
  }
}
