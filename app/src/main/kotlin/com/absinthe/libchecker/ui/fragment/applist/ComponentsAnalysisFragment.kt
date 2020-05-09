package com.absinthe.libchecker.ui.fragment.applist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import com.absinthe.libchecker.constant.ServiceLibMap
import com.absinthe.libchecker.databinding.FragmentManifestAnalysisBinding
import com.absinthe.libchecker.recyclerview.LibStringAdapter
import com.absinthe.libchecker.recyclerview.MODE_SERVICE
import com.absinthe.libchecker.utils.Constants
import com.absinthe.libchecker.utils.GlobalValues
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.view.EXTRA_PKG_NAME
import com.absinthe.libchecker.viewmodel.DetailViewModel

class ComponentsAnalysisFragment : Fragment() {

    private lateinit var binding: FragmentManifestAnalysisBinding
    private val viewModel by lazy { ViewModelProvider(requireActivity()).get(DetailViewModel::class.java) }
    private val packageName by lazy { arguments?.getString(EXTRA_PKG_NAME) ?: "" }
    private val adapter = LibStringAdapter().apply {
        mode = MODE_SERVICE
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
        }

        viewModel.apply {
            componentsItems.observe(viewLifecycleOwner, Observer {
                adapter.setList(it)
            })
            initComponentsData(requireContext(), packageName)
        }
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