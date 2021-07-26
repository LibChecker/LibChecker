package com.absinthe.libchecker.recyclerview.adapter.snapshot.provider

import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.bean.ADDED
import com.absinthe.libchecker.bean.CHANGED
import com.absinthe.libchecker.bean.REMOVED
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotNativeNode
import com.absinthe.libchecker.ui.fragment.detail.LibDetailDialogFragment
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.extensions.toColorStateList
import com.absinthe.libchecker.view.snapshot.SnapshotDetailNativeView
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.coroutines.launch

const val SNAPSHOT_NATIVE_PROVIDER = 2

class SnapshotNativeProvider(val lifecycleScope: LifecycleCoroutineScope) : BaseNodeProvider() {

  override val itemViewType: Int = SNAPSHOT_NATIVE_PROVIDER
  override val layoutId: Int = 0

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      SnapshotDetailNativeView(ContextThemeWrapper(context, R.style.AppListMaterialCard))
    )
  }

  override fun convert(helper: BaseViewHolder, item: BaseNode) {
    (helper.itemView as SnapshotDetailNativeView).container.apply {
      val snapshotItem = (item as SnapshotNativeNode).item

      name.text = snapshotItem.title
      libSize.text = snapshotItem.extra

      val colorRes = when (snapshotItem.diffType) {
        ADDED -> R.color.material_green_300
        REMOVED -> R.color.material_red_300
        CHANGED -> R.color.material_yellow_300
        else -> Color.TRANSPARENT
      }

      typeIcon.setImageResource(
        when (snapshotItem.diffType) {
          ADDED -> R.drawable.ic_add
          REMOVED -> R.drawable.ic_remove
          CHANGED -> R.drawable.ic_changed
          else -> Color.TRANSPARENT
        }
      )

      helper.itemView.backgroundTintList = colorRes.toColorStateList(context)

      lifecycleScope.launch {
        val rule = LCAppUtils.getRuleWithRegex(snapshotItem.name, NATIVE)

        setChip(rule, colorRes)
        if (rule != null) {
          setChipOnClickListener {
            val name = item.item.name
            val regexName =
              LCAppUtils.findRuleRegex(name, item.item.itemType)?.regexName
            LibDetailDialogFragment.newInstance(name, item.item.itemType, regexName)
              .apply {
                show(
                  (this@SnapshotNativeProvider.context as BaseActivity<*>).supportFragmentManager,
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
