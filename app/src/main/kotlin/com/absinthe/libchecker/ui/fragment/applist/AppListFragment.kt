package com.absinthe.libchecker.ui.fragment.applist

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.AppItem
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.databinding.FragmentAppListBinding
import com.absinthe.libchecker.recyclerview.adapter.AppAdapter
import com.absinthe.libchecker.recyclerview.diff.AppListDiffUtil
import com.absinthe.libchecker.ui.detail.AppDetailActivity
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.fragment.BaseFragment
import com.absinthe.libchecker.ui.main.MainActivity
import com.absinthe.libchecker.utils.AntiShakeUtils
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.viewmodel.AppViewModel
import com.blankj.utilcode.util.BarUtils
import jonathanfinerty.once.Once
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.material.widget.BorderView

class AppListFragment : BaseFragment<FragmentAppListBinding>(R.layout.fragment_app_list),
    SearchView.OnQueryTextListener {

    private val viewModel by activityViewModels<AppViewModel>()
    private val mAdapter = AppAdapter()
    private var hasInit = false

    override fun initBinding(view: View): FragmentAppListBinding = FragmentAppListBinding.bind(view)

    override fun init() {
        setHasOptionsMenu(true)

        mAdapter.apply {
            setOnItemClickListener { _, view, position ->
                if (AntiShakeUtils.isInvalidClick(view)) {
                    return@setOnItemClickListener
                }

                val intent = Intent(requireActivity(), AppDetailActivity::class.java).apply {
                    putExtras(Bundle().apply {
                        putString(EXTRA_PACKAGE_NAME, mAdapter.getItem(position).packageName)
                    })
                }

                val options = ActivityOptions.makeSceneTransitionAnimation(
                    requireActivity(), view, view.transitionName
                )

                if (GlobalValues.isShowEntryAnimation.value!!) {
                    startActivity(intent, options.toBundle())
                } else {
                    startActivity(intent)
                }
            }
            setDiffCallback(AppListDiffUtil())
        }

        binding.apply {
            recyclerview.apply {
                adapter = mAdapter
                borderVisibilityChangedListener =
                    BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
                        (requireActivity() as MainActivity).appBar?.setRaised(!top)
                    }
                setPadding(0, paddingTop + BarUtils.getStatusBarHeight(), 0, 0)
            }
            vfContainer.apply {
                setInAnimation(activity, R.anim.anim_fade_in)
                setOutAnimation(activity, R.anim.anim_fade_out)
            }
            tvFirstTip.isVisible = !Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)
        }

        AppItemRepository.allItems.value?.let { list ->
            updateItems(list)
        }

        if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.DB_MIGRATE_2_3)) {
            Once.clearDone(OnceTag.FIRST_LAUNCH)
            binding.tvFirstTip.isVisible = true
        }

        viewModel.apply {
            if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)) {
                initItems(requireContext())
            } else {
                dbItems.observe(viewLifecycleOwner, Observer {
                    if (it.isNullOrEmpty()) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            delay(500)
                            if (dbItems.value.isNullOrEmpty()) {
                                initItems(requireContext())
                            } else {
                                if (!viewModel.refreshLock) {
                                    viewModel.refreshLock = true
                                    addItem()
                                }
                            }
                        }
                    } else {
                        if (!viewModel.refreshLock) {
                            viewModel.refreshLock = true
                            addItem()
                        }
                    }
                })
            }

            AppItemRepository.allItems.observe(viewLifecycleOwner, Observer {
                updateItems(it)

                if (!hasInit) {
                    viewModel.requestChange(requireContext())
                    hasInit = true
                    (requireActivity() as MainActivity).hasInit = true
                }

                if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)) {
                    Once.markDone(OnceTag.FIRST_LAUNCH)
                    Once.markDone(OnceTag.DB_MIGRATE_2_3)
                }
            })
            clickBottomItemFlag.observe(viewLifecycleOwner, Observer {
                if (it) { returnTopOfList() }
            })
        }

        GlobalValues.apply {
            isShowSystemApps.observe(viewLifecycleOwner, Observer {
                viewModel.addItem()
            })
            appSortMode.observe(viewLifecycleOwner, Observer { mode ->
                AppItemRepository.allItems.value?.let { allItems ->
                    val list = allItems.toMutableList()

                    when (mode) {
                        Constants.SORT_MODE_DEFAULT -> list.sortWith(compareBy( { it.abi }, { it.appName }))
                        Constants.SORT_MODE_UPDATE_TIME_DESC -> list.sortByDescending { it.updateTime }
                    }
                    updateItems(list)
                }
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.app_list_menu, menu)

        val searchView = SearchView(requireContext()).apply {
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
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        AppItemRepository.allItems.value?.let { allItems ->
            val filter = allItems.filter {
                it.appName.contains(newText, ignoreCase = true) || it.packageName.contains(newText, ignoreCase = true)
            }
            updateItems(filter)
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

                popup.menu[GlobalValues.appSortMode.value
                    ?: Constants.SORT_MODE_DEFAULT].isChecked =
                    true
                popup.setOnMenuItemClickListener { menuItem ->
                    val mode = when (menuItem.itemId) {
                        R.id.sort_by_update_time_desc -> Constants.SORT_MODE_UPDATE_TIME_DESC
                        R.id.sort_default -> Constants.SORT_MODE_DEFAULT
                        else -> Constants.SORT_MODE_DEFAULT
                    }
                    GlobalValues.appSortMode.value = mode
                    SPUtils.putInt(Constants.PREF_APP_SORT_MODE, mode)
                    true
                }

                popup.show()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun updateItems(newItems: List<AppItem>) {
        val list = mAdapter.data
        mAdapter.setDiffNewData(newItems.toMutableList())

        if (list != newItems) {
            lifecycleScope.launch(Dispatchers.IO) {
                delay(300)

                withContext(Dispatchers.Main) {
                    try {
                        if (binding.vfContainer.displayedChild == 0) {
                            binding.vfContainer.displayedChild = 1
                        }
                        
                        if (GlobalValues.appSortMode.value!! == Constants.SORT_MODE_UPDATE_TIME_DESC) {
                            returnTopOfList()
                        }
                    } catch (ignore: Exception) {

                    }
                }
            }
        }
    }

    private fun returnTopOfList() {
        binding.recyclerview.apply {
            if (canScrollVertically(-1)) {
                smoothScrollToPosition(0)
            }
        }
    }
}
