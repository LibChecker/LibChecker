package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageButton
import com.absinthe.libchecker.R
import com.absinthe.libchecker.features.applist.detail.bean.FeatureItem
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.dp

class FeatureLabelView(context: Context) : AppCompatImageButton(context) {

  init {
    layoutParams = ViewGroup.MarginLayoutParams(36.dp, 36.dp).also {
      it.marginEnd = 8.dp
    }
    setBackgroundResource(R.drawable.ripple_feature_label_36dp)
  }

  fun setFeature(item: FeatureItem) {
    item.colorFilterInt?.let {
      val drawable = UiUtils.changeDrawableColor(context, item.res, it)
      setImageDrawable(drawable)
    } ?: run {
      setImageResource(item.res)
    }
    setOnClickListener {
      item.action()
    }
  }
}
