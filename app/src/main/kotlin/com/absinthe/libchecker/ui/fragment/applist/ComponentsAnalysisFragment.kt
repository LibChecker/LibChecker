package com.absinthe.libchecker.ui.fragment.applist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.librarymap.ServiceLibMap
import com.absinthe.libchecker.databinding.FragmentManifestAnalysisBinding
import com.absinthe.libchecker.recyclerview.LibStringAdapter
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.ui.main.LibReferenceActivity
import com.absinthe.libchecker.utils.ActivityStackManager
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.view.dialogfragment.EXTRA_PKG_NAME
import com.absinthe.libchecker.view.dialogfragment.LibDetailDialogFragment
import com.absinthe.libchecker.viewmodel.DetailViewModel

class ComponentsAnalysisFragment : Fragment() {

    private lateinit var binding: FragmentManifestAnalysisBinding
    private val viewModel by lazy { ViewModelProvider(requireActivity()).get(DetailViewModel::class.java) }
    private val packageName by lazy { arguments?.getString(EXTRA_PKG_NAME) ?: "" }
    private val adapter = LibStringAdapter().apply {
        mode = LibStringAdapter.Mode.SERVICE
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentManifestAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            list.apply {
                adapter = this@ComponentsAnalysisFragment.adapter
                addItemDecoration(
                    DividerItemDecoration(
                        requireContext(),
                        DividerItemDecoration.VERTICAL
                    )
                )
            }
            ibSort.setOnClickListener {
                GlobalValues.libSortMode.value =
                    if (GlobalValues.libSortMode.value == MODE_SORT_BY_SIZE) {
                        adapter.setList(adapter.data.sortedByDescending {
                            ServiceLibMap.MAP.containsKey(
                                it.name
                            )
                        })
                        MODE_SORT_BY_LIB
                    } else {
                        adapter.setList(adapter.data.sortedByDescending { it.name })
                        MODE_SORT_BY_SIZE
                    }
                SPUtils.putInt(
                    requireContext(),
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
                LibDetailDialogFragment.newInstance(adapter.getItem(position).name, adapter.mode).apply {
                    ActivityStackManager.topActivity?.apply {
                        show(supportFragmentManager, tag)
                    }
                }
            }
        }

        adapter.setOnItemClickListener { _, _, position ->
            openLibDetailDialog(position)
        }
        adapter.setOnItemChildClickListener { _, _, position ->
            openLibDetailDialog(position)
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