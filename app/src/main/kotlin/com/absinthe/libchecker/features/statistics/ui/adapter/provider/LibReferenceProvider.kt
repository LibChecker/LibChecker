package com.absinthe.libchecker.features.statistics.ui.adapter.provider

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.buildSpannedString
import androidx.core.text.italic
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.features.applist.detail.ui.LibDetailDialogFragment
import com.absinthe.libchecker.features.statistics.bean.LibReference
import com.absinthe.libchecker.features.statistics.ui.adapter.LibReferenceAdapter
import com.absinthe.libchecker.features.statistics.ui.view.LibReferenceItemView
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.tintHighlightText
import com.absinthe.rulesbundle.LCRules
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val LIB_REFERENCE_PROVIDER = 0

class LibReferenceProvider : BaseNodeProvider() {

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
      count.text = libReferenceItem.referredList.size.toString()

      setOrHighlightText(libName, libReferenceItem.libName)

      libReferenceItem.rule?.let {
        icon.apply {
          setImageResource(it.iconRes)

          if (!GlobalValues.isColorfulIcon && !it.isSimpleColorIcon) {
            this.drawable.mutate().colorFilter =
              ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
          }
        }

        setOrHighlightText(labelName, it.label)
      } ?: let {
        if (libReferenceItem.type == PERMISSION && libReferenceItem.libName.startsWith("android.permission")) {
          icon.setImageResource(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android)
        } else {
          icon.setImageResource(R.drawable.ic_question)
        }

        labelName.text = buildSpannedString {
          italic {
            append(context.getString(R.string.not_marked_lib))
          }
          // prevent text clipping
          append(" ")
        }
      }
    }
  }

  override fun onChildClick(helper: BaseViewHolder, view: View, data: BaseNode, position: Int) {
    super.onChildClick(helper, view, data, position)
    if (view.id == android.R.id.icon) {
      val ref = data as? LibReference ?: return
      if (ref.type == NATIVE || ref.type == SERVICE || ref.type == ACTIVITY || ref.type == RECEIVER || ref.type == PROVIDER) {
        val name = ref.libName

        (context as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
          val regexName = LCRules.getRule(name, ref.type, true)?.regexName

          withContext(Dispatchers.Main) {
            (context as BaseActivity<*>).findViewById<View>(androidx.appcompat.R.id.search_src_text)?.clearFocus()
            LibDetailDialogFragment.newInstance(name, ref.type, regexName)
              .show((context as BaseActivity<*>).supportFragmentManager, LibDetailDialogFragment::class.java.name)
          }
        }
      }
    }
  }

  private fun setOrHighlightText(view: TextView, text: CharSequence) {
    if (LibReferenceAdapter.highlightText.isNotBlank()) {
      view.tintHighlightText(LibReferenceAdapter.highlightText, text)
    } else {
      view.text = text
    }
  }
}
