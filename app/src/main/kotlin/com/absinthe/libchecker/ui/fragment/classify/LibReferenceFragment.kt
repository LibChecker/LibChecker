package com.absinthe.libchecker.ui.fragment.classify

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.absinthe.libchecker.*
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.FragmentLibReferenceBinding
import com.absinthe.libchecker.recyclerview.LibReferenceAdapter
import com.absinthe.libchecker.ui.main.*
import com.absinthe.libchecker.viewmodel.AppViewModel

class LibReferenceFragment : Fragment() {

    private val viewModel by activityViewModels<AppViewModel>()
    private val adapter = LibReferenceAdapter()

    private lateinit var binding: FragmentLibReferenceBinding
    private var isInit = false
    private var category = TYPE_NATIVE

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
            GlobalValues.isShowSystemApps.observe(viewLifecycleOwner, Observer {
                computeRef()
            })
            GlobalValues.libReferenceThreshold.observe(viewLifecycleOwner, Observer {
                computeRef() //Todo Use List#filter() to optimize
            })

            computeRef()
            isInit = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.lib_ref_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.ref_category_all -> {
                category = TYPE_ALL
            }
            R.id.ref_category_service -> {
                category = TYPE_SERVICE
            }
            R.id.ref_category_activity -> {
                category = TYPE_ACTIVITY
            }
            R.id.ref_category_br -> {
                category = TYPE_BROADCAST_RECEIVER
            }
            R.id.ref_category_cp -> {
                category = TYPE_CONTENT_PROVIDER
            }
        }
        computeRef()
        return super.onOptionsItemSelected(item)
    }

    private fun computeRef() {
        binding.vfContainer.displayedChild = 0
        viewModel.computeLibReference(requireContext(), category)
    }

    private fun initView() {
        setHasOptionsMenu(true)

        binding.rvList.adapter = this.adapter
        binding.vfContainer.apply {
            setInAnimation(activity, R.anim.anim_fade_in)
            setOutAnimation(activity, R.anim.anim_fade_out)
        }
        adapter.setOnItemClickListener { _, _, position ->
            startActivity(Intent(requireContext(), LibReferenceActivity::class.java).apply {
                val item = this@LibReferenceFragment.adapter.data[position]
                putExtra(EXTRA_NAME, item.libName)
                putExtra(EXTRA_TYPE, category)
            })
        }
    }
}