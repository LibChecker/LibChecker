package com.absinthe.libchecker.domain.app.detail.ui.base

import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.domain.app.detail.action.DetailItemLongClickActions
import com.absinthe.libchecker.domain.app.detail.action.GetPermissionProvidersUseCase
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.NativeLibraryExtractionResult
import com.absinthe.libchecker.domain.app.detail.ui.Referable
import com.absinthe.libchecker.domain.app.detail.ui.adapter.LibStringAdapter
import com.absinthe.libchecker.domain.app.detail.ui.dialog.ELFDetailDialogFragment
import com.absinthe.libchecker.domain.app.detail.ui.impl.ComponentsAnalysisFragment
import com.absinthe.libchecker.integrations.anywhere.AnywhereManager
import com.absinthe.libchecker.integrations.blocker.BlockerManager
import com.absinthe.libchecker.integrations.monkeyking.MonkeyKingManager
import com.absinthe.libchecker.integrations.monkeyking.ShareCmpInfo
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.launchLibReferencePage
import com.absinthe.libchecker.utils.extensions.reverseStrikeThroughAnimation
import com.absinthe.libchecker.utils.extensions.startStrikeThroughAnimation
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import rikka.core.util.ClipboardUtils
import timber.log.Timber

class DetailItemLongClickController(
  private val fragment: BaseDetailFragment<*>,
  private val viewModel: DetailViewModel,
  private val adapter: LibStringAdapter,
  private val coroutineScope: CoroutineScope,
  private val packageName: () -> String,
  private val type: () -> Int,
  private val getPermissionProvidersUseCase: GetPermissionProvidersUseCase
) {
  private var integrationMonkeyKingBlockList: List<ShareCmpInfo.Component>? = null
  private var integrationBlockerList: List<ShareCmpInfo.Component>? = null
  private var pendingExtractionItem: LibStringItem? = null
  private var loadingDialog: AlertDialog? = null

  init {
    collectNativeLibraryExtractionResults()
  }

  fun onLongClick(item: LibStringItemChip, position: Int) {
    val context = fragment.requireContext()
    val packageName = packageName()
    val actionMap = mutableMapOf<Int, () -> Unit>()
    val arrayAdapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_1)
    val actions = viewModel.buildDetailItemLongClickActions(
      item = item,
      packageName = packageName,
      detailType = type(),
      canReference = fragment is Referable
    )

    addCopyAction(arrayAdapter, actionMap, actions)
    addElfActions(arrayAdapter, actionMap, item, actions)
    addReferenceAction(arrayAdapter, actionMap, actions)
    addProviderPermissionAction(arrayAdapter, actionMap, actions)
    addIntegrationActions(arrayAdapter, actionMap, actions, position)

    BaseAlertDialogBuilder(context)
      .setAdapter(arrayAdapter) { _, which ->
        actionMap[which]?.invoke()
      }
      .show()
  }

  private fun addCopyAction(
    arrayAdapter: ArrayAdapter<String>,
    actionMap: MutableMap<Int, () -> Unit>,
    actions: DetailItemLongClickActions
  ) {
    val context = fragment.requireContext()
    arrayAdapter.add(fragment.getString(android.R.string.copy))
    actionMap[arrayAdapter.count - 1] = {
      ClipboardUtils.put(context, actions.copyText)
      VersionCompat.showCopiedOnClipboardToast(context)
    }
  }

  private fun addElfActions(
    arrayAdapter: ArrayAdapter<String>,
    actionMap: MutableMap<Int, () -> Unit>,
    item: LibStringItemChip,
    actions: DetailItemLongClickActions
  ) {
    if (!actions.elfExtractAvailable) {
      return
    }

    arrayAdapter.add(fragment.getString(com.absinthe.libchecker.R.string.lib_detail_elf_extract))
    actionMap[arrayAdapter.count - 1] = {
      pendingExtractionItem = item.item
      showLoading()
      viewModel.extractNativeLibrary(item.item)
    }

    actions.elfInfo?.let { elfInfo ->
      arrayAdapter.add(fragment.getString(com.absinthe.libchecker.R.string.lib_detail_elf_info))
      actionMap[arrayAdapter.count - 1] = {
        ELFDetailDialogFragment.newInstance(
          packageName = elfInfo.packageName,
          elfPath = elfInfo.elfPath,
          ruleIcon = elfInfo.ruleIcon
        ).show(fragment.childFragmentManager, ELFDetailDialogFragment::class.java.name)
      }
    }
  }

  private fun addReferenceAction(
    arrayAdapter: ArrayAdapter<String>,
    actionMap: MutableMap<Int, () -> Unit>,
    actions: DetailItemLongClickActions
  ) {
    val reference = actions.reference ?: return

    arrayAdapter.add(fragment.getString(com.absinthe.libchecker.R.string.tab_lib_reference_statistics))
    actionMap[arrayAdapter.count - 1] = {
      fragment.activity?.launchLibReferencePage(reference.refName, reference.label, reference.type, null)
    }
  }

  private fun addProviderPermissionAction(
    arrayAdapter: ArrayAdapter<String>,
    actionMap: MutableMap<Int, () -> Unit>,
    actions: DetailItemLongClickActions
  ) {
    if (!actions.providerPermissionAvailable) {
      return
    }

    val context = fragment.requireContext()
    arrayAdapter.add(fragment.getString(com.absinthe.libchecker.R.string.lib_detail_permission_providers))
    actionMap[arrayAdapter.count - 1] = {
      val loading = UiUtils.createLoadingDialog(fragment.requireActivity())
      loading.show()
      coroutineScope.launch {
        val items = getPermissionProvidersUseCase(actions.componentName)
        loading.dismiss()
        if (items.isNotEmpty()) {
          val encodedList = items.map { "${it.packageName}|${it.providerName}" }.toTypedArray()
          fragment.activity?.launchLibReferencePage(
            refName = actions.componentName,
            refLabel = fragment.getString(com.absinthe.libchecker.R.string.lib_detail_permission_providers),
            refType = com.absinthe.libchecker.annotation.PROVIDER,
            refList = encodedList
          )
        } else {
          context.showToast(com.absinthe.libchecker.R.string.lib_detail_permission_providers_empty)
        }
      }
    }
  }

  fun clear() {
    pendingExtractionItem = null
    dismissLoading()
  }

  private fun collectNativeLibraryExtractionResults() {
    coroutineScope.launch {
      viewModel.nativeLibraryExtractionResults.collect(::handleNativeLibraryExtractionResult)
    }
  }

  private fun handleNativeLibraryExtractionResult(extractionResult: NativeLibraryExtractionResult) {
    if (extractionResult.item != pendingExtractionItem) {
      return
    }

    pendingExtractionItem = null
    dismissLoading()
    val context = fragment.context ?: return
    extractionResult.result.onSuccess {
      context.showToast(com.absinthe.libchecker.R.string.lib_detail_elf_extract_success)
    }.onFailure { e ->
      Timber.e(e, "Failed to extract ELF: ${extractionResult.item}")
      context.showToast(com.absinthe.libchecker.R.string.lib_detail_elf_extract_failed)
    }
  }

  private fun showLoading() {
    dismissLoading()
    loadingDialog = UiUtils.createLoadingDialog(fragment.requireActivity()).also {
      it.show()
    }
  }

  private fun dismissLoading() {
    loadingDialog?.dismiss()
    loadingDialog = null
  }

  private fun addIntegrationActions(
    arrayAdapter: ArrayAdapter<String>,
    actionMap: MutableMap<Int, () -> Unit>,
    actions: DetailItemLongClickActions,
    position: Int
  ) {
    if (!actions.integrationsAvailable) {
      return
    }

    addBlockerAction(arrayAdapter, actionMap, actions, position)
    addMonkeyKingAction(arrayAdapter, actionMap, actions, position)
    addAnywhereAction(arrayAdapter, actionMap, actions)
  }

  private fun addBlockerAction(
    arrayAdapter: ArrayAdapter<String>,
    actionMap: MutableMap<Int, () -> Unit>,
    actions: DetailItemLongClickActions,
    position: Int
  ) {
    if (fragment !is ComponentsAnalysisFragment || !BlockerManager.isSupportInteraction) {
      return
    }

    val context = fragment.requireContext()
    if (integrationBlockerList == null) {
      integrationBlockerList = BlockerManager().queryBlockedComponent(context, actions.packageName)
    }
    val blockerShouldBlock = integrationBlockerList?.any { it.name == actions.fullComponentName } == false
    val blockStr = if (blockerShouldBlock) {
      com.absinthe.libchecker.R.string.integration_blocker_menu_block
    } else {
      com.absinthe.libchecker.R.string.integration_blocker_menu_unblock
    }
    arrayAdapter.add(fragment.getString(blockStr))
    actionMap[arrayAdapter.count - 1] = {
      BlockerManager().apply {
        addBlockedComponent(
          context,
          actions.packageName,
          actions.componentName,
          type(),
          blockerShouldBlock
        )
        integrationBlockerList = queryBlockedComponent(context, actions.packageName)
        val shouldTurnToDisable =
          integrationBlockerList?.any { it.name == actions.fullComponentName } == true && blockerShouldBlock
        animateTvTitle(position, shouldTurnToDisable)
      }
    }
  }

  private fun addMonkeyKingAction(
    arrayAdapter: ArrayAdapter<String>,
    actionMap: MutableMap<Int, () -> Unit>,
    actions: DetailItemLongClickActions,
    position: Int
  ) {
    if (fragment !is ComponentsAnalysisFragment || !MonkeyKingManager.isSupportInteraction) {
      return
    }

    val context = fragment.requireContext()
    if (integrationMonkeyKingBlockList == null) {
      integrationMonkeyKingBlockList = MonkeyKingManager().queryBlockedComponent(context, actions.packageName)
    }
    val monkeyKingShouldBlock = integrationMonkeyKingBlockList?.any { it.name == actions.componentName } == false
    val actionTitle = if (monkeyKingShouldBlock) {
      com.absinthe.libchecker.R.string.integration_monkey_king_menu_block
    } else {
      com.absinthe.libchecker.R.string.integration_monkey_king_menu_unblock
    }
    arrayAdapter.add(fragment.getString(actionTitle))
    actionMap[arrayAdapter.count - 1] = {
      MonkeyKingManager().apply {
        addBlockedComponent(
          context,
          actions.packageName,
          actions.componentName,
          type(),
          monkeyKingShouldBlock
        )
        integrationMonkeyKingBlockList = queryBlockedComponent(context, actions.packageName)
        val shouldTurnToDisable =
          integrationMonkeyKingBlockList?.any { it.name == actions.fullComponentName } == true && monkeyKingShouldBlock
        animateTvTitle(position, shouldTurnToDisable)
      }
    }
  }

  private fun addAnywhereAction(
    arrayAdapter: ArrayAdapter<String>,
    actionMap: MutableMap<Int, () -> Unit>,
    actions: DetailItemLongClickActions
  ) {
    if (type() != ACTIVITY || !AnywhereManager.isSupportInteraction) {
      return
    }

    arrayAdapter.add(fragment.getString(com.absinthe.libchecker.R.string.integration_anywhere_menu_editor))
    actionMap[arrayAdapter.count - 1] = {
      AnywhereManager().launchActivityEditor(
        fragment.requireContext(),
        actions.packageName,
        actions.fullComponentName
      )
    }
  }

  private fun animateTvTitle(position: Int, shouldTurnToDisable: Boolean) {
    (adapter.getViewByPosition(position, android.R.id.title) as? TextView)?.run {
      if (shouldTurnToDisable) startStrikeThroughAnimation() else reverseStrikeThroughAnimation()
    }
  }
}
