package com.absinthe.libchecker.features.snapshot.detail.ui.adapter.provider

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.features.applist.detail.ui.LibDetailDialogFragment
import com.absinthe.libchecker.features.snapshot.detail.bean.ADDED
import com.absinthe.libchecker.features.snapshot.detail.bean.CHANGED
import com.absinthe.libchecker.features.snapshot.detail.bean.REMOVED
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.node.SnapshotNativeNode
import com.absinthe.libchecker.features.snapshot.detail.ui.view.SnapshotDetailNativeView
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.utils.extensions.toColorStateList
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.rulesbundle.LCRules
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val SNAPSHOT_NATIVE_PROVIDER = 2

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
      val snapshotItem = (item as SnapshotNativeNode).item

      name.text = snapshotItem.title
      libSize.text = snapshotItem.extra

      val colorRes = when (snapshotItem.diffType) {
        ADDED -> R.color.material_green_300
        REMOVED -> R.color.material_red_300
        CHANGED -> R.color.material_yellow_300
        else -> throw IllegalArgumentException("wrong diff type")
      }

      typeIcon.setImageResource(
        when (snapshotItem.diffType) {
          ADDED -> R.drawable.ic_add
          REMOVED -> R.drawable.ic_remove
          CHANGED -> R.drawable.ic_changed
          else -> throw IllegalArgumentException("wrong diff type")
        }
      )

      helper.itemView.backgroundTintList = colorRes.toColorStateList(context)

      (this@SnapshotNativeProvider.context as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
        val rule = LCRules.getRule(snapshotItem.name, snapshotItem.itemType, true)

        withContext(Dispatchers.Main) {
          setChip(rule, colorRes)
          if (rule != null) {
            setChipOnClickListener {
              if (AntiShakeUtils.isInvalidClick(it)) {
                return@setChipOnClickListener
              }
              val name = item.item.name
              val fragmentManager =
                (this@SnapshotNativeProvider.context as BaseActivity<*>).supportFragmentManager
              LibDetailDialogFragment.newInstance(name, item.item.itemType, rule.regexName)
                .show(fragmentManager, LibDetailDialogFragment::class.java.name)
            }
          } else {
            setChipOnClickListener(null)
          }
        }
      }
    }
  }
}
