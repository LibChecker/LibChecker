package com.absinthe.libchecker.domain.snapshot.timenode.ui.adapter

import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotTimeNodeItem
import com.absinthe.libchecker.domain.snapshot.timenode.ui.view.TimeNodeItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class TimeNodeAdapter : BaseQuickAdapter<SnapshotTimeNodeItem, BaseViewHolder>(0) {

  private var packageIconSources: Map<String, SnapshotPackageIconSource> = emptyMap()

  fun setPackageIconSources(packageIconSources: Map<String, SnapshotPackageIconSource>) {
    this.packageIconSources = packageIconSources
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(TimeNodeItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: SnapshotTimeNodeItem) {
    (holder.itemView as TimeNodeItemView).apply {
      name.text = item.timestampText
      contentDescription = item.description
      adapter.setPackageIconSources(packageIconSources)
      adapter.setList(item.topAppPackageNames)
      if (item.topAppPackageNames.size <= 5) {
        adapter.removeAllFooterView()
      } else {
        adapter.setFooterView(
          AppCompatTextView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT,
              ViewGroup.LayoutParams.WRAP_CONTENT
            ).also {
              gravity = Gravity.CENTER_VERTICAL
            }
            // noinspection AndroidLintSetTextI18n
            text = "…"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
          }
        )
      }
    }
  }
}
