package com.absinthe.libchecker.ui.fragment.applist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.NATIVE
import com.absinthe.libchecker.constant.librarymap.BaseMap
import com.absinthe.libchecker.constant.librarymap.NativeLibMap
import com.absinthe.libchecker.databinding.FragmentLibNativeBinding
import com.absinthe.libchecker.databinding.LayoutEmptyListBinding
import com.absinthe.libchecker.recyclerview.adapter.LibStringAdapter
import com.absinthe.libchecker.recyclerview.diff.LibStringDiffUtil
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.fragment.BaseFragment
import com.absinthe.libchecker.utils.ActivityStackManager
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.view.dialogfragment.LibDetailDialogFragment
import com.absinthe.libchecker.viewmodel.DetailViewModel
import com.blankj.utilcode.util.ToastUtils
import rikka.core.util.ClipboardUtils
import java.lang.ref.WeakReference

const val MODE_SORT_BY_SIZE = 0
const val MODE_SORT_BY_LIB = 1

class NativeAnalysisFragment : BaseFragment<FragmentLibNativeBinding>(R.layout.fragment_lib_native), Sortable {

    private val viewModel by activityViewModels<DetailViewModel>()
    private val emptyLayoutBinding by lazy { LayoutEmptyListBinding.inflate(layoutInflater) }
    private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) ?: "" }
    private val adapter = LibStringAdapter(NATIVE)

    override fun initBinding(view: View): FragmentLibNativeBinding = FragmentLibNativeBinding.bind(view)

    override fun init() {
        binding.apply {
            list.apply {
                adapter = this@NativeAnalysisFragment.adapter
                setPadding(paddingStart, paddingTop, paddingEnd, paddingBottom + UiUtils.getNavBarHeight())
            }
        }

        viewModel.apply {
            libItems.observe(viewLifecycleOwner, Observer {
                if (it.isEmpty()) {
                    emptyLayoutBinding.text.text = getString(R.string.empty_list)
                } else {
                    adapter.setDiffNewData(it.toMutableList())
                }
            })
        }

        fun openLibDetailDialog(position: Int) {
            if (GlobalValues.config.enableLibDetail) {
                val name = adapter.getItem(position).name
                val regexName = NativeLibMap.findRegex(name)?.regexName
                LibDetailDialogFragment.newInstance(name, adapter.type, regexName)
                    .apply {
                        ActivityStackManager.topActivity?.apply {
                            show(supportFragmentManager, tag)
                        }
                    }
            }
        }

        adapter.apply {
            setOnItemClickListener { _, _, position ->
                openLibDetailDialog(position)
            }
            setOnItemLongClickListener { _, _, position ->
                ClipboardUtils.put(requireContext(), getItem(position).name)
                ToastUtils.showShort(R.string.toast_copied_to_clipboard)
                true
            }
            setOnItemChildClickListener { _, _, position ->
                openLibDetailDialog(position)
            }
            setDiffCallback(LibStringDiffUtil())
            emptyLayoutBinding.text.text = getString(R.string.loading)
            setEmptyView(emptyLayoutBinding.root)
        }
        viewModel.initSoAnalysisData(requireContext(), packageName)
    }

    override fun onResume() {
        super.onResume()
        Sortable.currentReference = WeakReference(this)
    }

    companion object {
        fun newInstance(packageName: String): NativeAnalysisFragment {
            return NativeAnalysisFragment()
                .apply {
                    arguments = Bundle().apply {
                        putString(EXTRA_PACKAGE_NAME, packageName)
                    }
                }
        }
    }

    override fun sort() {
        viewModel.sortMode = if (viewModel.sortMode == MODE_SORT_BY_SIZE) {
            val map = BaseMap.getMap(adapter.type)
            adapter.setDiffNewData(adapter.data.sortedByDescending { map.contains(it.name) }
                .toMutableList())
            MODE_SORT_BY_LIB
        } else {
            adapter.setDiffNewData(adapter.data.sortedByDescending { it.size }
                .toMutableList())
            MODE_SORT_BY_SIZE
        }
        GlobalValues.libSortMode.value = viewModel.sortMode
        SPUtils.putInt(Constants.PREF_LIB_SORT_MODE, viewModel.sortMode)
    }
}
