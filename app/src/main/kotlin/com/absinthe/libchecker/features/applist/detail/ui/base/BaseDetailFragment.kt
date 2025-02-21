package com.absinthe.libchecker.features.applist.detail.ui.base

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.TextView
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
import com.absinthe.libchecker.annotation.isComponentType
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.features.applist.DetailFragmentManager
import com.absinthe.libchecker.features.applist.MODE_SORT_BY_LIB
import com.absinthe.libchecker.features.applist.Referable
import com.absinthe.libchecker.features.applist.Sortable
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.applist.detail.IDetailContainer
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.features.applist.detail.ui.LibDetailDialogFragment
import com.absinthe.libchecker.features.applist.detail.ui.PermissionDetailDialogFragment
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringAdapter
import com.absinthe.libchecker.features.applist.detail.ui.impl.ComponentsAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.impl.MetaDataAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.view.EmptyListView
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.integrations.anywhere.AnywhereManager
import com.absinthe.libchecker.integrations.blocker.BlockerManager
import com.absinthe.libchecker.integrations.monkeyking.MonkeyKingManager
import com.absinthe.libchecker.integrations.monkeyking.ShareCmpInfo
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.ui.base.BaseFragment
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.launchLibReferencePage
import com.absinthe.libchecker.utils.extensions.reverseStrikeThroughAnimation
import com.absinthe.libchecker.utils.extensions.startStrikeThroughAnimation
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.rulesbundle.LCRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.core.util.ClipboardUtils
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
  private val dividerItemDecoration by lazy {
    DividerItemDecoration(
      requireContext(),
      DividerItemDecoration.VERTICAL
    )
  }
  protected var isListReady = false
  private var afterListReadyTask: Runnable? = null
  private var integrationMonkeyKingBlockList: List<ShareCmpInfo.Component>? = null
  private var integrationBlockerList: List<ShareCmpInfo.Component>? = null

  abstract fun getRecyclerView(): RecyclerView

  protected abstract val needShowLibDetailDialog: Boolean

  protected abstract suspend fun getItems(): List<LibStringItemChip>
  protected abstract fun onItemsAvailable(items: List<LibStringItemChip>)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
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
        doOnLongClick(context, getItem(position), position)
        true
      }
      setProcessMode(GlobalValues.processMode)
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
      val processMap = if (isComponentFragment()) {
        viewModel.processesMap
      } else if (isNativeSourceAvailable()) {
        viewModel.nativeSourceMap
      } else if (hasNonGrantedPermissions()) {
        val label = requireContext().getString(R.string.permission_not_granted)
        val color = R.color.material_red_400.getColor(requireContext())
        mapOf(label to color)
      } else {
        emptyMap()
      }
      viewModel.updateProcessMap(processMap)
      viewModel.updateProcessToolIconVisibility(processMap.isNotEmpty() && !hasNonGrantedPermissions())
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

    if (GlobalValues.libSortMode == MODE_SORT_BY_LIB) {
      if (type == NATIVE) {
        list.sortByDescending { it.item.size }
      } else {
        list.sortByDescending { it.item.name }
      }
    } else {
      list.sortWith(compareByDescending<LibStringItemChip> { it.rule != null }.thenBy { it.item.name })
    }

    if (itemChip != null) {
      val newHighlightPosition = list.indexOf(itemChip)
      adapter.setHighlightBackgroundItem(newHighlightPosition)
    }

    withContext(Dispatchers.Main) {
      adapter.setDiffNewData(list)
    }
  }

  private fun sortedList(origin: MutableList<LibStringItemChip>): MutableList<LibStringItemChip> {
    if (GlobalValues.libSortMode == MODE_SORT_BY_LIB) {
      if (type == NATIVE) {
        origin.sortByDescending { it.item.size }
      } else {
        origin.sortByDescending { it.item.name }
      }
    } else {
      origin.sortWith(compareByDescending<LibStringItemChip> { it.rule != null }.thenBy { it.item.name })
    }
    return origin
  }

  protected open suspend fun getFilterList(
    searchWords: String?,
    process: String?
  ): List<LibStringItemChip>? {
    return getItems().asSequence()
      .filter { searchWords == null || it.item.name.contains(searchWords, true) || it.item.source?.contains(searchWords, true) == true }
      .filter { process == null || it.item.process == process }
      .toList()
  }

  override suspend fun setItemsWithFilter(searchWords: String?, process: String?) {
    adapter.highlightText = searchWords.orEmpty()
    getFilterList(searchWords, process)?.let {
      val sortedList = sortedList(it.toMutableList())
      lifecycleScope.launch(Dispatchers.Main) {
        if (isDetached || !isBindingInitialized()) return@launch
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
        adapter.setDiffNewData(sortedList) {
          afterListReadyTask?.run()
          viewModel.updateItemsCountStateFlow(type, sortedList.size)
        }
      }
    }
  }

  fun switchProcessMode() {
    if (isComponentFragment() || isNativeSourceAvailable()) {
      adapter.switchProcessMode()
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
    (activity as? IDetailContainer)?.collapseAppBar()
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
    if (position < 0 || position >= adapter.itemCount) {
      return
    }
    val item = adapter.getItem(position)
    val name = item.item.name
    val isValidLib = item.rule != null

    if (adapter.type == PERMISSION) {
      PermissionDetailDialogFragment.newInstance(name)
        .show(childFragmentManager, PermissionDetailDialogFragment::class.java.name)
      return
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
    return isComponentType(type)
  }

  fun isNativeSourceAvailable(): Boolean {
    return type == NATIVE && viewModel.nativeSourceMap.isNotEmpty()
  }

  fun hasNonGrantedPermissions(): Boolean {
    return type == PERMISSION && viewModel.permissionsItems.value?.any { it.item.size == 0L } == true
  }

  private fun doOnLongClick(context: Context, item: LibStringItemChip, position: Int) {
    val actionMap = mutableMapOf<Int, () -> Unit>()
    val arrayAdapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_1)
    val componentName = item.item.name
    val fullComponentName = if (componentName.startsWith(".")) {
      viewModel.packageInfo.packageName + componentName
    } else {
      componentName
    }

    // Copy
    arrayAdapter.add(getString(android.R.string.copy))
    actionMap[arrayAdapter.count - 1] = {
      if (this is MetaDataAnalysisFragment) {
        ClipboardUtils.put(context, componentName + ": " + item.item.source)
      } else {
        ClipboardUtils.put(context, componentName)
      }
      VersionCompat.showCopiedOnClipboardToast(context)
    }

    // Reference
    if (this is Referable) {
      arrayAdapter.add(getString(R.string.tab_lib_reference_statistics))
      actionMap[arrayAdapter.count - 1] = {
        activity?.launchLibReferencePage(componentName, item.rule?.label, type, null)
      }
    }

    if (!viewModel.isApk) {
      // Blocker
      if (this is ComponentsAnalysisFragment && BlockerManager.isSupportInteraction) {
        if (integrationBlockerList == null) {
          integrationBlockerList =
            BlockerManager().queryBlockedComponent(context, viewModel.packageInfo.packageName)
        }
        val blockerShouldBlock =
          integrationBlockerList?.any { it.name == fullComponentName } == false
        val blockStr = if (blockerShouldBlock) {
          R.string.integration_blocker_menu_block
        } else {
          R.string.integration_blocker_menu_unblock
        }
        arrayAdapter.add(getString(blockStr))
        actionMap[arrayAdapter.count - 1] = {
          BlockerManager().apply {
            addBlockedComponent(
              context,
              viewModel.packageInfo.packageName,
              componentName,
              type,
              blockerShouldBlock
            )
            integrationBlockerList =
              queryBlockedComponent(context, viewModel.packageInfo.packageName)
            val shouldTurnToDisable =
              integrationBlockerList?.any { it.name == fullComponentName } == true && blockerShouldBlock
            animateTvTitle(position, shouldTurnToDisable)
          }
        }
      }

      // MonkeyKing Purify
      if (this is ComponentsAnalysisFragment && MonkeyKingManager.isSupportInteraction) {
        if (integrationMonkeyKingBlockList == null) {
          integrationMonkeyKingBlockList =
            MonkeyKingManager().queryBlockedComponent(context, viewModel.packageInfo.packageName)
        }
        val monkeyKingShouldBlock =
          integrationMonkeyKingBlockList?.any { it.name == componentName } == false
        if (monkeyKingShouldBlock) {
          arrayAdapter.add(getString(R.string.integration_monkey_king_menu_block))
        } else {
          arrayAdapter.add(getString(R.string.integration_monkey_king_menu_unblock))
        }
        actionMap[arrayAdapter.count - 1] = {
          MonkeyKingManager().apply {
            addBlockedComponent(
              context,
              viewModel.packageInfo.packageName,
              componentName,
              type,
              monkeyKingShouldBlock
            )
            integrationMonkeyKingBlockList =
              queryBlockedComponent(context, viewModel.packageInfo.packageName)
            val shouldTurnToDisable =
              integrationMonkeyKingBlockList?.any { it.name == fullComponentName } == true && monkeyKingShouldBlock
            animateTvTitle(position, shouldTurnToDisable)
          }
        }
      }

      // Anywhere-
      if (type == ACTIVITY && AnywhereManager.isSupportInteraction) {
        arrayAdapter.add(getString(R.string.integration_anywhere_menu_editor))
        actionMap[arrayAdapter.count - 1] = {
          AnywhereManager().launchActivityEditor(
            context,
            viewModel.packageInfo.packageName,
            componentName
          )
        }
      }
    }

    BaseAlertDialogBuilder(context)
      .setAdapter(arrayAdapter) { _, which ->
        actionMap[which]?.invoke()
      }
      .show()
  }

  private fun animateTvTitle(position: Int, shouldTurnToDisable: Boolean) {
    (adapter.getViewByPosition(position, android.R.id.title) as? TextView)?.run {
      if (shouldTurnToDisable) startStrikeThroughAnimation() else reverseStrikeThroughAnimation()
    }
  }
}
