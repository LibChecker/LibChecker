package com.absinthe.libchecker.ui.applist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.databinding.FragmentAppListBinding
import com.absinthe.libchecker.viewholder.AppItemViewBinder
import com.drakeet.multitype.MultiTypeAdapter

class AppListFragment : Fragment() {

    private lateinit var binding: FragmentAppListBinding
    private lateinit var viewModel: AppListViewModel
    private val adapter = MultiTypeAdapter()
    private val items = ArrayList<Any>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider(this).get(AppListViewModel::class.java)
        binding = FragmentAppListBinding.inflate(inflater, container, false)
        initView()

        return binding.root
    }

    private fun initView() {
        adapter.register(AppItemViewBinder())
        binding.recyclerview.adapter = adapter
        binding.recyclerview.layoutManager = LinearLayoutManager(activity)

        binding.vfContainer.setInAnimation(activity, R.anim.anim_fade_in)
        binding.vfContainer.setOutAnimation(activity, R.anim.anim_fade_out)

        viewModel.items.observe(viewLifecycleOwner, Observer {
            items.clear()
            items.addAll(it)
            adapter.items = items
            adapter.notifyDataSetChanged()
            binding.vfContainer.displayedChild = 1
        })
        context?.let {
            binding.vfContainer.displayedChild = 0
            viewModel.getItems(it)
        }
    }
}
