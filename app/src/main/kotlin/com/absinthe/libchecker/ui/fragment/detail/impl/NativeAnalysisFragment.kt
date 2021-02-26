package com.absinthe.libchecker.ui.fragment.detail.impl

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.databinding.FragmentLibNativeBinding
import com.absinthe.libchecker.databinding.LayoutEmptyListBinding
import com.absinthe.libchecker.extensions.addPaddingBottom
import com.absinthe.libchecker.recyclerview.diff.LibStringDiffUtil
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.fragment.BaseDetailFragment
import com.absinthe.libchecker.ui.fragment.detail.LibDetailDialogFragment
import com.absinthe.libchecker.ui.fragment.detail.LocatedCount
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libraries.utils.manager.SystemBarManager
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import rikka.core.util.ClipboardUtils

class NativeAnalysisFragment : BaseDetailFragment<FragmentLibNativeBinding>(R.layout.fragment_lib_native) {

    private val emptyLayoutBinding by lazy { LayoutEmptyListBinding.inflate(layoutInflater) }
    private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) ?: "" }

    override fun initBinding(view: View): FragmentLibNativeBinding = FragmentLibNativeBinding.bind(view)

    override fun init() {
        binding.apply {
            list.apply {
                adapter = this@NativeAnalysisFragment.adapter
                post { addPaddingBottom(SystemBarManager.navigationBarSize) }
            }
        }

        viewModel.apply {
            val observer = Observer<List<LibStringItemChip>> {
                if (it.isEmpty()) {
                    emptyLayoutBinding.text.text = getString(R.string.empty_list)
                } else {
                    adapter.setDiffNewData(it.toMutableList())
                }

                if (!isListReady) {
                    viewModel.itemsCountLiveData.value = LocatedCount(locate = type, count = it.size)
                    viewModel.itemsCountList[type] = it.size
                    isListReady = true
                }
            }

            nativeLibItems.observe(viewLifecycleOwner, observer)
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
        viewModel.initSoAnalysisData(packageName)
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
