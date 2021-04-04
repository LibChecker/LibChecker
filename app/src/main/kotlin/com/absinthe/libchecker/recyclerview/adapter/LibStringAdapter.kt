package com.absinthe.libchecker.recyclerview.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.bean.DISABLED
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.view.detail.ComponentLibItemView
import com.absinthe.libchecker.view.detail.NativeLibItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder


private const val HIGHLIGHT_TRANSITION_DURATION = 250

class LibStringAdapter(@LibType val type: Int) : BaseQuickAdapter<LibStringItemChip, BaseViewHolder>(0) {

    private var highlightPosition: Int = -1

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (type) {
            NATIVE -> createBaseViewHolder(NativeLibItemView(context))
            else -> createBaseViewHolder(ComponentLibItemView(context))
        }
    }

    @SuppressLint("SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: LibStringItemChip) {
        val itemName = if (item.item.source == DISABLED) {
            val sp = SpannableString(item.item.name)
            sp.setSpan(StrikethroughSpan(), 0, item.item.name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sp.setSpan(StyleSpan(Typeface.BOLD_ITALIC), 0, item.item.name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sp
        } else {
            item.item.name
        }

        if (type == NATIVE) {
            (holder.itemView as NativeLibItemView).apply {
                libName.text = itemName
                if (item.item.source?.startsWith(PackageUtils.STATIC_LIBRARY_SOURCE_PREFIX) == true) {
                    libSize.let{
                        it.text = "${item.item.source}\n${PackageUtils.VERSION_CODE_PREFIX}${item.item.size}"
                        it.post {
                            it.text = autoSplitText(it)
                            val spannableString = SpannableString(it.text)
                            val staticPrefixIndex = spannableString.indexOf(PackageUtils.STATIC_LIBRARY_SOURCE_PREFIX)
                            spannableString.setSpan(StyleSpan(Typeface.BOLD), staticPrefixIndex, staticPrefixIndex + PackageUtils.STATIC_LIBRARY_SOURCE_PREFIX.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                            val versionCodePrefixIndex = spannableString.indexOf(PackageUtils.VERSION_CODE_PREFIX)
                            spannableString.setSpan(StyleSpan(Typeface.BOLD), versionCodePrefixIndex, versionCodePrefixIndex + PackageUtils.VERSION_CODE_PREFIX.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                            it.text = spannableString
                        }
                    }
                } else {
                    libSize.text = PackageUtils.sizeToString(item.item)
                }
                setChip(item.chip)
            }
        } else {
            (holder.itemView as ComponentLibItemView).apply {
                libName.text = itemName
                setChip(item.chip)
            }
        }

        if (highlightPosition == -1 || holder.absoluteAdapterPosition != highlightPosition) {
            if (holder.itemView.background is TransitionDrawable) {
                (holder.itemView.background as TransitionDrawable).reverseTransition(HIGHLIGHT_TRANSITION_DURATION)
            }
            holder.itemView.background = null
        } else {
            val drawable = TransitionDrawable(
                listOf(
                    ColorDrawable(Color.TRANSPARENT),
                    ColorDrawable(ContextCompat.getColor(context, R.color.highlightComponent))
                ).toTypedArray()
            )
            holder.itemView.background = drawable
            if (holder.itemView.background is TransitionDrawable) {
                (holder.itemView.background as TransitionDrawable).startTransition(HIGHLIGHT_TRANSITION_DURATION)
            }
        }
    }

    fun setHighlightBackgroundItem(position: Int) {
        if (position < 0) {
            return
        }
        highlightPosition = position
    }

    private fun autoSplitText(tv: TextView): String {
        val rawText = tv.text.toString()
        val tvPaint: Paint = tv.paint
        val tvWidth = (tv.measuredWidth - tv.paddingStart - tv.paddingEnd).toFloat()

        val rawTextLines = rawText.replace("\r".toRegex(), "").split("\n".toRegex()).toTypedArray()
        val sbNewText = StringBuilder()

        for (rawTextLine in rawTextLines) {
            if (tvPaint.measureText(rawTextLine) <= tvWidth) {
                sbNewText.append(rawTextLine)
            } else {
                var lineWidth = 0f
                var cnt = 0

                while (cnt != rawTextLine.length) {
                    val ch = rawTextLine[cnt]
                    lineWidth += tvPaint.measureText(ch.toString())

                    if (lineWidth <= tvWidth) {
                        sbNewText.append(ch)
                    } else {
                        sbNewText.append("\n")
                        lineWidth = 0f
                        --cnt
                    }
                    cnt++
                }
            }
            sbNewText.append("\n")
        }

        if (!rawText.endsWith("\n")) {
            sbNewText.deleteCharAt(sbNewText.length - 1)
        }
        return sbNewText.toString()
    }
}