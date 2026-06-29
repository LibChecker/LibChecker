package com.chad.library.adapter.base

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.entity.node.BaseExpandNode
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chad.library.adapter4.BaseNodeAdapter as BaseNodeAdapter4

abstract class BaseNodeAdapter :
  BaseNodeAdapter4(),
  HeaderFooterSupport {

  val data: List<BaseNode>
    get() = items.filterIsInstance<BaseNode>()

  @Suppress("MemberVisibilityCanBePrivate")
  var headerWithEmptyEnable: Boolean = false

  private val providers = linkedMapOf<Int, BaseNodeProvider>()
  private val headerAdapters = mutableListOf<SingleViewAdapter>()
  private val footerAdapters = mutableListOf<SingleViewAdapter>()
  private var concatAdapter: ConcatAdapter? = null
  private var syncingHeaderFooter = false
  private var diffCallback: DiffUtil.ItemCallback<BaseNode>? = null

  val recyclerViewOrNull: RecyclerView?
    get() = runCatching { recyclerView }.getOrNull()

  override val legacyRecyclerViewOrNull: RecyclerView?
    get() = recyclerViewOrNull

  override fun legacyItemCount(): Int = headerAdapters.size + itemCount + footerAdapters.size

  abstract fun getItemType(data: List<BaseNode>, position: Int): Int

  fun addNodeProvider(provider: BaseNodeProvider) {
    providers[provider.itemViewType] = provider
  }

  fun setList(list: Collection<BaseNode>?) {
    submitList(list?.toList().orEmpty(), clearOpenStates = true)
  }

  fun setDiffNewData(list: Collection<BaseNode>?, commitCallback: Runnable? = null) {
    val callback = diffCallback
    val newList = list?.toList().orEmpty()
    val oldList = data.toList()
    val hasTreeNodes =
      oldList.any { it.childNode?.isNotEmpty() == true } ||
        newList.any { it.childNode?.isNotEmpty() == true }
    if (callback == null || hasTreeNodes || displayEmptyView() || displayEmptyView(newList)) {
      submitList(newList, clearOpenStates = false, commitCallback)
      return
    }

    val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
      override fun getOldListSize(): Int = oldList.size

      override fun getNewListSize(): Int = newList.size

      override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return callback.areItemsTheSame(oldList[oldItemPosition], newList[newItemPosition])
      }

      override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return callback.areContentsTheSame(oldList[oldItemPosition], newList[newItemPosition])
      }

      override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return callback.getChangePayload(oldList[oldItemPosition], newList[newItemPosition])
      }
    })
    items = newList
    diffResult.dispatchUpdatesTo(this)
    commitCallback?.run()
  }

  fun setDiffCallback(diffCallback: DiffUtil.ItemCallback<BaseNode>) {
    this.diffCallback = diffCallback
  }

  fun expandOrCollapse(position: Int): Boolean {
    val node = data.getOrNull(position) as? BaseExpandNode ?: return openOrClose(position)
    return if (isOpenedAt(position)) {
      node.isExpanded = false
      close(position)
    } else {
      node.isExpanded = true
      open(position)
    }
  }

  fun addHeaderView(view: View) {
    val adapter = SingleViewAdapter(view)
    val index = headerAdapters.size
    headerAdapters += adapter
    ensureHeaderFooterAdapter()?.let { concat ->
      if (!concat.adapters.contains(adapter)) {
        concat.addAdapter(index, adapter)
      }
    }
  }

  fun setHeaderView(view: View) {
    removeAllHeaderView()
    addHeaderView(view)
  }

  fun removeAllHeaderView() {
    concatAdapter?.let { concat ->
      headerAdapters.forEach { concat.removeAdapter(it) }
    }
    headerAdapters.clear()
  }

  override fun setFooterView(view: View) {
    removeAllFooterView()
    val adapter = SingleViewAdapter(view)
    footerAdapters += adapter
    ensureHeaderFooterAdapter()?.let { concat ->
      if (!concat.adapters.contains(adapter)) {
        concat.addAdapter(adapter)
      }
    }
  }

  override fun removeAllFooterView() {
    concatAdapter?.let { concat ->
      footerAdapters.forEach { concat.removeAdapter(it) }
    }
    footerAdapters.clear()
  }

  override fun hasFooterLayout(): Boolean = footerAdapters.isNotEmpty()

  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)
    if (!syncingHeaderFooter) {
      ensureHeaderFooterAdapter()
    }
  }

  override fun getChildNodeList(position: Int, parent: Any): List<Any>? {
    return (parent as? BaseNode)?.childNode
  }

  override fun isInitialOpen(position: Int, item: Any): Boolean {
    return (item as? BaseExpandNode)?.isExpanded == true
  }

  override fun isSameNode(item1: Any, item2: Any): Boolean {
    return item1 === item2 || item1 == item2
  }

  override fun getItemViewType(position: Int, list: List<Any>): Int {
    @Suppress("UNCHECKED_CAST")
    return getItemType(list as List<BaseNode>, position)
  }

  override fun onCreateViewHolder(
    context: Context,
    parent: ViewGroup,
    viewType: Int
  ): RecyclerView.ViewHolder {
    val provider = providers[viewType]
      ?: throw IllegalArgumentException("Node provider for viewType $viewType is not registered.")
    provider.attach(this, context)
    return provider.onCreateViewHolder(parent, viewType)
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, item: Any?) {
    val node = item as? BaseNode ?: return
    val viewHolder = holder as BaseViewHolder
    val provider = providers[getItemViewType(position)]
      ?: throw IllegalArgumentException("Node provider for position $position is not registered.")
    provider.convert(viewHolder, node)
    provider.bindClickListeners(viewHolder, node, position)
  }

  private fun ensureHeaderFooterAdapter(): ConcatAdapter? {
    if (headerAdapters.isEmpty() && footerAdapters.isEmpty()) return concatAdapter
    val recyclerView = recyclerViewOrNull ?: return null
    concatAdapter?.let { concat ->
      if (recyclerView.adapter !== concat) {
        recyclerView.adapter = concat
      }
      return concat
    }

    val concat = ConcatAdapter().also { concatAdapter = it }
    syncingHeaderFooter = true
    try {
      headerAdapters.forEach { concat.addAdapter(it) }
      concat.addAdapter(this)
      footerAdapters.forEach { concat.addAdapter(it) }

      if (recyclerView.adapter !== concat) {
        recyclerView.adapter = concat
      }
    } finally {
      syncingHeaderFooter = false
    }
    return concat
  }

  private class SingleViewAdapter(
    private val view: View
  ) : RecyclerView.Adapter<BaseViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
      (view.parent as? ViewGroup)?.removeView(view)
      return BaseViewHolder(view)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) = Unit

    override fun getItemCount(): Int = 1
  }
}
