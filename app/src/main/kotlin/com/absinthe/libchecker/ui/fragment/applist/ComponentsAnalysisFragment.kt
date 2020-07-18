package com.absinthe.libchecker.ui.fragment.applist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.librarymap.BaseMap
import com.absinthe.libchecker.constant.librarymap.ServiceLibMap
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.recyclerview.adapter.LibStringAdapter
import com.absinthe.libchecker.recyclerview.diff.LibStringDiffUtil
import com.absinthe.libchecker.ui.fragment.BaseFragment
import com.absinthe.libchecker.utils.ActivityStackManager
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.utils.TypeConverter
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.view.dialogfragment.LibDetailDialogFragment
import com.absinthe.libchecker.viewmodel.DetailViewModel
import com.blankj.utilcode.util.ToastUtils
import rikka.core.util.ClipboardUtils

const val EXTRA_PKG_NAME = "EXTRA_PKG_NAME"
const val EXTRA_MODE = "EXTRA_MODE"

class ComponentsAnalysisFragment :
    BaseFragment<FragmentLibComponentBinding>(R.layout.fragment_lib_component) {

    private val viewModel by viewModels<DetailViewModel>()
    private val packageName by lazy { arguments?.getString(EXTRA_PKG_NAME) }
    private val adapter by lazy { LibStringAdapter(arguments?.getSerializable(EXTRA_MODE) as? LibStringAdapter.Mode ?: LibStringAdapter.Mode.SERVICE) }

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
                setPadding(
                    paddingStart,
                    paddingTop,
                    paddingEnd,
                    paddingBottom + UiUtils.getNavBarHeight()
                )
            }
            ibSort.setOnClickListener {
                GlobalValues.libSortMode.value =
                    if (GlobalValues.libSortMode.value == MODE_SORT_BY_SIZE) {
                        adapter.setDiffNewData(adapter.data.sortedByDescending {
                            ServiceLibMap.contains(it.name)
                        }.toMutableList())
                        MODE_SORT_BY_LIB
                    } else {
                        adapter.setDiffNewData(adapter.data.sortedByDescending { it.name }
                            .toMutableList())
                        MODE_SORT_BY_SIZE
                    }
                SPUtils.putInt(
                    Constants.PREF_LIB_SORT_MODE,
                    GlobalValues.libSortMode.value ?: MODE_SORT_BY_SIZE
                )
            }
        }

        viewModel.apply {
            componentsItems.observe(viewLifecycleOwner, Observer {
                val list = mutableListOf<LibStringItem>()
                for (item in it) {
                    list.add(LibStringItem(item))
                }
                adapter.setDiffNewData(list)
            })
        }

        fun openLibDetailDialog(position: Int) {
            if (GlobalValues.config.enableComponentsDetail) {
                val name = adapter.getItem(position).name
                val regexName = BaseMap.getMap(adapter.mode).findRegex(name)?.regexName

                LibDetailDialogFragment.newInstance(name, adapter.mode, regexName)
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
        }

        packageName?.let {
            viewModel.initComponentsData(
                requireContext(),
                it,
                TypeConverter.libModeToRefType(adapter.mode)
            )
        }
    }

    companion object {
        fun newInstance(
            packageName: String,
            mode: LibStringAdapter.Mode
        ): ComponentsAnalysisFragment {
            return ComponentsAnalysisFragment()
                .apply {
                    arguments = Bundle().apply {
                        putString(EXTRA_PKG_NAME, packageName)
                        putSerializable(EXTRA_MODE, mode)
                    }
                }
        }
    }
}