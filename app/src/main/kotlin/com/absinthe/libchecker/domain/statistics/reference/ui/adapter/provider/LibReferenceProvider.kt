package com.absinthe.libchecker.domain.statistics.reference.ui.adapter.provider

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.buildSpannedString
import androidx.core.text.italic
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.isComponentType
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.ui.view.LibReferenceItemView
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.tintHighlightText
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import java.text.NumberFormat

const val LIB_REFERENCE_PROVIDER = 0

class LibReferenceProvider(
  private val colorfulRuleIcon: () -> Boolean,
  private val highlightText: () -> String,
  private val onDetailIconClick: (LibReference) -> Unit
) : BaseNodeProvider() {

  override val itemViewType: Int = LIB_REFERENCE_PROVIDER
  override val layoutId: Int = 0

  init {
    addChildClickViewIds(android.R.id.icon)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      LibReferenceItemView(context).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
          val margin = context.getDimensionPixelSize(R.dimen.main_card_margin)
          it.setMargins(0, margin, 0, margin)
        }
      }
    )
  }

  override fun convert(helper: BaseViewHolder, item: BaseNode) {
    (helper.itemView as LibReferenceItemView).container.apply {
      val libReferenceItem = item as LibReference
      val canOpenDetail = libReferenceItem.type == NATIVE ||
        isComponentType(libReferenceItem.type) ||
        libReferenceItem.type == ACTION
      count.text = NumberFormat.getIntegerInstance().format(libReferenceItem.referredList.size)
      icon.importantForAccessibility = if (canOpenDetail) {
        View.IMPORTANT_FOR_ACCESSIBILITY_YES
      } else {
        View.IMPORTANT_FOR_ACCESSIBILITY_NO
      }

      setOrHighlightText(libName, libReferenceItem.libName)

      libReferenceItem.rule?.let {
        icon.apply {
          setImageResource(it.iconRes)
          contentDescription = it.label

          if (!colorfulRuleIcon() && !it.isSimpleColorIcon) {
            this.drawable.mutate().colorFilter =
              ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
          }
        }

        setOrHighlightText(labelName, it.label)
      } ?: let {
        val isAndroidGroupPermission = libReferenceItem.type == PERMISSION && libReferenceItem.libName.startsWith("android.permission")
        val isAndroidGroupAction = libReferenceItem.type == ACTION && libReferenceItem.libName.startsWith("android.intent.action")
        if (isAndroidGroupPermission || isAndroidGroupAction) {
          icon.setImageResource(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android)
        } else {
          icon.setImageResource(R.drawable.ic_question)
        }
        icon.contentDescription = libReferenceItem.libName

        labelName.text = buildSpannedString {
          italic {
            append(context.getString(R.string.not_marked_lib))
          }
          // prevent text clipping
          append(" ")
        }
      }
      helper.itemView.contentDescription = buildItemDescription(
        labelName.text,
        libName.text,
        count.text
      )
    }
  }

  override fun onChildClick(helper: BaseViewHolder, view: View, data: BaseNode, position: Int) {
    super.onChildClick(helper, view, data, position)
    if (view.id == android.R.id.icon) {
      val ref = data as? LibReference ?: return
      if (ref.type == NATIVE || isComponentType(ref.type) || ref.type == ACTION) {
        onDetailIconClick(ref)
      }
    }
  }

  private fun setOrHighlightText(view: TextView, text: CharSequence) {
    val keyword = highlightText()
    if (keyword.isNotBlank()) {
      view.tintHighlightText(keyword, text)
    } else {
      view.text = text
    }
  }

  private fun buildItemDescription(vararg parts: CharSequence?): String {
    return parts
      .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
      .joinToString()
  }
}
