package com.absinthe.libchecker.ui.applist

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.databinding.FragmentAppListBinding
import com.absinthe.libchecker.viewholder.AppItem
import com.absinthe.libchecker.viewholder.AppItemViewBinder
import com.drakeet.multitype.MultiTypeAdapter

class AppListFragment : Fragment(), SearchView.OnQueryTextListener {

    private lateinit var binding: FragmentAppListBinding
    private lateinit var viewModel: AppListViewModel
    private val adapter = MultiTypeAdapter()
    private val items = ArrayList<Any>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this).get(AppListViewModel::class.java)
        binding = FragmentAppListBinding.inflate(inflater, container, false)
        initView()

        return binding.root
    }

    private fun initView() {
        setHasOptionsMenu(true)
        adapter.register(AppItemViewBinder())
        adapter
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.app_list_menu, menu)

        val searchView = SearchView(context).apply {
            setIconifiedByDefault(false)
            setOnQueryTextListener(this@AppListFragment)
            queryHint = getText(R.string.search_hint)
            isQueryRefinementEnabled = true

            findViewById<View>(androidx.appcompat.R.id.search_plate).apply {
                setBackgroundColor(Color.TRANSPARENT)
            }
        }

        menu.findItem(R.id.search).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
            actionView = searchView
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        val filter = items.filter {
            (it as AppItem).appName.contains(newText) || it.packageName.contains(newText)
        }
        adapter.items = filter
        adapter.notifyDataSetChanged()
        return false
    }
}
