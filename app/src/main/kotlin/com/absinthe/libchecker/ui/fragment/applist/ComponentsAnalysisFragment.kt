package com.absinthe.libchecker.ui.fragment.applist

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.constant.librarymap.BaseMap
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.databinding.LayoutEmptyListBinding
import com.absinthe.libchecker.extensions.addPaddingBottom
import com.absinthe.libchecker.recyclerview.diff.LibStringDiffUtil
import com.absinthe.libchecker.ui.fragment.BaseDetailFragment
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.view.dialogfragment.LibDetailDialogFragment
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.libraries.utils.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.core.util.ClipboardUtils

const val EXTRA_TYPE = "EXTRA_TYPE"

class ComponentsAnalysisFragment : BaseDetailFragment<FragmentLibComponentBinding>(R.layout.fragment_lib_component) {

    private val emptyLayoutBinding by lazy { LayoutEmptyListBinding.inflate(layoutInflater) }

    override fun initBinding(view: View): FragmentLibComponentBinding = FragmentLibComponentBinding.bind(view)

    override fun init() {
        binding.apply {
            list.apply {
                adapter = this@ComponentsAnalysisFragment.adapter
                addItemDecoration(
                    DividerItemDecoration(
                        requireContext(),
                        DividerItemDecoration.VERTICAL
                    )
                )
                addPaddingBottom(UiUtils.getNavBarHeight(requireActivity().contentResolver))
            }
        }

        viewModel.apply {
            componentsMap[adapter.type]?.observe(viewLifecycleOwner, { componentList ->
                if (componentList.isEmpty()) {
                    emptyLayoutBinding.text.text = getString(R.string.empty_list)
                } else {
                    lifecycleScope.launch {
                        val list = mutableListOf<LibStringItemChip>()
                        val map = BaseMap.getMap(adapter.type)

                        for (item in componentList) {
                            list.add(LibStringItemChip(LibStringItem(item), map.getChip(item)))
                        }

                        if (sortMode == MODE_SORT_BY_LIB) {
                            list.sortByDescending { it.chip != null }
                        } else {
                            adapter.data.sortedByDescending { it.item.name }
                        }

                        withContext(Dispatchers.Main) {
                            adapter.setDiffNewData(list)
                        }
                    }
                }
                if (!isListReady) {
                    viewModel.itemsCountLiveData.value = componentList.size
                    isListReady = true
                }
            })
        }

        fun openLibDetailDialog(position: Int) {
            val name = adapter.getItem(position).item.name
            val regexName = BaseMap.getMap(adapter.type).findRegex(name)?.regexName

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
                ClipboardUtils.put(requireContext(), getItem(position).item.name)
                Toasty.show(requireContext(), R.string.toast_copied_to_clipboard)
                true
            }
            setOnItemChildClickListener { _, view, position ->
                if (AntiShakeUtils.isInvalidClick(view)) {
                    return@setOnItemChildClickListener
                }
                openLibDetailDialog(position)
            }
            setDiffCallback(LibStringDiffUtil())
            emptyLayoutBinding.text.text = getString(R.string.loading)
            setEmptyView(emptyLayoutBinding.root)
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