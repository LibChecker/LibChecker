package com.chad.library.adapter.base.provider

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.chad.library.adapter.base.BaseNodeAdapter
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.viewholder.BaseViewHolder

abstract class BaseNodeProvider {
  abstract val itemViewType: Int
  abstract val layoutId: Int

  private val childClickViewIds = linkedSetOf<Int>()
  private var adapter: BaseNodeAdapter? = null

  lateinit var context: Context
    private set

  fun attach(adapter: BaseNodeAdapter, context: Context) {
    this.adapter = adapter
    this.context = context
  }

  fun getAdapter(): BaseNodeAdapter? = adapter

  fun addChildClickViewIds(vararg ids: Int) {
    childClickViewIds += ids.toList()
  }

  internal fun bindClickListeners(holder: BaseViewHolder, data: BaseNode, position: Int) {
    childClickViewIds.forEach { id ->
      holder.itemView.findViewById<View>(id)?.setOnClickListener { view ->
        onChildClick(
          holder,
          view,
          data,
          holder.bindingAdapterPosition.takeIf { it >= 0 } ?: position
        )
      }
    }
  }

  open fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    throw NotImplementedError("Override onCreateViewHolder() when layoutId is 0.")
  }

  abstract fun convert(helper: BaseViewHolder, item: BaseNode)

  open fun onClick(helper: BaseViewHolder, view: View, data: BaseNode, position: Int) = Unit

  open fun onChildClick(helper: BaseViewHolder, view: View, data: BaseNode, position: Int) = Unit
}
