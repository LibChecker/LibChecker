package com.absinthe.libchecker.ui.fragment.applist

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.librarymap.BaseMap
import com.absinthe.libchecker.constant.librarymap.ServiceLibMap
import com.absinthe.libchecker.databinding.FragmentManifestAnalysisBinding
import com.absinthe.libchecker.recyclerview.adapter.LibStringAdapter
import com.absinthe.libchecker.ui.fragment.BaseFragment
import com.absinthe.libchecker.ui.main.LibReferenceActivity
import com.absinthe.libchecker.utils.ActivityStackManager
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.view.EXTRA_PKG_NAME
import com.absinthe.libchecker.view.dialogfragment.LibDetailDialogFragment
import com.absinthe.libchecker.viewmodel.DetailViewModel
import com.blankj.utilcode.util.ToastUtils
import rikka.core.util.ClipboardUtils

class ComponentsAnalysisFragment :
    BaseFragment<FragmentManifestAnalysisBinding>(R.layout.fragment_manifest_analysis) {

    private val viewModel by activityViewModels<DetailViewModel>()
    private val packageName by lazy { arguments?.getString(EXTRA_PKG_NAME) ?: "" }
    private val adapter = LibStringAdapter()
        .apply {
            mode = LibStringAdapter.Mode.SERVICE
        }

    override fun initBinding(view: View): FragmentManifestAnalysisBinding =
        FragmentManifestAnalysisBinding.bind(view)

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
                setPadding(paddingStart, paddingTop, paddingEnd, paddingBottom + UiUtils.getNavBarHeight())
            }
            ibSort.setOnClickListener {
                GlobalValues.libSortMode.value =
                    if (GlobalValues.libSortMode.value == MODE_SORT_BY_SIZE) {
                        adapter.setList(adapter.data.sortedByDescending {
                            ServiceLibMap.contains(it.name)
                        })
                        MODE_SORT_BY_LIB
                    } else {
                        adapter.setList(adapter.data.sortedByDescending { it.name })
                        MODE_SORT_BY_SIZE
                    }
                SPUtils.putInt(
                    Constants.PREF_LIB_SORT_MODE,
                    GlobalValues.libSortMode.value ?: MODE_SORT_BY_SIZE
                )
            }
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    adapter.mode = when (position) {
                        0 -> LibStringAdapter.Mode.SERVICE
                        1 -> LibStringAdapter.Mode.ACTIVITY
                        2 -> LibStringAdapter.Mode.RECEIVER
                        else -> LibStringAdapter.Mode.PROVIDER
                    }
                    val type = when (position) {
                        0 -> LibReferenceActivity.Type.TYPE_SERVICE
                        1 -> LibReferenceActivity.Type.TYPE_ACTIVITY
                        2 -> LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER
                        else -> LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER
                    }
                    viewModel.initComponentsData(requireContext(), packageName, type)
                }
            }
        }

        viewModel.apply {
            componentsItems.observe(viewLifecycleOwner, Observer {
                adapter.setList(it)
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
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.initComponentsData(
            requireContext(),
            packageName,
            LibReferenceActivity.Type.TYPE_SERVICE
        )
    }

    companion object {
        fun newInstance(packageName: String): ComponentsAnalysisFragment {
            return ComponentsAnalysisFragment()
                .apply {
                    arguments = Bundle().apply {
                        putString(EXTRA_PKG_NAME, packageName)
                    }
                }
        }
    }
}