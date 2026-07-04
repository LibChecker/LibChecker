package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.provider

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.ui.dialog.LibDetailDialogFragment
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SNAPSHOT_NATIVE_PROVIDER
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotNativeNode
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotDetailNativeView
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class SnapshotNativeProvider : BaseNodeProvider() {

  override val itemViewType: Int = SNAPSHOT_NATIVE_PROVIDER
  override val layoutId: Int = 0

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      SnapshotDetailNativeView(ContextThemeWrapper(context, R.style.AppListMaterialCard)).also {
        it.layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      }
    )
  }

  override fun convert(helper: BaseViewHolder, item: BaseNode) {
    (helper.itemView as SnapshotDetailNativeView).container.apply {
      val node = item as SnapshotNativeNode
      val displayData = node.displayData

      name.text = displayData.title
      libSize.text = displayData.extra
      typeIcon.setImageResource(displayData.status.iconRes)

      background = displayData.backgroundColor.toDrawable()

      val ruleChip = displayData.ruleChip
      setChip(ruleChip, displayData.backgroundColor)
      helper.itemView.contentDescription = displayData.description
      if (ruleChip != null) {
        setChipOnClickListener {
          if (AntiShakeUtils.isInvalidClick(it)) {
            return@setChipOnClickListener
          }
          val name = displayData.item.name
          val fragmentManager =
            (this@SnapshotNativeProvider.context as BaseActivity<*>).supportFragmentManager
          LibDetailDialogFragment.newInstance(name, displayData.item.itemType, ruleChip.regexName)
            .show(fragmentManager, LibDetailDialogFragment::class.java.name)
        }
      } else {
        setChipOnClickListener(null)
      }
    }
  }
}
