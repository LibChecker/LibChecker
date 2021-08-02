package com.absinthe.libchecker.recyclerview.adapter.snapshot

import android.view.ViewGroup
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libchecker.view.snapshot.TimeNodeItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import timber.log.Timber
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeNodeAdapter : BaseQuickAdapter<TimeStampItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(TimeNodeItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: TimeStampItem) {
    (holder.itemView as TimeNodeItemView).apply {
      name.text = getFormatDateString(item.timestamp)
      try {
        item.topApps?.let {
          adapter.setList(it.fromJson<List<String>>(
            List::class.java,
            String::class.java
          ))
        }
      } catch (e: IOException) {
        Timber.e(e)
      }
    }
  }

  private fun getFormatDateString(timestamp: Long): String {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.getDefault())
    val date = Date(timestamp)
    return simpleDateFormat.format(date)
  }
}
