package com.absinthe.libchecker.recyclerview.adapter.snapshot

import android.view.ViewGroup
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.view.snapshot.TimeNodeItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeNodeAdapter : BaseQuickAdapter<TimeStampItem, BaseViewHolder>(0) {

    private val gson by lazy { Gson() }

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return BaseViewHolder(TimeNodeItemView(context))
    }

    override fun convert(holder: BaseViewHolder, item: TimeStampItem) {
        (holder.itemView as TimeNodeItemView).apply {
            name.text = getFormatDateString(item.timestamp)
            try {
                val list: List<String>? =
                    gson.fromJson(item.topApps, object : TypeToken<List<String>?>() {}.type)
                adapter.setList(list)
            } catch (e: JsonSyntaxException) {
                Timber.e(e)
            }
        }
    }

    fun getFormatDateString(timestamp: Long): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.getDefault())
        val date = Date(timestamp)
        return simpleDateFormat.format(date)
    }
}
