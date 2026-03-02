package com.absinthe.libchecker.features.snapshot.ui.adapter

import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.features.snapshot.ui.view.TimeNodeItemView
import com.absinthe.libchecker.utils.fromJson
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import timber.log.Timber

class TimeNodeAdapter : BaseQuickAdapter<TimeStampItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(TimeNodeItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: TimeStampItem) {
    (holder.itemView as TimeNodeItemView).apply {
      name.text = getFormatDateString(item.timestamp)
      try {
        item.topApps?.let {
          val list = it.fromJson<List<String>>(
            List::class.java,
            String::class.java
          )
          adapter.setList(list)
          if ((list?.size ?: 0) <= 5) {
            adapter.removeAllFooterView()
          } else {
            adapter.setFooterView(
              AppCompatTextView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                  ViewGroup.LayoutParams.WRAP_CONTENT,
                  ViewGroup.LayoutParams.WRAP_CONTENT
                ).also {
                  gravity = Gravity.CENTER_VERTICAL
                }
                // noinspection AndroidLintSetTextI18n
                text = "…"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
              }
            )
          }
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
