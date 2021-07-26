package com.absinthe.libchecker.recyclerview.adapter.snapshot.provider

import android.content.res.ColorStateList
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.bean.ADDED
import com.absinthe.libchecker.bean.CHANGED
import com.absinthe.libchecker.bean.MOVED
import com.absinthe.libchecker.bean.REMOVED
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotComponentNode
import com.absinthe.libchecker.ui.fragment.detail.LibDetailDialogFragment
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.view.snapshot.SnapshotDetailComponentView
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.coroutines.launch

const val SNAPSHOT_COMPONENT_PROVIDER = 3

class SnapshotComponentProvider(val lifecycleScope: LifecycleCoroutineScope) : BaseNodeProvider() {

  override val itemViewType: Int = SNAPSHOT_COMPONENT_PROVIDER
  override val layoutId: Int = 0

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      SnapshotDetailComponentView(ContextThemeWrapper(context, R.style.AppListMaterialCard))
    )
  }

  override fun convert(helper: BaseViewHolder, item: BaseNode) {
    (helper.itemView as SnapshotDetailComponentView).container.apply {
      val snapshotItem = (item as SnapshotComponentNode).item

      name.text = snapshotItem.title

      val colorRes = when (snapshotItem.diffType) {
        ADDED -> R.color.material_green_300
        REMOVED -> R.color.material_red_300
        CHANGED -> R.color.material_yellow_300
        MOVED -> R.color.material_blue_300
        else -> throw IllegalArgumentException("wrong diff type")
      }

      typeIcon.setImageResource(
        when (snapshotItem.diffType) {
          ADDED -> R.drawable.ic_add
          REMOVED -> R.drawable.ic_remove
          CHANGED -> R.drawable.ic_changed
          MOVED -> R.drawable.ic_move
          else -> throw IllegalArgumentException("wrong diff type")
        }
      )

      helper.itemView.backgroundTintList =
        ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))

      lifecycleScope.launch {
        val rule = LCAppUtils.getRuleWithRegex(snapshotItem.name, snapshotItem.itemType)

        setChip(rule, colorRes)
        if (rule != null) {
          setChipOnClickListener {
            val name = item.item.name
            val regexName =
              LCAppUtils.findRuleRegex(name, item.item.itemType)?.regexName
            LibDetailDialogFragment.newInstance(name, item.item.itemType, regexName)
              .apply {
                show(
                  (this@SnapshotComponentProvider.context as BaseActivity<*>).supportFragmentManager,
                  tag
                )
              }
          }
        } else {
          setChipOnClickListener(null)
        }
      }
    }
  }
}
