package com.absinthe.libchecker.ui.fragment.detail.impl

import android.content.Context
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.bean.DISABLED
import com.absinthe.libchecker.bean.LibChip
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.integrations.anywhere_.AnywhereManager
import com.absinthe.libchecker.integrations.monkeyking.MonkeyKingManager
import com.absinthe.libchecker.integrations.monkeyking.ShareCmpInfo
import com.absinthe.libchecker.recyclerview.diff.LibStringDiffUtil
import com.absinthe.libchecker.ui.fragment.BaseDetailFragment
import com.absinthe.libchecker.ui.fragment.EXTRA_TYPE
import com.absinthe.libchecker.ui.fragment.detail.LocatedCount
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_LIB
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.utils.showToast
import com.absinthe.rulesbundle.LCRules
import com.absinthe.rulesbundle.Rule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.core.util.ClipboardUtils

class ComponentsAnalysisFragment : BaseDetailFragment<FragmentLibComponentBinding>() {

  private val hasIntegration by lazy {
    !viewModel.isApk && (MonkeyKingManager.isSupportInteraction || AnywhereManager.isSupportInteraction)
  }
  private var integrationMonkeyKingBlockList: List<ShareCmpInfo.Component>? = null
  private var itemsList: List<LibStringItemChip>? = null

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = true

  override fun init() {
    binding.apply {
      list.apply {
        adapter = this@ComponentsAnalysisFragment.adapter
      }
    }

    viewModel.apply {
      componentsMap[adapter.type]?.observe(viewLifecycleOwner) { componentList ->
        if (componentList.isEmpty()) {
          emptyView.text.text = getString(R.string.empty_list)
        } else {
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
                LibStringItem(name = item.componentName, source = source),
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
        doOnLongClick(context, getItem(position).item.name)
        true
      }
      setDiffCallback(LibStringDiffUtil())
      setEmptyView(emptyView)
    }
  }

  override fun getFilterList(text: String): List<LibStringItemChip>? {
    return itemsList?.filter { it.item.name.contains(text, true) }
  }

  private fun doOnLongClick(context: Context, componentName: String) {
    if (hasIntegration) {
      val arrayAdapter =
        ArrayAdapter<String>(context, android.R.layout.simple_list_item_1)
      arrayAdapter.add(getString(android.R.string.copy))

      // MonkeyKing Purify
      if (integrationMonkeyKingBlockList == null) {
        integrationMonkeyKingBlockList =
          MonkeyKingManager().queryBlockedComponent(context, viewModel.packageInfo.packageName)
      }
      val monkeyKingShouldBlock =
        integrationMonkeyKingBlockList!!.find { it.name == componentName } == null
      if (MonkeyKingManager.isSupportInteraction) {
        if (monkeyKingShouldBlock) {
          arrayAdapter.add(getString(R.string.integration_monkey_king_menu_block))
        } else {
          arrayAdapter.add(getString(R.string.integration_monkey_king_menu_unblock))
        }
      }
      // Anywhere-
      if (AnywhereManager.isSupportInteraction && type == ACTIVITY) {
        arrayAdapter.add(getString(R.string.integration_anywhere_menu_editor))
      }

      BaseAlertDialogBuilder(context)
        .setAdapter(arrayAdapter) { _, which ->
          when (which) {
            0 -> {
              ClipboardUtils.put(context, componentName)
              context.showToast(R.string.toast_copied_to_clipboard)
            }
            1 -> {
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
                }
              }
            }
            2 -> {
              AnywhereManager().launchActivityEditor(
                context,
                viewModel.packageInfo.packageName,
                componentName
              )
            }
            else -> { /*Do nothing*/
            }
          }
        }
        .show()
    } else {
      ClipboardUtils.put(context, componentName)
      context.showToast(R.string.toast_copied_to_clipboard)
    }
  }

  companion object {
    fun newInstance(@LibType type: Int): ComponentsAnalysisFragment {
      return ComponentsAnalysisFragment().putArguments(
        EXTRA_TYPE to type
      )
    }
  }
}
