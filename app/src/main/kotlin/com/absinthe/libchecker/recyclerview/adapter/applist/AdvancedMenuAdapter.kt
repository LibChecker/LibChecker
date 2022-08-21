package com.absinthe.libchecker.recyclerview.adapter.applist

import android.view.ViewGroup
import com.absinthe.libchecker.bean.AdvancedMenuItem
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.view.app.CheckableChipView
import com.absinthe.libchecker.view.applist.AdvancedMenuItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AdvancedMenuAdapter : BaseQuickAdapter<AdvancedMenuItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(AdvancedMenuItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: AdvancedMenuItem) {
    (holder.itemView as AdvancedMenuItemView).chip.apply {
      text = context.getString(item.labelRes)
      isChecked = item.isSelected
      onCheckedChangeListener = { _: CheckableChipView, isChecked: Boolean ->
        val newOptions = if (isChecked) {
          GlobalValues.advancedOptions or item.flag
        } else {
          GlobalValues.advancedOptions and item.flag.inv()
        }
        GlobalValues.advancedOptions = newOptions
        GlobalValues.advancedOptionsLiveData.postValue(newOptions)
      }
    }
  }
}
