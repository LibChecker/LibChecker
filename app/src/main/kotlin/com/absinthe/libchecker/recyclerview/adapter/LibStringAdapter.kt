package com.absinthe.libchecker.recyclerview.adapter

import android.content.res.ColorStateList
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.librarymap.*
import com.absinthe.libchecker.utils.PackageUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibStringAdapter : BaseQuickAdapter<LibStringItem, BaseViewHolder>(R.layout.item_lib_string) {

    var mode = Mode.NATIVE

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
            val libIcon = holder.getView<Chip>(R.id.chip)

            val map: BaseMap = when (mode) {
                Mode.NATIVE -> NativeLibMap
                Mode.SERVICE -> ServiceLibMap
                Mode.ACTIVITY -> ActivityLibMap
                Mode.RECEIVER -> ReceiverLibMap
                Mode.PROVIDER -> ProviderLibMap
            }

            map.getChip(item.name)?.let {
                libIcon.apply {
                    withContext(Dispatchers.Main) {
                        setChipIconResource(it.iconRes)
                        text = it.name
                        visibility = View.VISIBLE

                        if (!GlobalValues.isColorfulIcon.value!!) {
                            libIcon.chipIconTint = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    context,
                                    R.color.textNormal
                                )
                            )
                        }
                    }
                }
            } ?: withContext(Dispatchers.Main) {
                libIcon.visibility = View.GONE
            }
        }
    }

    enum class Mode {
        NATIVE,
        SERVICE,
        ACTIVITY,
        RECEIVER,
        PROVIDER
    }
}