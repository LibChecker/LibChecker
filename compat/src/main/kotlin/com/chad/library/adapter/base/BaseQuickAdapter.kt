package com.chad.library.adapter.base

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chad.library.adapter4.BaseQuickAdapter as BaseQuickAdapter4
import java.util.concurrent.atomic.AtomicLong

abstract class BaseQuickAdapter<T : Any, VH : RecyclerView.ViewHolder>(
  @LayoutRes private val layoutResId: Int = 0,
  diffCallback: DiffUtil.ItemCallback<T>? = null
) : BaseQuickAdapter4<T, VH>(diffCallback?.let { AsyncDifferConfig.Builder(it).build() }),
  HeaderFooterSupport {

  val data: MutableList<T> = AdapterDataList(this)

  @Suppress("MemberVisibilityCanBePrivate")
  var headerWithEmptyEnable: Boolean = false

  private val childClickViewIds = linkedSetOf<Int>()
  private val childLongClickViewIds = linkedSetOf<Int>()
  private var legacyChildClickListener: ((BaseQuickAdapter<T, VH>, View, Int) -> Unit)? = null
  private var legacyChildLongClickListener: ((BaseQuickAdapter<T, VH>, View, Int) -> Boolean)? = null

  private val headerAdapters = mutableListOf<SingleViewAdapter>()
  private val footerAdapters = mutableListOf<SingleViewAdapter>()
  private var concatAdapter: ConcatAdapter? = null
  private var syncingHeaderFooter = false

  val recyclerViewOrNull: RecyclerView?
    get() = runCatching { recyclerView }.getOrNull()

  override val legacyRecyclerViewOrNull: RecyclerView?
    get() = recyclerViewOrNull

  override fun legacyItemCount(): Int = headerAdapters.size + itemCount + footerAdapters.size

  protected open fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): VH {
    check(layoutResId != 0) {
      "Please override onCreateDefViewHolder() or provide a layout id."
    }
    @Suppress("UNCHECKED_CAST")
    return BaseViewHolder(layoutResId, parent) as VH
  }

  final override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): VH {
    return onCreateDefViewHolder(parent, viewType)
  }

  protected abstract fun convert(holder: VH, item: T)

  protected open fun convert(holder: VH, item: T, payloads: List<Any>) {
    convert(holder, item)
  }

  final override fun onBindViewHolder(holder: VH, position: Int, item: T?) {
    item ?: return
    convert(holder, item)
  }

  final override fun onBindViewHolder(holder: VH, position: Int, item: T?, payloads: List<Any>) {
    item ?: return
    if (payloads.isEmpty()) {
      convert(holder, item)
    } else {
      convert(holder, item, payloads)
    }
  }

  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)
    if (!syncingHeaderFooter) {
      ensureHeaderFooterAdapter()
    }
  }

  fun createBaseViewHolder(view: View): BaseViewHolder = BaseViewHolder(view)

  fun setList(list: Collection<T>?) {
    submitList(list?.toList().orEmpty())
  }

  fun setNewInstance(list: MutableList<T>?) {
    setList(list)
  }

  fun setData(position: Int, item: T) {
    set(position, item)
  }

  fun addData(item: T) {
    add(item)
  }

  fun addData(position: Int, item: T) {
    add(position, item)
  }

  fun addData(collection: Collection<T>) {
    addAll(collection)
  }

  fun addData(position: Int, collection: Collection<T>) {
    addAll(position, collection)
  }

  fun setDiffNewData(list: Collection<T>?, commitCallback: Runnable? = null) {
    submitList(list?.toList().orEmpty(), commitCallback)
  }

  fun setOnItemClickListener(
    listener: com.chad.library.adapter.base.listener.OnItemClickListener?
  ) = apply {
    super.setOnItemClickListener { _, view, position ->
      listener?.onItemClick(this, view, position)
    }
  }

  fun addChildClickViewIds(@IdRes vararg ids: Int) {
    ids.forEach { id ->
      if (childClickViewIds.add(id)) {
        legacyChildClickListener?.let { listener ->
          addOnItemChildClickListener(id) { _, view, position -> listener(this, view, position) }
        }
      }
    }
  }

  fun addChildLongClickViewIds(@IdRes vararg ids: Int) {
    ids.forEach { id ->
      if (childLongClickViewIds.add(id)) {
        legacyChildLongClickListener?.let { listener ->
          addOnItemChildLongClickListener(id) { _, view, position -> listener(this, view, position) }
        }
      }
    }
  }

  fun setOnItemChildClickListener(
    listener: ((BaseQuickAdapter<T, VH>, View, Int) -> Unit)?
  ) = apply {
    legacyChildClickListener = listener
    childClickViewIds.forEach { id ->
      removeOnItemChildClickListener(id)
      listener?.let {
        addOnItemChildClickListener(id) { _, view, position -> it(this, view, position) }
      }
    }
  }

  fun setOnItemChildLongClickListener(
    listener: ((BaseQuickAdapter<T, VH>, View, Int) -> Boolean)?
  ) = apply {
    legacyChildLongClickListener = listener
    childLongClickViewIds.forEach { id ->
      removeOnItemChildLongClickListener(id)
      listener?.let {
        addOnItemChildLongClickListener(id) { _, view, position -> it(this, view, position) }
      }
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

  fun hasHeaderLayout(): Boolean = headerAdapters.isNotEmpty()

  fun getViewByPosition(position: Int, viewId: Int): View? {
    val recyclerView = recyclerViewOrNull ?: return null
    val adapterPosition = position + headerAdapters.size
    return recyclerView.findViewHolderForAdapterPosition(adapterPosition)
      ?.itemView
      ?.findViewById(viewId)
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

  private fun ensureHeaderFooterAdapter(): ConcatAdapter? {
    if (headerAdapters.isEmpty() && footerAdapters.isEmpty()) return concatAdapter
    val recyclerView = recyclerViewOrNull ?: return null
    concatAdapter?.let { concat ->
      if (recyclerView.adapter !== concat) {
        recyclerView.adapter = concat
      }
      return concat
    }

    val concat = createHeaderFooterConcatAdapter().also { concatAdapter = it }
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

  private class AdapterDataList<T : Any>(
    private val adapter: BaseQuickAdapter<T, *>
  ) : AbstractMutableList<T>() {
    override val size: Int
      get() = adapter.items.size

    override fun get(index: Int): T = adapter.items[index]

    override fun add(index: Int, element: T) {
      adapter.add(index, element)
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
      if (elements.isEmpty()) return false
      adapter.addAll(index, elements)
      return true
    }

    override fun set(index: Int, element: T): T {
      val previous = get(index)
      adapter.set(index, element)
      return previous
    }

    override fun removeAt(index: Int): T {
      val previous = get(index)
      adapter.removeAt(index)
      return previous
    }

    override fun clear() {
      adapter.submitList(emptyList())
    }
  }

  private class SingleViewAdapter(
    private val view: View
  ) : RecyclerView.Adapter<BaseViewHolder>() {
    private val stableItemId = nextStableItemId.getAndDecrement()

    init {
      setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
      (view.parent as? ViewGroup)?.removeView(view)
      return BaseViewHolder(view)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) = Unit

    override fun getItemCount(): Int = 1

    override fun getItemId(position: Int): Long = stableItemId

    companion object {
      private val nextStableItemId = AtomicLong(Long.MAX_VALUE)
    }
  }

  private fun createHeaderFooterConcatAdapter(): ConcatAdapter {
    if (!hasStableIds()) {
      return ConcatAdapter()
    }
    val config = ConcatAdapter.Config.Builder()
      .setStableIdMode(ConcatAdapter.Config.StableIdMode.SHARED_STABLE_IDS)
      .build()
    return ConcatAdapter(config)
  }
}
