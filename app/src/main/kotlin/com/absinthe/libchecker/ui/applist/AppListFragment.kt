package com.absinthe.libchecker.ui.applist

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.MainActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.databinding.FragmentAppListBinding
import com.absinthe.libchecker.recyclerview.AppAdapter
import com.absinthe.libchecker.recyclerview.AppListDiffUtil
import com.absinthe.libchecker.utils.Constants
import com.absinthe.libchecker.utils.GlobalValues
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.view.EXTRA_PKG_NAME
import com.absinthe.libchecker.view.NativeLibDialogFragment
import com.absinthe.libchecker.viewholder.AppItem
import com.absinthe.libchecker.viewmodel.AppViewModel
import jonathanfinerty.once.Once
import kotlinx.coroutines.*


class AppListFragment : Fragment(), SearchView.OnQueryTextListener {

    private lateinit var binding: FragmentAppListBinding
    private lateinit var viewModel: AppViewModel
    private val mAdapter = AppAdapter()
    private val mItems = ArrayList<AppItem>()
    private var added = false

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

    override fun onResume() {
        super.onResume()
        if (added) {
            viewModel.requestChange(requireContext())
        }
    }

    private fun initView() {
        setHasOptionsMenu(true)

        mAdapter.apply {
            setOnItemClickListener { adapter, _, position ->
                NativeLibDialogFragment().apply {
                    arguments = Bundle().apply {
                        putString(EXTRA_PKG_NAME, (adapter.getItem(position) as AppItem).packageName)
                    }
                    MainActivity.instance?.apply {
                        show(supportFragmentManager, tag)
                    }
                }
            }
            setDiffCallback(AppListDiffUtil())
        }

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

        viewModel.apply {
            if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)) {
                initItems(requireContext())
            } else {
                allItems.observe(viewLifecycleOwner, Observer {
                    if (!refreshLock.value!! && !added) {
                        binding.vfContainer.displayedChild = 0

                        if (it.isNullOrEmpty()) {
                            initItems(requireContext())
                        } else {
                            addItem(requireContext())
                        }
                    }
                })
            }

            refreshLock.observe(viewLifecycleOwner, Observer {
                if (!it) {
                    addItem(requireContext())
                }
            })

            items.observe(viewLifecycleOwner, Observer {
                updateItems(it)
                mItems.apply {
                    clear()
                    addAll(it)
                }

                if (!added) {
                    binding.vfContainer.displayedChild = 1
                    requestChange(requireContext())
                    added = true
                }

                if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)) {
                    Once.markDone(OnceTag.FIRST_LAUNCH)
                }
            })
        }

        GlobalValues.apply {
            isShowSystemApps.observe(viewLifecycleOwner, Observer {
                viewModel.addItem(requireContext())
            })
            sortMode.observe(viewLifecycleOwner, Observer { mode ->

                when (mode) {
                    Constants.SORT_MODE_DEFAULT -> mItems.sortWith(
                        compareBy(
                            { it.abi },
                            { it.appName })
                    )
                    Constants.SORT_MODE_UPDATE_TIME_DESC -> mItems.sortByDescending { it.updateTime }
                }
                updateItems(mItems)
            })
        }
    }

    private fun updateItems(newItems: List<AppItem>) {
        mAdapter.setDiffNewData(newItems.toMutableList())

        GlobalScope.launch(Dispatchers.IO) {
            delay(1000)

            withContext(Dispatchers.Main) {
                binding.recyclerview.smoothScrollToPosition(0)
            }
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
        val filter = mItems.filter {
            it.appName.contains(newText) || it.packageName.contains(newText)
        }
        updateItems(filter)
        return false
    }

    @SuppressLint("RestrictedApi")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> {
                val popup = PopupMenu(requireContext(), requireActivity().findViewById(R.id.sort))
                popup.menuInflater.inflate(R.menu.sort_menu, popup.menu)

                if (popup.menu is MenuBuilder) {
                    (popup.menu as MenuBuilder).setOptionalIconsVisible(true)
                }

                popup.menu[GlobalValues.sortMode.value ?: Constants.SORT_MODE_DEFAULT].isChecked =
                    true
                popup.setOnMenuItemClickListener { menuItem ->
                    val mode = when (menuItem.itemId) {
                        R.id.sort_by_update_time_desc -> Constants.SORT_MODE_UPDATE_TIME_DESC
                        R.id.sort_default -> Constants.SORT_MODE_DEFAULT
                        else -> Constants.SORT_MODE_DEFAULT
                    }
                    GlobalValues.sortMode.value = mode
                    SPUtils.putInt(requireContext(), Constants.PREF_SORT_MODE, mode)

                    true
                }

                popup.show()
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
