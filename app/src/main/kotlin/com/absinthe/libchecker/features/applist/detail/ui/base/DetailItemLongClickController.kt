package com.absinthe.libchecker.features.applist.detail.ui.base

import android.widget.ArrayAdapter
import android.widget.TextView
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.ACTION_IN_RULES
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.ET_NOT_ELF
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.features.applist.Referable
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.applist.detail.ui.ELFDetailDialogFragment
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringAdapter
import com.absinthe.libchecker.features.applist.detail.ui.impl.ComponentsAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.impl.MetaDataAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.impl.NativeAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.impl.PermissionAnalysisFragment
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.core.util.ClipboardUtils
import timber.log.Timber

class DetailItemLongClickController(
  private val fragment: BaseDetailFragment<*>,
  private val viewModel: DetailViewModel,
  private val adapter: LibStringAdapter,
  private val coroutineScope: CoroutineScope,
  private val packageName: () -> String,
  private val type: () -> Int
) {
  private var integrationMonkeyKingBlockList: List<ShareCmpInfo.Component>? = null
  private var integrationBlockerList: List<ShareCmpInfo.Component>? = null

  fun onLongClick(item: LibStringItemChip, position: Int) {
    val context = fragment.requireContext()
    val packageName = packageName()
    val actionMap = mutableMapOf<Int, () -> Unit>()
    val arrayAdapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_1)
    var componentName = item.item.name
    if (fragment is PermissionAnalysisFragment) {
      componentName = componentName.substringBefore(" ")
    }
    val fullComponentName = if (componentName.startsWith(".")) {
      packageName + componentName
    } else {
      componentName
    }

    addCopyAction(arrayAdapter, actionMap, componentName, item)
    addElfActions(arrayAdapter, actionMap, packageName, item)
    addReferenceAction(arrayAdapter, actionMap, componentName, item)
    addIntegrationActions(arrayAdapter, actionMap, packageName, componentName, fullComponentName, position)

    BaseAlertDialogBuilder(context)
      .setAdapter(arrayAdapter) { _, which ->
        actionMap[which]?.invoke()
      }
      .show()
  }

  private fun addCopyAction(
    arrayAdapter: ArrayAdapter<String>,
    actionMap: MutableMap<Int, () -> Unit>,
    componentName: String,
    item: LibStringItemChip
  ) {
    val context = fragment.requireContext()
    arrayAdapter.add(fragment.getString(android.R.string.copy))
    actionMap[arrayAdapter.count - 1] = {
      if (fragment is MetaDataAnalysisFragment) {
        ClipboardUtils.put(context, componentName + ": " + item.item.source)
      } else {
        ClipboardUtils.put(context, componentName)
      }
      VersionCompat.showCopiedOnClipboardToast(context)
    }
  }

  private fun addElfActions(
    arrayAdapter: ArrayAdapter<String>,
    actionMap: MutableMap<Int, () -> Unit>,
    packageName: String,
    item: LibStringItemChip
  ) {
    if (viewModel.isApkPreview || fragment !is NativeAnalysisFragment) {
      return
    }

    val context = fragment.requireContext()
    arrayAdapter.add(fragment.getString(com.absinthe.libchecker.R.string.lib_detail_elf_extract))
    actionMap[arrayAdapter.count - 1] = {
      val loading = UiUtils.createLoadingDialog(fragment.requireActivity())
      coroutineScope.launch {
        withContext(Dispatchers.Main) {
          loading.show()
        }

        val result = viewModel.extractNativeLibrary(item.item)

        withContext(Dispatchers.Main) {
          loading.dismiss()
          result.onSuccess {
            context.showToast(com.absinthe.libchecker.R.string.lib_detail_elf_extract_success)
          }.onFailure { e ->
            Timber.e(e, "Failed to extract ELF: ${item.item}")
            context.showToast(com.absinthe.libchecker.R.string.lib_detail_elf_extract_failed)
          }
        }
      }
    }

    if (item.item.elfInfo.elfType != ET_NOT_ELF) {
      arrayAdapter.add(fragment.getString(com.absinthe.libchecker.R.string.lib_detail_elf_info))
      actionMap[arrayAdapter.count - 1] = {
        ELFDetailDialogFragment.newInstance(
          packageName = packageName,
          elfPath = item.item.source.orEmpty(),
          ruleIcon = item.rule?.iconRes
            ?: com.absinthe.lc.rulesbundle.R.drawable.ic_sdk_placeholder
        ).show(fragment.childFragmentManager, ELFDetailDialogFragment::class.java.name)
      }
    }
  }

  private fun addReferenceAction(
    arrayAdapter: ArrayAdapter<String>,
    actionMap: MutableMap<Int, () -> Unit>,
    componentName: String,
    item: LibStringItemChip
  ) {
    if (fragment !is Referable || componentName.startsWith(".")) {
      return
    }

    arrayAdapter.add(fragment.getString(com.absinthe.libchecker.R.string.tab_lib_reference_statistics))
    actionMap[arrayAdapter.count - 1] = {
      val refName = item.rule?.libName ?: componentName
      val libType = if (item.rule?.libType == ACTION_IN_RULES) ACTION else type()
      fragment.activity?.launchLibReferencePage(refName, item.rule?.label, libType, null)
    }
  }

  private fun addIntegrationActions(
    arrayAdapter: ArrayAdapter<String>,
    actionMap: MutableMap<Int, () -> Unit>,
    packageName: String,
    componentName: String,
    fullComponentName: String,
    position: Int
  ) {
    if (viewModel.isApk || viewModel.isApkPreview) {
      return
    }

    addBlockerAction(arrayAdapter, actionMap, packageName, componentName, fullComponentName, position)
    addMonkeyKingAction(arrayAdapter, actionMap, packageName, componentName, fullComponentName, position)
    addAnywhereAction(arrayAdapter, actionMap, packageName, componentName)
  }

  private fun addBlockerAction(
    arrayAdapter: ArrayAdapter<String>,
    actionMap: MutableMap<Int, () -> Unit>,
    packageName: String,
    componentName: String,
    fullComponentName: String,
    position: Int
  ) {
    if (fragment !is ComponentsAnalysisFragment || !BlockerManager.isSupportInteraction) {
      return
    }

    val context = fragment.requireContext()
    if (integrationBlockerList == null) {
      integrationBlockerList = BlockerManager().queryBlockedComponent(context, packageName)
    }
    val blockerShouldBlock = integrationBlockerList?.any { it.name == fullComponentName } == false
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
          packageName,
          componentName,
          type(),
          blockerShouldBlock
        )
        integrationBlockerList = queryBlockedComponent(context, packageName)
        val shouldTurnToDisable =
          integrationBlockerList?.any { it.name == fullComponentName } == true && blockerShouldBlock
        animateTvTitle(position, shouldTurnToDisable)
      }
    }
  }

  private fun addMonkeyKingAction(
    arrayAdapter: ArrayAdapter<String>,
    actionMap: MutableMap<Int, () -> Unit>,
    packageName: String,
    componentName: String,
    fullComponentName: String,
    position: Int
  ) {
    if (fragment !is ComponentsAnalysisFragment || !MonkeyKingManager.isSupportInteraction) {
      return
    }

    val context = fragment.requireContext()
    if (integrationMonkeyKingBlockList == null) {
      integrationMonkeyKingBlockList = MonkeyKingManager().queryBlockedComponent(context, packageName)
    }
    val monkeyKingShouldBlock = integrationMonkeyKingBlockList?.any { it.name == componentName } == false
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
          packageName,
          componentName,
          type(),
          monkeyKingShouldBlock
        )
        integrationMonkeyKingBlockList = queryBlockedComponent(context, packageName)
        val shouldTurnToDisable =
          integrationMonkeyKingBlockList?.any { it.name == fullComponentName } == true && monkeyKingShouldBlock
        animateTvTitle(position, shouldTurnToDisable)
      }
    }
  }

  private fun addAnywhereAction(
    arrayAdapter: ArrayAdapter<String>,
    actionMap: MutableMap<Int, () -> Unit>,
    packageName: String,
    componentName: String
  ) {
    if (type() != ACTIVITY || !AnywhereManager.isSupportInteraction) {
      return
    }

    arrayAdapter.add(fragment.getString(com.absinthe.libchecker.R.string.integration_anywhere_menu_editor))
    actionMap[arrayAdapter.count - 1] = {
      AnywhereManager().launchActivityEditor(
        fragment.requireContext(),
        packageName,
        componentName
      )
    }
  }

  private fun animateTvTitle(position: Int, shouldTurnToDisable: Boolean) {
    (adapter.getViewByPosition(position, android.R.id.title) as? TextView)?.run {
      if (shouldTurnToDisable) startStrikeThroughAnimation() else reverseStrikeThroughAnimation()
    }
  }
}
