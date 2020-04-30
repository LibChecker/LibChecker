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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.databinding.FragmentAppListBinding
import com.absinthe.libchecker.recyclerview.AppListDiffUtil
import com.absinthe.libchecker.utils.Constants
import com.absinthe.libchecker.utils.GlobalValues
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.viewholder.AppItem
import com.absinthe.libchecker.viewholder.AppItemViewBinder
import com.absinthe.libchecker.viewmodel.AppViewModel
import com.drakeet.multitype.MultiTypeAdapter
import jonathanfinerty.once.Once


class AppListFragment : Fragment(), SearchView.OnQueryTextListener {

    private lateinit var binding: FragmentAppListBinding
    private lateinit var viewModel: AppViewModel
    private val mAdapter = MultiTypeAdapter()
    private val mItems = ArrayList<AppItem>()
    private val mTempItems = ArrayList<AppItem>()
    private var mIsInit = false

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
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
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
                    if (!refreshLock.value!!) {
                        if (!mIsInit) {
                            binding.vfContainer.displayedChild = 0
                            mIsInit = true
                        }

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
                mTempItems.apply {
                    clear()
                    addAll(it)
                }

                binding.vfContainer.displayedChild = 1

                if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)) {
                    Once.markDone(OnceTag.FIRST_LAUNCH)
                }
            })

            GlobalValues.apply {
                isShowSystemApps.observe(viewLifecycleOwner, Observer {
                    addItem(requireContext())
                })
                sortMode.observe(viewLifecycleOwner, Observer { mode ->
                    val newItems = mTempItems

                    when (mode) {
                        Constants.SORT_MODE_NAME_DESC -> newItems.sortedWith(compareByDescending<AppItem> { it.appName }.thenBy { it.abi })
                        Constants.SORT_MODE_NAME_ASC -> newItems.sortWith(compareBy({ it.abi }, { it.appName }))
                        Constants.SORT_MODE_UPDATE_TIME_DESC -> newItems.sortByDescending { it.updateTime }
                    }
                    updateItems(newItems)
                })
            }
        }
    }

    private fun updateItems(newItems: List<AppItem>) {
        if (mTempItems.isEmpty()) {
            mAdapter.items = newItems
            mAdapter.notifyDataSetChanged()
        } else {
            val diffResult = DiffUtil.calculateDiff(AppListDiffUtil(mTempItems, newItems), true)
            diffResult.dispatchUpdatesTo(mAdapter)
            mAdapter.items = mTempItems
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
        mTempItems.apply {
            clear()
            addAll(filter)
        }
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

                popup.menu[GlobalValues.sortMode.value ?: Constants.SORT_MODE_NAME_ASC].isChecked =
                    true
                popup.setOnMenuItemClickListener { menuItem ->
                    val mode = when (menuItem.itemId) {
                        R.id.sort_by_update_time_desc -> Constants.SORT_MODE_UPDATE_TIME_DESC
                        R.id.sort_by_name_desc -> Constants.SORT_MODE_NAME_DESC
                        R.id.sort_by_name_asc -> Constants.SORT_MODE_NAME_ASC
                        else -> Constants.SORT_MODE_NAME_ASC
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
