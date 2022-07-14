package com.absinthe.libchecker.ui.fragment.detail.impl

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.bean.DISABLED
import com.absinthe.libchecker.bean.LibChip
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.integrations.anywhere.AnywhereManager
import com.absinthe.libchecker.integrations.blocker.BlockerManager
import com.absinthe.libchecker.integrations.monkeyking.MonkeyKingManager
import com.absinthe.libchecker.integrations.monkeyking.ShareCmpInfo
import com.absinthe.libchecker.recyclerview.diff.LibStringDiffUtil
import com.absinthe.libchecker.ui.fragment.BaseComponentFragment
import com.absinthe.libchecker.ui.fragment.EXTRA_TYPE
import com.absinthe.libchecker.ui.fragment.detail.LocatedCount
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_LIB
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.utils.extensions.reverseStrikeThroughAnimation
import com.absinthe.libchecker.utils.extensions.startStrikeThroughAnimation
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.rulesbundle.LCRules
import com.absinthe.rulesbundle.Rule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.core.util.ClipboardUtils

class ComponentsAnalysisFragment : BaseComponentFragment<FragmentLibComponentBinding>() {

  private val hasIntegration by unsafeLazy {
    BlockerManager.isSupportInteraction ||
      MonkeyKingManager.isSupportInteraction ||
      AnywhereManager.isSupportInteraction
  }
  private var integrationMonkeyKingBlockList: List<ShareCmpInfo.Component>? = null
  private var integrationBlockerList: List<ShareCmpInfo.Component>? = null
  private var itemsList: List<LibStringItemChip>? = null

  override fun getRecyclerView(): RecyclerView = binding.list
  override val needShowLibDetailDialog: Boolean = true

  override fun init() {
    binding.list.adapter = adapter

    viewModel.apply {
      componentsMap.get(adapter.type).observe(viewLifecycleOwner) { componentList ->
        if (componentList.isEmpty()) {
          emptyView.text.text = getString(R.string.empty_list)
        } else {
          if (!componentList.isNullOrEmpty()) {
            adapter.processMap = viewModel.processesMap
          }
          lifecycleScope.launch(Dispatchers.IO) {
            val list = mutableListOf<LibStringItemChip>()
            var chip: LibChip?
            var rule: Rule?
            var source: String?

            for (item in componentList) {
              rule = if (!item.componentName.startsWith(".")) {
                LCRules.getRule(item.componentName, adapter.type, true)
              } else {
                null
              }
              chip = if (rule != null) {
                LibChip(
                  iconRes = rule.iconRes,
                  name = rule.label,
                  regexName = rule.regexName
                )
              } else {
                null
              }
              source = if (item.enabled) null else DISABLED

              list += LibStringItemChip(
                LibStringItem(
                  name = item.componentName,
                  source = source,
                  process = item.processName.takeIf { it.isNotEmpty() }
                ),
                chip
              )
            }

            if (sortMode == MODE_SORT_BY_LIB) {
              list.sortWith(compareByDescending<LibStringItemChip> { it.chip != null }.thenBy { it.item.name })
            } else {
              list.sortBy { it.item.name }
            }
            itemsList = list

            if (viewModel.queriedText?.isNotEmpty() == true) {
              filterList(viewModel.queriedText!!)
            } else {
              withContext(Dispatchers.Main) {
                context?.let {
                  binding.list.addItemDecoration(dividerItemDecoration)
                }
                adapter.setDiffNewData(list, afterListReadyTask)
              }
            }

            if (viewModel.queriedProcess?.isNotEmpty() == true) {
              filterProcesses(viewModel.queriedProcess!!)
            }
          }
        }
        if (!isListReady) {
          viewModel.itemsCountLiveData.value =
            LocatedCount(locate = type, count = componentList.size)
          viewModel.itemsCountList[type] = componentList.size
          isListReady = true
        }
      }
    }

    adapter.apply {
      setOnItemLongClickListener { _, _, position ->
        doOnLongClick(context, getItem(position).item.name, position)
        true
      }
      setDiffCallback(LibStringDiffUtil())
      setEmptyView(emptyView)
    }
  }

  override fun getFilterList(text: String): List<LibStringItemChip>? {
    return itemsList?.filter { it.item.name.contains(text, true) }
  }

  override fun getFilterProcessesList(process: String?): List<LibStringItemChip>? {
    return if (process.isNullOrEmpty()) {
      itemsList
    } else {
      itemsList?.filter { it.item.process == process }
    }
  }

  private fun doOnLongClick(context: Context, componentName: String, position: Int) {
    if (!viewModel.isApk && hasIntegration) {
      val actionMap = mutableMapOf<Int, () -> Unit>()
      val arrayAdapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_1)
      val fullComponentName = if (componentName.startsWith(".")) {
        viewModel.packageInfo.packageName + componentName
      } else {
        componentName
      }

      // Copy
      arrayAdapter.add(getString(android.R.string.copy))
      actionMap[arrayAdapter.count - 1] = {
        ClipboardUtils.put(context, componentName)
        VersionCompat.showCopiedOnClipboardToast(context)
      }

      // Blocker
      if (BlockerManager.isSupportInteraction) {
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
          if (BlockerManager.isSupportInteraction) {
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
      }

      // MonkeyKing Purify
      if (MonkeyKingManager.isSupportInteraction) {
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
          if (MonkeyKingManager.isSupportInteraction) {
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
      }

      // Anywhere-
      if (AnywhereManager.isSupportInteraction && type == ACTIVITY) {
        arrayAdapter.add(getString(R.string.integration_anywhere_menu_editor))
        actionMap[arrayAdapter.count - 1] = {
          AnywhereManager().launchActivityEditor(
            context,
            viewModel.packageInfo.packageName,
            componentName
          )
        }
      }

      BaseAlertDialogBuilder(context)
        .setAdapter(arrayAdapter) { _, which ->
          actionMap[which]?.invoke()
        }
        .show()
    } else {
      ClipboardUtils.put(context, componentName)
      VersionCompat.showCopiedOnClipboardToast(context)
    }
  }

  private fun animateTvTitle(position: Int, shouldTurnToDisable: Boolean) {
    (adapter.getViewByPosition(position, android.R.id.title) as? TextView)?.run {
      if (shouldTurnToDisable) startStrikeThroughAnimation() else reverseStrikeThroughAnimation()
    }
  }

  companion object {
    fun newInstance(@LibType type: Int): ComponentsAnalysisFragment {
      return ComponentsAnalysisFragment().putArguments(EXTRA_TYPE to type)
    }
  }
}
