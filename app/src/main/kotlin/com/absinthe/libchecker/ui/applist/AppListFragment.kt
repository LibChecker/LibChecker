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
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.databinding.FragmentAppListBinding
import com.absinthe.libchecker.utils.GlobalValues
import com.absinthe.libchecker.viewholder.AppItem
import com.absinthe.libchecker.viewholder.AppItemViewBinder
import com.absinthe.libchecker.viewmodel.AppViewModel
import com.drakeet.multitype.MultiTypeAdapter
import jonathanfinerty.once.Once

class AppListFragment : Fragment(), SearchView.OnQueryTextListener {

    private lateinit var binding: FragmentAppListBinding
    private lateinit var viewModel: AppViewModel
    private val mAdapter = MultiTypeAdapter()
    private val mItems = ArrayList<Any>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireActivity()).get(AppViewModel::class.java)
        binding = FragmentAppListBinding.inflate(inflater, container, false)
        initView()

        return binding.root
    }

    private fun initView() {
        setHasOptionsMenu(true)
        mAdapter.register(AppItemViewBinder())

        binding.apply {
            recyclerview.apply {
                adapter = mAdapter
                layoutManager = LinearLayoutManager(activity)
            }
            vfContainer.apply {
                setInAnimation(activity, R.anim.anim_fade_in)
                setOutAnimation(activity, R.anim.anim_fade_out)
            }
        }

        if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)) {
            viewModel.initItems(requireContext())
        } else {
            viewModel.allItems.observe(viewLifecycleOwner, Observer {
                if (it.isNullOrEmpty()) {
                    binding.vfContainer.displayedChild = 0
                    viewModel.initItems(requireContext())
                } else {
                    binding.vfContainer.displayedChild = 0
                    viewModel.addItem()
                }
            })
        }

        viewModel.items.observe(viewLifecycleOwner, Observer {
            mItems.apply {
                clear()
                addAll(it)
            }
            mAdapter.apply {
                items = mItems
                notifyDataSetChanged()
            }
            binding.vfContainer.displayedChild = 1

            if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)) {
                Once.markDone(OnceTag.FIRST_LAUNCH)
            }
        })

        GlobalValues.isShowSystemApps.observe(viewLifecycleOwner, Observer {
            viewModel.addItem()
        })
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
        val filter = mItems.filter {
            (it as AppItem).appName.contains(newText) || it.packageName.contains(newText)
        }
        mAdapter.apply {
            items = filter
            notifyDataSetChanged()
        }
        return false
    }
}
