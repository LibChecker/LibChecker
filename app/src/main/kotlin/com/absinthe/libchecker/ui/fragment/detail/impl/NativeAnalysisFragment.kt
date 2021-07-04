package com.absinthe.libchecker.ui.fragment.detail.impl

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.Observer
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.databinding.FragmentLibNativeBinding
import com.absinthe.libchecker.extensions.addPaddingTop
import com.absinthe.libchecker.extensions.dp
import com.absinthe.libchecker.recyclerview.diff.LibStringDiffUtil
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.fragment.BaseDetailFragment
import com.absinthe.libchecker.ui.fragment.detail.LibDetailDialogFragment
import com.absinthe.libchecker.ui.fragment.detail.LocatedCount
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import rikka.core.util.ClipboardUtils

class NativeAnalysisFragment : BaseDetailFragment<FragmentLibNativeBinding>(R.layout.fragment_lib_native) {

    override fun initBinding(view: View): FragmentLibNativeBinding = FragmentLibNativeBinding.bind(view)

    override fun getRecyclerView() = binding.list

    override fun init() {
        binding.apply {
            list.apply {
                adapter = this@NativeAnalysisFragment.adapter
            }
        }

        viewModel.apply {
            val observer = Observer<List<LibStringItemChip>> {
                if (it.isEmpty()) {
                    emptyView.text.text = getString(R.string.empty_list)
                } else {
                    adapter.setDiffNewData(it.toMutableList(), navigateToComponentTask)
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
            setDiffCallback(LibStringDiffUtil())

            emptyView.apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).also {
                    it.gravity = Gravity.CENTER_HORIZONTAL
                }
                addPaddingTop(96.dp)
                text.text = getString(R.string.loading)
            }
            setEmptyView(emptyView)
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
