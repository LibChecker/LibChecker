package com.absinthe.libchecker.ui.fragment.applist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.NativeLibMap
import com.absinthe.libchecker.databinding.FragmentSoAnalysisBinding
import com.absinthe.libchecker.recyclerview.LibStringAdapter
import com.absinthe.libchecker.recyclerview.MODE_NATIVE
import com.absinthe.libchecker.utils.ActivityStackManager
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.view.dialogfragment.EXTRA_PKG_NAME
import com.absinthe.libchecker.view.dialogfragment.LibDetailDialogFragment
import com.absinthe.libchecker.viewmodel.DetailViewModel

const val MODE_SORT_BY_SIZE = 0
const val MODE_SORT_BY_LIB = 1

class SoAnalysisFragment : Fragment() {

    private lateinit var binding: FragmentSoAnalysisBinding
    private val viewModel by lazy { ViewModelProvider(requireActivity()).get(DetailViewModel::class.java) }
    private val packageName by lazy { arguments?.getString(EXTRA_PKG_NAME) ?: "" }
    private val adapter = LibStringAdapter().apply {
        mode = MODE_NATIVE
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSoAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            list.adapter = this@SoAnalysisFragment.adapter
            ibSort.setOnClickListener {
                GlobalValues.libSortMode.value =
                    if (GlobalValues.libSortMode.value == MODE_SORT_BY_SIZE) {
                        adapter.setList(adapter.data.sortedByDescending {
                            NativeLibMap.MAP.containsKey(
                                it.name
                            )
                        })
                        MODE_SORT_BY_LIB
                    } else {
                        adapter.setList(adapter.data.sortedByDescending { it.size })
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
            libItems.observe(viewLifecycleOwner, Observer {
                adapter.setList(it)
            })
        }

        fun openLibDetailDialog(position: Int) {
            if (GlobalValues.config.enableLibDetail) {
                LibDetailDialogFragment.newInstance(adapter.getItem(position).name).apply {
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
        viewModel.initSoAnalysisData(requireContext(), packageName)
    }

    companion object {
        fun newInstance(packageName: String): SoAnalysisFragment {
            return SoAnalysisFragment()
                .apply {
                    arguments = Bundle().apply {
                        putString(EXTRA_PKG_NAME, packageName)
                    }
                }
        }
    }
}
