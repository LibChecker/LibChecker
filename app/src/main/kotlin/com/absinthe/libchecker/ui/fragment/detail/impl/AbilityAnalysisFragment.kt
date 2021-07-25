package com.absinthe.libchecker.ui.fragment.detail.impl

import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.bean.DISABLED
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.recyclerview.diff.LibStringDiffUtil
import com.absinthe.libchecker.ui.fragment.BaseDetailFragment
import com.absinthe.libchecker.ui.fragment.EXTRA_TYPE
import com.absinthe.libchecker.ui.fragment.detail.LibDetailDialogFragment
import com.absinthe.libchecker.ui.fragment.detail.LocatedCount
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_LIB
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.core.util.ClipboardUtils

class AbilityAnalysisFragment : BaseDetailFragment<FragmentLibComponentBinding>(
    R.layout.fragment_lib_component
) {

    override fun initBinding(view: View): FragmentLibComponentBinding =
        FragmentLibComponentBinding.bind(view)

    override fun getRecyclerView() = binding.list

    override fun init() {
        binding.apply {
            list.apply {
                adapter = this@AbilityAnalysisFragment.adapter
            }
        }

        viewModel.apply {
            abilitiesMap[adapter.type]?.observe(viewLifecycleOwner) { componentList ->
                if (componentList.isEmpty()) {
                    emptyView.text.text = getString(R.string.empty_list)
                } else {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val list = mutableListOf<LibStringItemChip>()

                        for (item in componentList) {
                            list += if (item.enabled) {
                                LibStringItemChip(LibStringItem(item.componentName), null)
                            } else {
                                LibStringItemChip(
                                    LibStringItem(
                                        name = item.componentName,
                                        source = DISABLED
                                    ),
                                    null
                                )
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
                                    requireContext(), DividerItemDecoration.VERTICAL
                                )
                            )
                            adapter.setDiffNewData(list, navigateToComponentTask)
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

        fun openLibDetailDialog(position: Int) {
            val name = adapter.getItem(position).item.name
            val regexName = LCAppUtils.findRuleRegex(name, adapter.type)?.regexName

            LibDetailDialogFragment.newInstance(name, adapter.type, regexName)
                .show(childFragmentManager, tag)
        }

        adapter.apply {
//            setOnItemClickListener { _, view, position ->
//                if (AntiShakeUtils.isInvalidClick(view)) {
//                    return@setOnItemClickListener
//                }
//                openLibDetailDialog(position)
//            }
            setOnItemLongClickListener { _, _, position ->
                doOnLongClick(getItem(position).item.name)
                true
            }
            setDiffCallback(LibStringDiffUtil())
            setEmptyView(emptyView)
        }
    }

    private fun doOnLongClick(componentName: String) {
        ClipboardUtils.put(requireContext(), componentName)
        context?.showToast(R.string.toast_copied_to_clipboard)
    }

    companion object {
        fun newInstance(@LibType type: Int): AbilityAnalysisFragment {
            return AbilityAnalysisFragment().putArguments(
                EXTRA_TYPE to type
            )
        }
    }
}
