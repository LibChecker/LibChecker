package com.absinthe.libchecker.ui.fragment.detail.impl

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.bean.DISABLED
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.constant.LibChip
import com.absinthe.libchecker.constant.librarymap.IconResMap
import com.absinthe.libchecker.database.entity.RuleEntity
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.integrations.anywhere_.AnywhereManager
import com.absinthe.libchecker.integrations.monkeyking.MonkeyKingManager
import com.absinthe.libchecker.integrations.monkeyking.ShareCmpInfo
import com.absinthe.libchecker.recyclerview.diff.LibStringDiffUtil
import com.absinthe.libchecker.ui.fragment.BaseDetailFragment
import com.absinthe.libchecker.ui.fragment.EXTRA_TYPE
import com.absinthe.libchecker.ui.fragment.detail.LibDetailDialogFragment
import com.absinthe.libchecker.ui.fragment.detail.LocatedCount
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_LIB
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.core.util.ClipboardUtils


class ComponentsAnalysisFragment : BaseDetailFragment<FragmentLibComponentBinding>(R.layout.fragment_lib_component) {

    private val hasIntegration by lazy {
        !viewModel.isApk && (MonkeyKingManager.isSupportInteraction || (AnywhereManager.isSupportInteraction && type == ACTIVITY))
    }
    private var integrationMonkeyKingBlockList: List<ShareCmpInfo.Component>? = null

    override fun initBinding(view: View): FragmentLibComponentBinding = FragmentLibComponentBinding.bind(view)

    override fun getRecyclerView() = binding.list

    override fun init() {
        binding.apply {
            list.apply {
                adapter = this@ComponentsAnalysisFragment.adapter
            }
        }

        viewModel.apply {
            componentsMap[adapter.type]?.observe(viewLifecycleOwner, { componentList ->
                if (componentList.isEmpty()) {
                    emptyView.text.text = getString(R.string.empty_list)
                } else {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val list = mutableListOf<LibStringItemChip>()
                        var chip: LibChip?
                        var rule: RuleEntity?

                        for (item in componentList) {
                            rule = LCAppUtils.getRuleWithRegex(item.componentName, adapter.type)
                            chip = null
                            if (rule != null) {
                                chip = LibChip(iconRes = IconResMap.getIconRes(rule.iconIndex), name = rule.label, regexName = rule.regexName)
                            }
                            if (item.enabled) {
                                list.add(LibStringItemChip(LibStringItem(item.componentName), chip))
                            } else {
                                list.add(LibStringItemChip(LibStringItem(name = item.componentName, source = DISABLED), chip))
                            }
                        }

                        if (sortMode == MODE_SORT_BY_LIB) {
                            list.sortByDescending { it.chip != null }
                        } else {
                            adapter.data.sortedByDescending { it.item.name }
                        }

                        withContext(Dispatchers.Main) {
                            binding.list.addItemDecoration(
                                DividerItemDecoration(
                                    requireContext(),
                                    DividerItemDecoration.VERTICAL
                                )
                            )
                            adapter.setDiffNewData(list, navigateToComponentTask)
                        }
                    }
                }
                if (!isListReady) {
                    viewModel.itemsCountLiveData.value = LocatedCount(locate = type, count = componentList.size)
                    viewModel.itemsCountList[type] = componentList.size
                    isListReady = true
                }
            })
        }

        fun openLibDetailDialog(position: Int) {
            val name = adapter.getItem(position).item.name
            val regexName = LCAppUtils.findRuleRegex(name, adapter.type)?.regexName

            LibDetailDialogFragment.newInstance(name, adapter.type, regexName).show(childFragmentManager, tag)
        }

        adapter.apply {
            setOnItemClickListener { _, view, position ->
                if (AntiShakeUtils.isInvalidClick(view)) {
                    return@setOnItemClickListener
                }
                openLibDetailDialog(position)
            }
            setOnItemLongClickListener { _, _, position ->
                doOnLongClick(getItem(position).item.name)
                true
            }
            setDiffCallback(LibStringDiffUtil())
            setEmptyView(emptyView)
        }
    }

    private fun doOnLongClick(componentName: String) {
        if (hasIntegration) {
            val arrayAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1)
            arrayAdapter.add(getString(android.R.string.copy))

            //MonkeyKing Purify
            if (integrationMonkeyKingBlockList == null) {
                integrationMonkeyKingBlockList = MonkeyKingManager().queryBlockedComponent(requireContext(), viewModel.packageName)
            }
            val monkeyKingShouldBlock = integrationMonkeyKingBlockList!!.find { it.name == componentName } == null
            if (MonkeyKingManager.isSupportInteraction) {
                if (monkeyKingShouldBlock) {
                    arrayAdapter.add(getString(R.string.integration_monkey_king_menu_block))
                } else {
                    arrayAdapter.add(getString(R.string.integration_monkey_king_menu_unblock))
                }
            }
            //Anywhere-
            if (AnywhereManager.isSupportInteraction && type == ACTIVITY) {
                arrayAdapter.add(getString(R.string.integration_anywhere_menu_editor))
            }

            AlertDialog.Builder(requireContext())
                .setAdapter(arrayAdapter) { _, which ->
                    when (which) {
                        0 -> {
                            ClipboardUtils.put(requireContext(), componentName)
                            Toasty.show(requireContext(), R.string.toast_copied_to_clipboard)
                        }
                        1 -> {
                            if (MonkeyKingManager.isSupportInteraction) {
                                MonkeyKingManager().apply {
                                    addBlockedComponent(
                                        requireContext(),
                                        viewModel.packageName,
                                        componentName,
                                        type,
                                        monkeyKingShouldBlock
                                    )
                                    integrationMonkeyKingBlockList = queryBlockedComponent(requireContext(), viewModel.packageName)
                                }
                            }
                        }
                        2 -> {
                            AnywhereManager().launchActivityEditor(requireActivity(), viewModel.packageName, componentName)
                        }
                        else -> { /*Do nothing*/
                        }
                    }
                }
                .show()
        } else {
            ClipboardUtils.put(requireContext(), componentName)
            Toasty.show(requireContext(), R.string.toast_copied_to_clipboard)
        }
    }

    companion object {
        fun newInstance(@LibType type: Int): ComponentsAnalysisFragment {
            return ComponentsAnalysisFragment()
                .apply {
                    arguments = Bundle().apply {
                        putInt(EXTRA_TYPE, type)
                    }
                }
        }
    }
}
