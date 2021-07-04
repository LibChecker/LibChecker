package com.absinthe.libchecker.ui.fragment.detail.impl

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.recyclerview.diff.LibStringDiffUtil
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.fragment.BaseDetailFragment
import com.absinthe.libchecker.ui.fragment.detail.LibDetailDialogFragment
import com.absinthe.libchecker.ui.fragment.detail.LocatedCount
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import rikka.core.util.ClipboardUtils

class DexAnalysisFragment : BaseDetailFragment<FragmentLibComponentBinding>(R.layout.fragment_lib_component) {

    override fun initBinding(view: View): FragmentLibComponentBinding = FragmentLibComponentBinding.bind(view)

    override fun getRecyclerView() = binding.list

    override fun init() {
        binding.apply {
            list.apply {
                adapter = this@DexAnalysisFragment.adapter
                addItemDecoration(
                    DividerItemDecoration(
                        requireContext(),
                        DividerItemDecoration.VERTICAL
                    )
                )
            }
        }

        viewModel.apply {
            val observer = Observer<List<LibStringItemChip>> {
                if (it.isEmpty()) {
                    emptyView.text.text = getString(R.string.uncharted_territory)
                } else {
                    adapter.setDiffNewData(it.toMutableList(), navigateToComponentTask)
                }

                if (!isListReady) {
                    viewModel.itemsCountLiveData.value = LocatedCount(locate = type, count = it.size)
                    viewModel.itemsCountList[type] = it.size
                    isListReady = true
                }
            }

            dexLibItems.observe(viewLifecycleOwner, observer)
        }

        fun openLibDetailDialog(position: Int) {
            val name = adapter.getItem(position).item.name
            val regexName = LCAppUtils.findRuleRegex(name, type)?.regexName
            LibDetailDialogFragment.newInstance(name, type, regexName).show(childFragmentManager, tag)
        }

        adapter.apply {
            animationEnable = true
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
            setDiffCallback(LibStringDiffUtil())
            setEmptyView(emptyView)
        }
        viewModel.initDexData(packageName)
    }

    companion object {
        fun newInstance(packageName: String, @LibType type: Int): DexAnalysisFragment {
            return DexAnalysisFragment()
                .apply {
                    arguments = Bundle().apply {
                        putString(EXTRA_PACKAGE_NAME, packageName)
                        putInt(EXTRA_TYPE, type)
                    }
                }
        }
    }
}
