package com.absinthe.libchecker.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

internal class BindOnlyAdapter<T : Any, V : View>(
  private val viewFactory: (Context) -> V,
  private val bindView: V.(T) -> Unit
) : BaseQuickAdapter<T, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(viewFactory(context))
  }

  @Suppress("UNCHECKED_CAST")
  override fun convert(holder: BaseViewHolder, item: T) {
    (holder.itemView as V).bindView(item)
  }
}
