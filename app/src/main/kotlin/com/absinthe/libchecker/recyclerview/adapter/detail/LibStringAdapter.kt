package com.absinthe.libchecker.recyclerview.adapter.detail

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.graphics.text.LineBreaker
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.bean.DISABLED
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.recyclerview.adapter.HighlightAdapter
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.view.detail.ComponentLibItemView
import com.absinthe.libchecker.view.detail.NativeLibItemView
import com.absinthe.libchecker.view.detail.StaticLibItemView
import com.chad.library.adapter.base.viewholder.BaseViewHolder

private const val HIGHLIGHT_TRANSITION_DURATION = 250

class LibStringAdapter(@LibType val type: Int) : HighlightAdapter<LibStringItemChip>() {

  var highlightPosition: Int = -1
    private set

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return when (type) {
      NATIVE, METADATA -> createBaseViewHolder(NativeLibItemView(context))
      STATIC -> createBaseViewHolder(StaticLibItemView(context))
      else -> createBaseViewHolder(ComponentLibItemView(context))
    }
  }

  override fun convert(holder: BaseViewHolder, item: LibStringItemChip) {
    val itemName = if (item.item.source == DISABLED) {
      val sp = SpannableString(item.item.name)
      sp.setSpan(
        StrikethroughSpan(),
        0,
        item.item.name.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
      sp.setSpan(
        StyleSpan(Typeface.BOLD_ITALIC),
        0,
        item.item.name.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
      sp
    } else {
      item.item.name
    }

    when (type) {
      NATIVE -> {
        (holder.itemView as NativeLibItemView).apply {
          setOrHighlightText(libName, itemName)
          libSize.text = PackageUtils.sizeToString(context, item.item)
          setChip(item.chip)
        }
      }
      METADATA -> {
        (holder.itemView as NativeLibItemView).apply {
          setOrHighlightText(libName, itemName)
          libSize.text = item.item.source
          setChip(item.chip)
        }
      }
      STATIC -> {
        (holder.itemView as StaticLibItemView).apply {
          setOrHighlightText(libName, itemName)
          libDetail.let {
            if (OsUtils.atLeastQ()) {
              it.breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
            } else if (OsUtils.atLeastO()) {
              // noinspection WrongConstant
              it.breakStrategy = Layout.BREAK_STRATEGY_SIMPLE
            }
            val spannableString = SpannableString(item.item.source)
            val staticPrefixIndex =
              spannableString.indexOf(PackageUtils.STATIC_LIBRARY_SOURCE_PREFIX)
            spannableString.setSpan(
              StyleSpan(Typeface.BOLD),
              staticPrefixIndex,
              staticPrefixIndex + PackageUtils.STATIC_LIBRARY_SOURCE_PREFIX.length,
              Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            val versionCodePrefixIndex =
              spannableString.indexOf(PackageUtils.VERSION_CODE_PREFIX)
            spannableString.setSpan(
              StyleSpan(Typeface.BOLD),
              versionCodePrefixIndex,
              versionCodePrefixIndex + PackageUtils.VERSION_CODE_PREFIX.length,
              Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            it.text = spannableString
          }
          setChip(item.chip)
        }
      }
      else -> {
        (holder.itemView as ComponentLibItemView).apply {
          setOrHighlightText(libName, itemName)
          setChip(item.chip)
        }
      }
    }

    if (highlightPosition == -1 || holder.absoluteAdapterPosition != highlightPosition) {
      if (holder.itemView.background is TransitionDrawable) {
        (holder.itemView.background as TransitionDrawable).reverseTransition(
          HIGHLIGHT_TRANSITION_DURATION
        )
      }
      holder.itemView.background = null
    } else {
      val drawable = TransitionDrawable(
        listOf(
          ColorDrawable(Color.TRANSPARENT),
          ColorDrawable(R.color.highlight_component.getColor(context))
        ).toTypedArray()
      )
      holder.itemView.background = drawable
      if (holder.itemView.background is TransitionDrawable) {
        (holder.itemView.background as TransitionDrawable).startTransition(
          HIGHLIGHT_TRANSITION_DURATION
        )
      }
    }
  }

  fun setHighlightBackgroundItem(position: Int) {
    if (position < 0) {
      return
    }
    highlightPosition = position
  }
}
