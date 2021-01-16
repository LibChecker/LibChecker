package com.absinthe.libchecker.ui.fragment.applist

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.databinding.FragmentLibNativeBinding
import com.absinthe.libchecker.databinding.LayoutDexEmptyListBinding
import com.absinthe.libchecker.databinding.LayoutEmptyListBinding
import com.absinthe.libchecker.extensions.addPaddingBottom
import com.absinthe.libchecker.recyclerview.diff.LibStringDiffUtil
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.fragment.BaseDetailFragment
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.view.dialogfragment.LibDetailDialogFragment
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.libraries.utils.utils.UiUtils
import rikka.core.util.ClipboardUtils

const val MODE_SORT_BY_SIZE = 0
const val MODE_SORT_BY_LIB = 1

class NativeAnalysisFragment : BaseDetailFragment<FragmentLibNativeBinding>(R.layout.fragment_lib_native) {

    private val emptyLayoutBinding by lazy { LayoutEmptyListBinding.inflate(layoutInflater) }
    private val dexEmptyLayoutBinding by lazy { LayoutDexEmptyListBinding.inflate(layoutInflater) }
    private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) ?: "" }

    override fun initBinding(view: View): FragmentLibNativeBinding = FragmentLibNativeBinding.bind(view)

    override fun init() {
        binding.apply {
            list.apply {
                adapter = this@NativeAnalysisFragment.adapter
                addPaddingBottom(UiUtils.getNavBarHeight(requireActivity().contentResolver))
                if (type == DEX) {
                    addItemDecoration(
                        DividerItemDecoration(
                            requireContext(),
                            DividerItemDecoration.VERTICAL
                        )
                    )
                }
            }
        }

        viewModel.apply {
            val observer = Observer<List<LibStringItemChip>> {
                if (it.isEmpty()) {
                    if (type == DEX) {
                        dexEmptyLayoutBinding.text.text = getString(R.string.uncharted_territory)
                    } else {
                        emptyLayoutBinding.text.text = getString(R.string.empty_list)
                    }
                } else {
                    adapter.setDiffNewData(it.toMutableList())
                }

                if (!isListReady) {
                    viewModel.itemsCountLiveData.value = it.size
                    isListReady = true
                }
            }

            if (type == DEX) {
                dexLibItems.observe(viewLifecycleOwner, observer)
            } else {
                nativeLibItems.observe(viewLifecycleOwner, observer)
            }
        }

        fun openLibDetailDialog(position: Int) {
            val name = adapter.getItem(position).item.name
            val regexName = LCAppUtils.findRuleRegex(name)?.regexName
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
            setOnItemChildClickListener { _, view, position ->
                if (AntiShakeUtils.isInvalidClick(view)) {
                    return@setOnItemChildClickListener
                }
                openLibDetailDialog(position)
            }
            setDiffCallback(LibStringDiffUtil())

            if (type == DEX) {
                dexEmptyLayoutBinding.text.text = getString(R.string.loading)
            } else {
                emptyLayoutBinding.text.text = getString(R.string.loading)
            }

            if (type == DEX) {
                setEmptyView(dexEmptyLayoutBinding.root)
            } else {
                setEmptyView(emptyLayoutBinding.root)
            }
        }

        if (type == DEX) {
            viewModel.initDexData(packageName)
        } else {
            viewModel.initSoAnalysisData(packageName)
        }
    }

    companion object {
        fun newInstance(packageName: String, @LibType type: Int): NativeAnalysisFragment {
            return NativeAnalysisFragment()
                .apply {
                    arguments = Bundle().apply {
                        putString(EXTRA_PACKAGE_NAME, packageName)
                        putInt(EXTRA_TYPE, type)
                    }
                }
        }
    }
}
