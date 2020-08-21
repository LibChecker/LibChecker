package com.absinthe.libchecker.recyclerview.adapter

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.constant.DEX
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.LibType
import com.absinthe.libchecker.constant.librarymap.BaseMap
import com.absinthe.libchecker.utils.PackageUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.chip.Chip
import com.zhangyue.we.x2c.X2C
import com.zhangyue.we.x2c.ano.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Xml(layouts = ["item_lib_string"])
class LibStringAdapter(@LibType val type: Int) : BaseQuickAdapter<LibStringItem, BaseViewHolder>(0) {

    private val map: BaseMap = BaseMap.getMap(type)

    init {
        addChildClickViewIds(R.id.chip)
    }

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return createBaseViewHolder(X2C.inflate(context, R.layout.item_lib_string, parent, false))
    }

    override fun convert(holder: BaseViewHolder, item: LibStringItem) {
        holder.setText(R.id.tv_name, item.name)
        holder.setGone(R.id.tv_lib_size, item.size == 0L)

        if (item.size != 0L) {
            val text = if (type == DEX) {
                "${item.size} files"
            } else {
                PackageUtils.sizeToString(item.size)
            }
            holder.setText(R.id.tv_lib_size, text)
        }

        (context as BaseActivity).lifecycleScope.launch(Dispatchers.IO) {
            val libIcon = holder.getView<Chip>(R.id.chip)

            map.getChip(item.name)?.let {
                libIcon.apply {
                    withContext(Dispatchers.Main) {
                        setChipIconResource(it.iconRes)
                        text = it.name
                        visibility = View.VISIBLE

                        if (!GlobalValues.isColorfulIcon.value!!) {
                            chipDrawable.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                        }
                    }
                }
            } ?: withContext(Dispatchers.Main) {
                libIcon.visibility = View.GONE
            }
        }
    }
}