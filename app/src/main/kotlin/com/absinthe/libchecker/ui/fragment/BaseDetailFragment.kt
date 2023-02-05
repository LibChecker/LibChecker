package com.absinthe.libchecker.ui.fragment

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.base.BaseFragment
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.recyclerview.adapter.detail.LibStringAdapter
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.detail.IDetailContainer
import com.absinthe.libchecker.ui.fragment.detail.DetailFragmentManager
import com.absinthe.libchecker.ui.fragment.detail.LibDetailDialogFragment
import com.absinthe.libchecker.ui.fragment.detail.LocatedCount
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_LIB
import com.absinthe.libchecker.ui.fragment.detail.Sortable
import com.absinthe.libchecker.ui.fragment.detail.impl.ComponentsAnalysisFragment
import com.absinthe.libchecker.utils.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.detail.EmptyListView
import com.absinthe.libchecker.viewmodel.DetailViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.rulesbundle.LCRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * <pre>
 * author : Absinthe
 * time : 2020/11/27
 * </pre>
 */

const val EXTRA_TYPE = "EXTRA_TYPE"

abstract class BaseDetailFragment<T : ViewBinding> : BaseFragment<T>(), Sortable {

  protected val viewModel: DetailViewModel by activityViewModels()
  protected val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME).orEmpty() }
  protected val type by lazy { arguments?.getInt(EXTRA_TYPE) ?: NATIVE }
  protected val adapter by lazy { LibStringAdapter(packageName, type, childFragmentManager) }
  protected val emptyView by unsafeLazy {
    EmptyListView(requireContext()).apply {
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
      ).also {
        it.gravity = Gravity.CENTER_HORIZONTAL
      }
      addPaddingTop(96.dp)
      text.text = getString(R.string.loading)
    }
  }
  protected val dividerItemDecoration by lazy {
    DividerItemDecoration(
      requireContext(),
      DividerItemDecoration.VERTICAL
    )
  }
  protected var isListReady = false
  protected var afterListReadyTask: Runnable? = null

  abstract fun getRecyclerView(): RecyclerView
  abstract fun getFilterListByText(text: String): List<LibStringItemChip>?

  protected abstract val needShowLibDetailDialog: Boolean

  override fun onAttach(context: Context) {
    super.onAttach(context)
    if (context is IDetailContainer) {
      context.detailFragmentManager.register(type, this)
    }
    if (DetailFragmentManager.navType == type) {
      DetailFragmentManager.navComponent?.let {
        afterListReadyTask = Runnable {
          navigateToComponentImpl(it)
        }
      }
      DetailFragmentManager.resetNavigationParams()
    } else {
      afterListReadyTask = Runnable {
        lifecycleScope.launch(Dispatchers.IO) {
          viewModel.queriedText?.let {
            if (it.isNotEmpty()) {
              filterList(it)
            }
          }
          if (this@BaseDetailFragment is BaseFilterAnalysisFragment) {
            viewModel.queriedProcess?.let {
              if (it.isNotEmpty()) {
                filterItems(it)
              }
            }
          }
        }
      }
    }
    adapter.apply {
      if (needShowLibDetailDialog) {
        setOnItemClickListener { _, view, position ->
          if (AntiShakeUtils.isInvalidClick(view)) {
            return@setOnItemClickListener
          }
          openLibDetailDialog(position)
        }
      }
      setProcessMode(viewModel.processMode)
    }
  }

  override fun onDetach() {
    super.onDetach()
    activity?.let {
      if (it is IDetailContainer) {
        (it as IDetailContainer).detailFragmentManager.unregister(type)
      }
    }
  }

  override fun onVisibilityChanged(visible: Boolean) {
    super.onVisibilityChanged(visible)
    if (visible) {
      if (this is ComponentsAnalysisFragment && viewModel.processesMap.isNotEmpty()) {
        viewModel.processToolIconVisibilityLiveData.postValue(isComponentFragment())
      } else {
        viewModel.processToolIconVisibilityLiveData.postValue(false)
      }
    }
  }

  override suspend fun sort() {
    val list = mutableListOf<LibStringItemChip>().also {
      it += adapter.data
    }
    val itemChip =
      if (adapter.highlightPosition != -1 && adapter.highlightPosition < adapter.data.size) {
        adapter.data[adapter.highlightPosition]
      } else {
        null
      }

    if (viewModel.sortMode == MODE_SORT_BY_LIB) {
      if (type == NATIVE) {
        list.sortByDescending { it.item.size }
      } else {
        list.sortByDescending { it.item.name }
      }
    } else {
      list.sortWith(compareByDescending<LibStringItemChip> { it.chip != null }.thenBy { it.item.name })
    }

    if (itemChip != null) {
      val newHighlightPosition = list.indexOf(itemChip)
      adapter.setHighlightBackgroundItem(newHighlightPosition)
    }

    withContext(Dispatchers.Main) {
      adapter.setDiffNewData(list)
    }
  }

  override fun filterList(text: String) {
    adapter.highlightText = text
    getFilterListByText(text)?.let {
      lifecycleScope.launch(Dispatchers.Main) {
        if (it.isEmpty()) {
          if (getRecyclerView().itemDecorationCount > 0) {
            getRecyclerView().removeItemDecoration(dividerItemDecoration)
          }
          emptyView.text.text = getString(R.string.empty_list)
        }
        adapter.setDiffNewData(it.toMutableList()) {
          viewModel.itemsCountLiveData.value = LocatedCount(locate = type, count = it.size)
          viewModel.itemsCountList[type] = it.size
          doOnMainThreadIdle {
            //noinspection NotifyDataSetChanged
            adapter.notifyDataSetChanged()
          }
        }
      }
    }
  }

  fun getItemsCount() = adapter.itemCount

  fun switchProcessMode() {
    if (isComponentFragment()) {
      adapter.switchProcessMode()
    }
  }

  private fun navigateToComponentImpl(component: String) {
    var componentPosition = adapter.data.indexOfFirst { it.item.name == component }
    if (componentPosition == -1) {
      return
    }
    if (adapter.hasHeaderLayout()) {
      componentPosition++
    }

    Timber.d("navigateToComponent: componentPosition = $componentPosition")
    getRecyclerView().scrollToPosition(componentPosition.coerceAtMost(adapter.itemCount - 1))

    with(getRecyclerView().layoutManager) {
      if (this is LinearLayoutManager) {
        scrollToPositionWithOffset(componentPosition, 0)
      } else if (this is StaggeredGridLayoutManager) {
        scrollToPositionWithOffset(componentPosition, 0)
      }
    }

    adapter.setHighlightBackgroundItem(componentPosition)
    //noinspection NotifyDataSetChanged
    adapter.notifyDataSetChanged()
  }

  private fun openLibDetailDialog(position: Int) {
    val item = adapter.getItem(position)
    val name = item.item.name
    val isValidLib = if (adapter.type == NATIVE) {
      item.chip != null
    } else {
      true
    }

    lifecycleScope.launch(Dispatchers.IO) {
      val regexName = LCRules.getRule(name, adapter.type, true)?.regexName

      withContext(Dispatchers.Main) {
        LibDetailDialogFragment.newInstance(name, adapter.type, regexName, isValidLib)
          .show(childFragmentManager, LibDetailDialogFragment::class.java.name)
      }
    }
  }

  fun isComponentFragment(): Boolean {
    return type == ACTIVITY || type == SERVICE || type == RECEIVER || type == PROVIDER
  }

  protected fun hasNonGrantedPermissions(): Boolean {
    return type == PERMISSION && (viewModel.permissionsItems.value?.any { it.item.size == 0L } == true)
  }
}
