package com.absinthe.libchecker.recyclerview.adapter.snapshot.provider

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.bean.ADDED
import com.absinthe.libchecker.bean.CHANGED
import com.absinthe.libchecker.bean.MOVED
import com.absinthe.libchecker.bean.REMOVED
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotComponentNode
import com.absinthe.libchecker.ui.fragment.detail.LibDetailDialogFragment
import com.absinthe.libchecker.utils.extensions.toColorStateList
import com.absinthe.libchecker.view.snapshot.SnapshotDetailComponentView
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.rulesbundle.LCRules
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.coroutines.launch

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

      helper.itemView.backgroundTintList = colorRes.toColorStateList(context)

      (context as? LifecycleOwner)?.lifecycleScope?.launch {
        val rule = LCRules.getRule(snapshotItem.name, snapshotItem.itemType, true)

        setChip(rule, colorRes)
        if (rule != null) {
          setChipOnClickListener {
            if (AntiShakeUtils.isInvalidClick(it)) {
              return@setChipOnClickListener
            }
            val name = item.item.name
            LibDetailDialogFragment.newInstance(name, item.item.itemType, rule.regexName)
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
