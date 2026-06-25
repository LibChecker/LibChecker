package com.absinthe.libchecker.features.snapshot.detail.ui.adapter.provider

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.GetSnapshotRuleUseCase
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.CHANGED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import com.absinthe.libchecker.features.applist.detail.ui.LibDetailDialogFragment
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.node.SnapshotNativeNode
import com.absinthe.libchecker.features.snapshot.detail.ui.view.SnapshotDetailNativeView
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.libraries.utils.utils.UiUtils
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.coroutines.launch

const val SNAPSHOT_NATIVE_PROVIDER = 2

class SnapshotNativeProvider(
  private val colorfulRuleIcon: Boolean,
  private val getSnapshotRuleUseCase: GetSnapshotRuleUseCase
) : BaseNodeProvider() {

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
      helper.itemView.contentDescription = buildItemDescription(
        getStatusLabel(snapshotItem.diffType),
        snapshotItem.title,
        snapshotItem.extra
      )

      val baseColor = colorRes.getColor(context)
      val alpha = if (UiUtils.isDarkMode()) {
        (0.75f * 255).toInt() and 0xFF
      } else {
        (0.95f * 255).toInt() and 0xFF
      }
      val alphaColor = (baseColor and 0x00FFFFFF) or (alpha shl 24)
      background = alphaColor.toDrawable()

      (this@SnapshotNativeProvider.context as? LifecycleOwner)?.lifecycleScope?.launch {
        val rule = getSnapshotRuleUseCase(snapshotItem)

        setChip(rule, alphaColor, colorfulRuleIcon)
        helper.itemView.contentDescription = buildItemDescription(
          getStatusLabel(snapshotItem.diffType),
          snapshotItem.title,
          snapshotItem.extra,
          rule?.label
        )
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

  private fun buildItemDescription(vararg parts: CharSequence?): String {
    return parts
      .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
      .joinToString()
  }

  private fun getStatusLabel(status: Int): String {
    return context.getString(
      when (status) {
        ADDED -> R.string.snapshot_indicator_added
        REMOVED -> R.string.snapshot_indicator_removed
        CHANGED -> R.string.snapshot_indicator_changed
        else -> android.R.string.untitled
      }
    )
  }
}
