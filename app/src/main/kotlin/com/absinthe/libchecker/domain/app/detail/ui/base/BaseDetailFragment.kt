package com.absinthe.libchecker.domain.app.detail.ui.base

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.domain.app.AppDetailSettingsRepository
import com.absinthe.libchecker.domain.app.AppListSettingsRepository
import com.absinthe.libchecker.domain.app.BuildNativeLibraryItemDisplayDataUseCase
import com.absinthe.libchecker.domain.app.ResolveAppResourceValueUseCase
import com.absinthe.libchecker.domain.app.detail.action.DetailItemDialogRequest
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.ui.DetailFragmentManager
import com.absinthe.libchecker.domain.app.detail.ui.IDetailContainer
import com.absinthe.libchecker.domain.app.detail.ui.Sortable
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.features.applist.detail.ui.LibDetailDialogFragment
import com.absinthe.libchecker.features.applist.detail.ui.PermissionDetailDialogFragment
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringAdapter
import com.absinthe.libchecker.ui.base.BaseFragment
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.app.EmptyListView
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber

/**
 * <pre>
 * author : Absinthe
 * time : 2020/11/27
 * </pre>
 */

const val EXTRA_TYPE = "EXTRA_TYPE"

abstract class BaseDetailFragment<T : ViewBinding> :
  BaseFragment<T>(),
  Sortable {

  protected val viewModel: DetailViewModel by activityViewModel()
  private val appDetailSettingsRepository: AppDetailSettingsRepository by inject()
  private val appListSettingsRepository: AppListSettingsRepository by inject()
  private val buildNativeLibraryItemDisplayData: BuildNativeLibraryItemDisplayDataUseCase by inject()
  private val resolveAppResourceValue: ResolveAppResourceValueUseCase by inject()
  protected val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME).orEmpty() }
  protected val type by lazy { arguments?.getInt(EXTRA_TYPE) ?: NATIVE }
  protected val adapter by lazy {
    LibStringAdapter(
      packageName = packageName,
      type = type,
      itemDisplayOptions = appListSettingsRepository.itemDisplayOptions,
      colorfulRuleIcon = appListSettingsRepository.colorfulRuleIcon,
      fragmentManager = childFragmentManager,
      buildNativeLibraryItemDisplayData = buildNativeLibraryItemDisplayData,
      resolveAppResourceValue = resolveAppResourceValue
    )
  }
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
  private val dividerItemDecoration by lazy {
    DividerItemDecoration(
      requireContext(),
      DividerItemDecoration.VERTICAL
    )
  }
  protected var isListReady = false
  private var afterListReadyTask: Runnable? = null
  private val longClickController by unsafeLazy {
    DetailItemLongClickController(
      fragment = this,
      viewModel = viewModel,
      adapter = adapter,
      coroutineScope = lifecycleScope,
      packageName = { packageName },
      type = { type }
    )
  }

  abstract fun getRecyclerView(): RecyclerView

  protected abstract val needShowLibDetailDialog: Boolean
  protected open val autoLoadItems: Boolean = true

  protected abstract suspend fun getItems(): List<LibStringItemChip>
  protected abstract fun onItemsAvailable(items: List<LibStringItemChip>)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    if (!autoLoadItems) {
      return
    }
    lifecycleScope.launch(Dispatchers.IO) {
      val items = getItems()
      withContext(Dispatchers.Main) {
        onItemsAvailable(items)
      }
    }
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    if (context is IDetailContainer) {
      context.detailFragmentManager.register(type, this)
    }
    if (DetailFragmentManager.navType != DetailFragmentManager.NAV_TYPE_NONE) {
      setupListReadyTask()
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
      setOnItemLongClickListener { _, _, position ->
        longClickController.onLongClick(getItem(position), position)
        true
      }
      setProcessMode(appDetailSettingsRepository.processMode)
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
      viewModel.filterState.updateProcessFilterData(
        viewModel.buildProcessFilterData(
          type = type,
          permissionNotGrantedLabel = getString(R.string.permission_not_granted),
          permissionNotGrantedColor = R.color.material_red_400.getColor(requireContext())
        )
      )
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

    val sortedList = viewModel.sortDetailItems(list, type)

    if (itemChip != null) {
      val newHighlightPosition = sortedList.indexOf(itemChip)
      adapter.setHighlightBackgroundItem(newHighlightPosition)
    }

    withContext(Dispatchers.Main) {
      adapter.setDiffNewData(sortedList.toMutableList())
    }
  }

  protected open fun filterItems(
    items: List<LibStringItemChip>,
    searchWords: String?,
    process: String?
  ): List<LibStringItemChip> {
    return viewModel.filterDetailItems(items, searchWords, process)
  }

  protected open suspend fun getFilterList(
    searchWords: String?,
    process: String?
  ): List<LibStringItemChip>? {
    return filterItems(getItems(), searchWords, process)
  }

  protected suspend fun setItemsWithFilter(
    items: List<LibStringItemChip>,
    searchWords: String?,
    process: String?
  ) {
    adapter.highlightText = searchWords.orEmpty()
    updateItemsWithFilterResult(filterItems(items, searchWords, process))
  }

  override suspend fun setItemsWithFilter(searchWords: String?, process: String?) {
    adapter.highlightText = searchWords.orEmpty()
    updateItemsWithFilterResult(getFilterList(searchWords, process))
  }

  private suspend fun updateItemsWithFilterResult(items: List<LibStringItemChip>?) {
    items?.let {
      val sortedList = viewModel.sortDetailItems(it, type)
      withContext(Dispatchers.Main) {
        if (isDetached || !isBindingInitialized()) return@withContext
        if (sortedList.isEmpty()) {
          if (getRecyclerView().itemDecorationCount > 0) {
            getRecyclerView().removeItemDecoration(dividerItemDecoration)
          }
          emptyView.text.text = getString(R.string.empty_list)
        } else {
          if (getRecyclerView().itemDecorationCount == 0) {
            getRecyclerView().addItemDecoration(dividerItemDecoration)
          }
        }
        adapter.setDiffNewData(sortedList.toMutableList()) {
          afterListReadyTask?.run()
          viewModel.filterState.updateItemsCount(type, sortedList.size)
        }
      }
    }
  }

  fun setProcessMode(processMode: Boolean) {
    if (isComponentFragment()) {
      adapter.setProcessMode(processMode)
    }
  }

  fun setupListReadyTask() {
    if (DetailFragmentManager.navType == type) {
      DetailFragmentManager.navComponent?.let {
        afterListReadyTask = Runnable {
          navigateToComponentImpl(it)
        }
      }
      DetailFragmentManager.resetNavigationParams()
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

    doOnMainThreadIdle {
      (activity as? IDetailContainer)?.collapseAppBar()
      getRecyclerView().scrollToPosition(componentPosition.coerceAtMost(adapter.itemCount - 1))

      // Calculate better offset to provide improved visual experience for highlighting
      val recyclerView = getRecyclerView()
      // Place highlighted item about 1/4 from top for better visibility
      val centerOffset = recyclerView.height / 4

      with(recyclerView.layoutManager) {
        if (this is LinearLayoutManager) {
          scrollToPositionWithOffset(componentPosition, centerOffset)
        } else if (this is StaggeredGridLayoutManager) {
          scrollToPositionWithOffset(componentPosition, centerOffset)
        }
      }
    }

    adapter.setHighlightBackgroundItem(componentPosition)
    //noinspection NotifyDataSetChanged
    adapter.notifyDataSetChanged()
  }

  private fun openLibDetailDialog(position: Int) {
    if (position < 0 || position >= adapter.itemCount) {
      return
    }
    when (val request = viewModel.buildDetailItemDialogRequest(adapter.getItem(position), adapter.type)) {
      is DetailItemDialogRequest.Permission -> {
        PermissionDetailDialogFragment.newInstance(request.permissionName)
          .show(childFragmentManager, PermissionDetailDialogFragment::class.java.name)
      }

      is DetailItemDialogRequest.Library -> {
        LibDetailDialogFragment.newInstance(request.name, request.type, request.regexName, request.isValidLib)
          .show(childFragmentManager, LibDetailDialogFragment::class.java.name)
      }
    }
  }

  fun isComponentFragment(): Boolean {
    return viewModel.isComponentDetailType(type)
  }

  fun hasNonGrantedPermissions(): Boolean {
    return viewModel.hasNonGrantedPermissions(type)
  }
}
