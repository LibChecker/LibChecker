package com.absinthe.libchecker.ui.fragment.applist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.absinthe.libchecker.constant.NativeLibMap
import com.absinthe.libchecker.databinding.FragmentSoAnalysisBinding
import com.absinthe.libchecker.recyclerview.LibStringAdapter
import com.absinthe.libchecker.view.EXTRA_PKG_NAME
import com.absinthe.libchecker.viewmodel.DetailViewModel

const val MODE_SOR_BY_SIZE = 0
const val MODE_SOR_BY_LIB = 1

class SoAnalysisFragment : Fragment() {

    private lateinit var binding: FragmentSoAnalysisBinding
    private val viewModel by lazy { ViewModelProvider(requireActivity()).get(DetailViewModel::class.java) }
    private val adapter = LibStringAdapter()
    private val packageName by lazy { arguments?.getString(EXTRA_PKG_NAME) ?: "" }
    private var mode = MODE_SOR_BY_SIZE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSoAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            list.adapter = this@SoAnalysisFragment.adapter
            ibSort.setOnClickListener {
                mode = if (mode == MODE_SOR_BY_SIZE) {
                    adapter.setList(adapter.data.sortedByDescending {
                        NativeLibMap.MAP.containsKey(
                            it.name
                        )
                    })
                    MODE_SOR_BY_LIB
                } else {
                    adapter.setList(adapter.data.sortedByDescending { it.size })
                    MODE_SOR_BY_SIZE
                }
            }
        }

        viewModel.apply {
            libItems.observe(viewLifecycleOwner, Observer {
                adapter.setList(it)
            })
            initData(requireContext(), packageName)
        }
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