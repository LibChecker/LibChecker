package com.absinthe.libchecker.features.about

import android.content.Intent
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import coil.load
import coil.transform.CircleCropTransformation
import com.absinthe.libchecker.utils.Toasty
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import timber.log.Timber

class DeveloperInfoAdapter : BaseQuickAdapter<DeveloperInfo, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      DeveloperItemView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      }
    )
  }

  override fun convert(holder: BaseViewHolder, item: DeveloperInfo) {
    (holder.itemView as DeveloperItemView).apply {
      container.icon.load(item.avatarRes) {
        transformations(CircleCropTransformation())
      }
      container.name.text = item.name
      container.desc.text = item.desc
      setOnClickListener {
        val context = it.context
        runCatching {
          CustomTabsIntent.Builder().build().apply {
            launchUrl(context, item.github.toUri())
          }
        }.onFailure { t ->
          Timber.e(t)
          runCatching {
            val intent = Intent(Intent.ACTION_VIEW)
              .setData(item.github.toUri())
            context.startActivity(intent)
          }.onFailure { inner ->
            Timber.e(inner)
            Toasty.showShort(context, "No browser application")
          }
        }
      }
    }
  }
}
