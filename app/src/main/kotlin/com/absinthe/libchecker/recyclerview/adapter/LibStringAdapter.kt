package com.absinthe.libchecker.recyclerview.adapter

import android.content.res.ColorStateList
import android.view.View
import android.view.ViewStub
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.LibType
import com.absinthe.libchecker.constant.librarymap.BaseMap
import com.absinthe.libchecker.utils.PackageUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class LibStringAdapter(@LibType val type: Int) : BaseQuickAdapter<LibStringItem, BaseViewHolder>(R.layout.item_lib_string) {

    private val map: BaseMap = BaseMap.getMap(type)

    init {
        addChildClickViewIds(R.id.chip)
    }

    override fun convert(holder: BaseViewHolder, item: LibStringItem) {
        holder.setText(R.id.tv_name, item.name)
        holder.setGone(R.id.tv_lib_size, item.size == 0L)

        if (item.size != 0L) {
            holder.setText(R.id.tv_lib_size, PackageUtils.sizeToString(item.size))
        }

        (context as BaseActivity).lifecycleScope.launch(Dispatchers.IO) {
            val libIconStub = holder.getView<ViewStub>(R.id.stub_chip)

            map.getChip(item.name)?.let {
                libIconStub.apply {
                    withContext(Dispatchers.Main) {
                        val inflated: Chip = (inflate() as Chip).apply {
                            setChipIconResource(it.iconRes)
                            text = it.name
                        }

                        if (!GlobalValues.isColorfulIcon.value!!) {
                            inflated.chipIconTint = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    context,
                                    R.color.textNormal
                                )
                            )
                        }
                    }
                }
            } ?: withContext(Dispatchers.Main) {
                libIconStub.visibility = View.GONE
            }
        }
    }
}