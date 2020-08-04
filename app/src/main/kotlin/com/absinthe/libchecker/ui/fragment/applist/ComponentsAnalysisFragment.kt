package com.absinthe.libchecker.ui.fragment.applist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.librarymap.BaseMap
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.core.util.ClipboardUtils
import java.lang.ref.WeakReference

const val EXTRA_MODE = "EXTRA_MODE"

class ComponentsAnalysisFragment :
    BaseFragment<FragmentLibComponentBinding>(R.layout.fragment_lib_component), Sortable {

    private val viewModel by activityViewModels<DetailViewModel>()
    private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }
    private val adapter by lazy { LibStringAdapter(arguments?.getSerializable(EXTRA_MODE) as? LibStringAdapter.Mode ?: LibStringAdapter.Mode.SERVICE) }
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
                setPadding(
                    paddingStart,
                    paddingTop,
                    paddingEnd,
                    paddingBottom + UiUtils.getNavBarHeight()
                )
            }
        }

        viewModel.apply {
            componentsMap[adapter.mode]?.observe(viewLifecycleOwner, Observer { componentList ->
                if (componentList.isEmpty()) {
                    emptyLayoutBinding.text.text = getString(R.string.empty_list)
                } else {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val list = mutableListOf<LibStringItem>()
                        for (item in componentList) {
                            list.add(LibStringItem(item))
                        }

                        val map = BaseMap.getMap(adapter.mode)
                        if (sortMode == MODE_SORT_BY_LIB) {
                            list.sortByDescending { map.contains(it.name) }
                        } else {
                            adapter.data.sortedByDescending { it.name }
                        }

                        withContext(Dispatchers.Main) {
                            adapter.setDiffNewData(list)
                        }
                    }
                }
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
            emptyLayoutBinding.text.text = getString(R.string.loading)
            setEmptyView(emptyLayoutBinding.root)
        }

        packageName?.let {
            viewModel.initComponentsData(requireContext(), it)
        }
    }

    override fun onResume() {
        super.onResume()
        Sortable.currentReference = WeakReference(this)
    }

    companion object {
        fun newInstance(
            packageName: String,
            mode: LibStringAdapter.Mode
        ): ComponentsAnalysisFragment {
            return ComponentsAnalysisFragment()
                .apply {
                    arguments = Bundle().apply {
                        putString(EXTRA_PACKAGE_NAME, packageName)
                        putSerializable(EXTRA_MODE, mode)
                    }
                }
        }
    }

    override fun sort() {
        viewModel.sortMode = if (viewModel.sortMode == MODE_SORT_BY_SIZE) {
            val map = BaseMap.getMap(adapter.mode)
            adapter.setDiffNewData(adapter.data.sortedByDescending { map.contains(it.name) }
                .toMutableList())
            MODE_SORT_BY_LIB
        } else {
            adapter.setDiffNewData(adapter.data.sortedByDescending { it.name }
                .toMutableList())
            MODE_SORT_BY_SIZE
        }
        GlobalValues.libSortMode.value = viewModel.sortMode
        SPUtils.putInt(Constants.PREF_LIB_SORT_MODE, viewModel.sortMode)
    }
}