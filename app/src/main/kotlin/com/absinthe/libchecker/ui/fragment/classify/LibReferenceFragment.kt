package com.absinthe.libchecker.ui.fragment.classify

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.absinthe.libchecker.*
import com.absinthe.libchecker.databinding.FragmentLibReferenceBinding
import com.absinthe.libchecker.recyclerview.LibReferenceAdapter
import com.absinthe.libchecker.ui.main.*
import com.absinthe.libchecker.viewmodel.AppViewModel

class LibReferenceFragment : Fragment() {

    private val viewModel by activityViewModels<AppViewModel>()
    private val adapter = LibReferenceAdapter()
    private lateinit var binding: FragmentLibReferenceBinding
    private var isInit = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLibReferenceBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        if (!isInit) {
            viewModel.libReference.observe(viewLifecycleOwner, Observer {
                adapter.setNewInstance(it.toMutableList())
                binding.vfContainer.displayedChild = 1
            })
            viewModel.computeLibReference(requireContext())
            isInit = true
        }
    }

    private fun initView() {
        binding.rvList.adapter = this.adapter
        binding.vfContainer.apply {
            setInAnimation(activity, R.anim.anim_fade_in)
            setOutAnimation(activity, R.anim.anim_fade_out)
        }
        adapter.setOnItemClickListener { _, _, position ->
            startActivity(Intent(requireContext(), LibReferenceActivity::class.java).apply {
                val item = this@LibReferenceFragment.adapter.data[position]
                putExtra(EXTRA_NAME, item.libName)
                putExtra(
                    EXTRA_TYPE,
                    if (item.libName.endsWith(".so")) TYPE_NATIVE else TYPE_SERVICE
                )
            })
        }
    }
}