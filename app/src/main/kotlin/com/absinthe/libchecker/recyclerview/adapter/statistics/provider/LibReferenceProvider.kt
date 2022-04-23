package com.absinthe.libchecker.recyclerview.adapter.statistics.provider

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.bean.LibReference
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.recyclerview.adapter.statistics.LibReferenceAdapter
import com.absinthe.libchecker.ui.fragment.detail.LibDetailDialogFragment
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.tintHighlightText
import com.absinthe.libchecker.utils.extensions.valueUnsafe
import com.absinthe.libchecker.view.statistics.LibReferenceItemView
import com.absinthe.rulesbundle.LCRules
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val LIB_REFERENCE_PROVIDER = 0

class LibReferenceProvider(val lifecycleScope: LifecycleCoroutineScope) : BaseNodeProvider() {

  override val itemViewType: Int = LIB_REFERENCE_PROVIDER
  override val layoutId: Int = 0

  init {
    addChildClickViewIds(android.R.id.icon)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      LibReferenceItemView(ContextThemeWrapper(context, R.style.AppListMaterialCard)).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
          val margin = context.getDimensionPixelSize(R.dimen.main_card_margin)
          it.setMargins(margin, margin, margin, margin)
        }
      }
    )
  }

  override fun convert(helper: BaseViewHolder, item: BaseNode) {
    (helper.itemView as LibReferenceItemView).container.apply {
      val libReferenceItem = item as LibReference
      count.text = libReferenceItem.referredList.size.toString()

      setOrHighlightText(libName, libReferenceItem.libName)

      libReferenceItem.chip?.let {
        icon.apply {
          setImageResource(it.iconRes)

          if (!GlobalValues.isColorfulIcon.valueUnsafe) {
            this.drawable.mutate().colorFilter =
              ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
          }
        }

        setOrHighlightText(labelName, it.name)
      } ?: let {
        if (libReferenceItem.type == PERMISSION && libReferenceItem.libName.startsWith("android.permission")) {
          icon.setImageResource(R.drawable.ic_lib_android)
        } else {
          icon.setImageResource(R.drawable.ic_question)
        }
        val spannableString = SpannableString(context.getString(R.string.not_marked_lib))
        val colorSpanit = StyleSpan(Typeface.ITALIC)
        spannableString.setSpan(
          colorSpanit,
          0,
          spannableString.length,
          Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        labelName.text = spannableString
      }
    }
  }

  override fun onChildClick(helper: BaseViewHolder, view: View, data: BaseNode, position: Int) {
    super.onChildClick(helper, view, data, position)
    if (view.id == android.R.id.icon) {
      val ref = data as? LibReference ?: return
      if (ref.type == NATIVE || ref.type == SERVICE || ref.type == ACTIVITY || ref.type == RECEIVER || ref.type == PROVIDER) {
        val name = ref.libName

        lifecycleScope.launch(Dispatchers.IO) {
          val regexName = LCRules.getRule(name, ref.type, true)?.regexName

          withContext(Dispatchers.Main) {
            LibDetailDialogFragment.newInstance(name, ref.type, regexName)
              .show((context as BaseActivity<*>).supportFragmentManager, "LibDetailDialogFragment")
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
