package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.provider

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.ui.dialog.LibDetailDialogFragment
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotComponentNode
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotDetailComponentView
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.libraries.utils.utils.UiUtils
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder

const val SNAPSHOT_COMPONENT_PROVIDER = 3

class SnapshotComponentProvider : BaseNodeProvider() {

  override val itemViewType: Int = SNAPSHOT_COMPONENT_PROVIDER
  override val layoutId: Int = 0

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      SnapshotDetailComponentView(ContextThemeWrapper(context, R.style.AppListMaterialCard)).also {
        it.layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      }
    )
  }

  override fun convert(helper: BaseViewHolder, item: BaseNode) {
    (helper.itemView as SnapshotDetailComponentView).container.apply {
      val node = item as SnapshotComponentNode
      val displayData = node.displayData

      name.text = displayData.title
      typeIcon.setImageResource(displayData.status.iconRes)

      val baseColor = displayData.status.colorRes.getColor(context)
      val alpha = if (UiUtils.isDarkMode()) {
        (0.75f * 255).toInt() and 0xFF
      } else {
        (0.95f * 255).toInt() and 0xFF
      }
      val alphaColor = (baseColor and 0x00FFFFFF) or (alpha shl 24)
      background = alphaColor.toDrawable()

      val rule = displayData.rule
      setChip(rule, alphaColor, displayData.colorfulRuleIcon)
      helper.itemView.contentDescription = displayData.description
      if (rule != null) {
        setChipOnClickListener {
          if (AntiShakeUtils.isInvalidClick(it)) {
            return@setChipOnClickListener
          }
          val name = displayData.item.name
          val fragmentManager =
            (this@SnapshotComponentProvider.context as BaseActivity<*>).supportFragmentManager
          LibDetailDialogFragment.newInstance(name, displayData.item.itemType, rule.regexName)
            .show(fragmentManager, LibDetailDialogFragment::class.java.name)
        }
      } else {
        setChipOnClickListener(null)
      }
    }
  }
}
