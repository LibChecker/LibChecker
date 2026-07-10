package com.absinthe.libchecker.domain.snapshot.timenode.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.utils.extensions.dp
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class TimeNodeItemAdapter : BaseQuickAdapter<String, BaseViewHolder>(0) {

  private var packageIconSources: Map<String, SnapshotPackageIconSource> = emptyMap()

  fun bind(
    packageNames: List<String>,
    packageIconSources: Map<String, SnapshotPackageIconSource>
  ) {
    this.packageIconSources = packageIconSources
    setList(packageNames)
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      AppCompatImageView(context).apply {
        layoutParams = ViewGroup.LayoutParams(24.dp, 24.dp)
        setPadding(0, 0, 4.dp, 0)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
      }
    )
  }

  override fun convert(holder: BaseViewHolder, item: String) {
    (holder.itemView as AppCompatImageView).also { imageView ->
      when (val iconSource = packageIconSources[item]) {
        is SnapshotPackageIconSource.InstalledPackage -> imageView.load(iconSource.packageInfo)

        SnapshotPackageIconSource.Fallback,
        null -> imageView.load(R.drawable.ic_icon_blueprint)
      }
    }
  }
}
