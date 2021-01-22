package com.absinthe.libchecker.ui.fragment.applist

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.content.res.Configuration
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
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.databinding.FragmentAppListBinding
import com.absinthe.libchecker.extensions.valueUnsafe
import com.absinthe.libchecker.recyclerview.adapter.AppAdapter
import com.absinthe.libchecker.recyclerview.diff.AppListDiffUtil
import com.absinthe.libchecker.ui.detail.AppDetailActivity
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.fragment.BaseListControllerFragment
import com.absinthe.libchecker.ui.main.MainActivity
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.doOnMainThreadIdle
import com.absinthe.libchecker.viewmodel.AppViewModel
import com.absinthe.libraries.utils.extensions.addPaddingBottom
import com.absinthe.libraries.utils.extensions.addPaddingTop
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.libraries.utils.utils.UiUtils
import com.blankj.utilcode.util.BarUtils
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import jonathanfinerty.once.Once
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import rikka.material.widget.BorderView

const val VF_LOADING = 0
const val VF_LIST = 1

class AppListFragment : BaseListControllerFragment<FragmentAppListBinding>(R.layout.fragment_app_list), SearchView.OnQueryTextListener {

    private val isFirstLaunch = !Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)
    private val viewModel by activityViewModels<AppViewModel>()
    private val mAdapter = AppAdapter()
    private var hasRequestChanges = false
    private var isListReady = false
    private var menu: Menu? = null

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

                if (GlobalValues.isShowEntryAnimation.valueUnsafe) {
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
                borderDelegate = borderViewDelegate
                layoutManager = getSuitableLayoutManager()
                borderVisibilityChangedListener =
                    BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
                        (requireActivity() as MainActivity).appBar?.setRaised(!top)
                    }
                setHasFixedSize(true)
                addPaddingTop(BarUtils.getStatusBarHeight())
                addPaddingBottom(UiUtils.getNavBarHeight(requireActivity().contentResolver))
                FastScrollerBuilder(this).useMd2Style().build()
            }
            vfContainer.apply {
                setInAnimation(activity, R.anim.anim_fade_in)
                setOutAnimation(activity, R.anim.anim_fade_out)
            }
            tvFirstTip.isVisible = isFirstLaunch
            loading.enableMergePathsForKitKatAndAbove(true)
        }

        initObserver()
    }

    override fun onResume() {
        super.onResume()

        if (!binding.recyclerview.canScrollVertically(-1)) {
            viewModel.shouldReturnTopOfList = true
        }
        if (!isFirstLaunch && isListReady) {
            if (AppItemRepository.shouldRefreshAppList) {
                viewModel.dbItems.value?.let {
                    updateItems(it)
                    AppItemRepository.shouldRefreshAppList = false
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        mAdapter.release()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.app_list_menu, menu)
        this.menu = menu

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

            if (!isListReady) {
                isVisible = false
            }
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.recyclerview.layoutManager = getSuitableLayoutManager()
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        viewModel.dbItems.value?.let { allDatabaseItems ->
            val filter = allDatabaseItems.filter {
                it.label.contains(newText, ignoreCase = true) ||
                        it.packageName.contains(newText, ignoreCase = true)
            }
            updateItems(filter)

            if (newText.equals("Easter Egg", true)) {
                Toasty.show(requireContext(), "🥚")
                Analytics.trackEvent(Constants.Event.EASTER_EGG, EventProperties().set("EASTER_EGG", "AppList Search"))
            }
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

    private fun initObserver() {
        viewModel.apply {
            reloadAppsFlag.observe(viewLifecycleOwner, {
                if (isListReady && it) {
                    binding.tvFirstTip.isVisible = true
                    doOnMainThreadIdle({
                        flip(VF_LOADING)
                        initItems()
                    })
                }
            })
            dbItems.observe(viewLifecycleOwner, {
                if (it.isNullOrEmpty()) {
                    return@observe
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        updateItems(it)
                    }
                }

                if (!hasRequestChanges) {
                    viewModel.requestChange(requireActivity().packageManager)
                    hasRequestChanges = true

                    if (isFirstLaunch) {
                        binding.tvFirstTip.isGone = true
                        Once.markDone(OnceTag.FIRST_LAUNCH)
                    }
                }
            })
        }

        GlobalValues.apply {
            isShowSystemApps.observe(viewLifecycleOwner, {
                if (isListReady) {
                    updateItems(viewModel.dbItems.value!!)
                }
            })
            appSortMode.observe(viewLifecycleOwner, { mode ->
                if (isListReady) {
                    viewModel.dbItems.value?.let { allDatabaseItems ->
                        val list = allDatabaseItems.toMutableList()

                        when (mode) {
                            Constants.SORT_MODE_DEFAULT -> list.sortWith(
                                compareBy(
                                    { it.abi },
                                    { it.label })
                            )
                            Constants.SORT_MODE_UPDATE_TIME_DESC -> list.sortByDescending { it.lastUpdatedTime }
                        }
                        updateItems(list)
                    }
                }
            })
            shouldRequestChange.observe(viewLifecycleOwner, {
                if (isListReady && !it) {
                    doOnMainThreadIdle({
                        flip(VF_LIST)
                    })
                }
            })
        }
    }

    private fun updateItems(newItems: List<LCItem>) {
        val list = mAdapter.data
        val filterList = mutableListOf<LCItem>()

        GlobalValues.isShowSystemApps.value?.let { isShowSystem ->
            newItems.forEach {
                if (isShowSystem || (!isShowSystem && !it.isSystem)) {
                    filterList.add(it)
                }
            }
        }

        when (GlobalValues.appSortMode.value) {
            Constants.SORT_MODE_DEFAULT -> filterList.sortWith(compareBy({ it.abi }, { it.label }))
            Constants.SORT_MODE_UPDATE_TIME_DESC -> filterList.sortByDescending { it.lastUpdatedTime }
        }

        mAdapter.setDiffNewData(filterList)

        if (list != newItems) {
            doOnMainThreadIdle({
                try {
                    flip(VF_LIST)

                    if (GlobalValues.appSortMode.valueUnsafe == Constants.SORT_MODE_UPDATE_TIME_DESC
                        && binding.recyclerview.scrollState != RecyclerView.SCROLL_STATE_DRAGGING
                    ) {
                        if (viewModel.shouldReturnTopOfList) {
                            viewModel.shouldReturnTopOfList = false
                            returnTopOfList()
                        }
                    }

                    menu?.findItem(R.id.search)?.isVisible = true
                    isListReady = true
                } catch (ignore: Exception) {
                }
            })
        }
    }

    private fun returnTopOfList() {
        binding.recyclerview.apply {
            if (canScrollVertically(-1)) {
                smoothScrollToPosition(0)
            }
        }
    }

    private fun getSuitableLayoutManager(): RecyclerView.LayoutManager {
        return when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> LinearLayoutManager(requireContext())
            Configuration.ORIENTATION_LANDSCAPE -> StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            else -> throw IllegalStateException("Wrong orientation at AppListFragment.")
        }
    }

    private fun flip(page: Int) {
        if (viewModel.isInitingItems) {
            return
        }
        if (binding.vfContainer.displayedChild != page) {
            binding.vfContainer.displayedChild = page
        }
        if (page == VF_LOADING) {
            binding.loading.resumeAnimation()
        } else {
            binding.loading.pauseAnimation()
        }
    }

    override fun onReturnTop() {
        if (binding.recyclerview.canScrollVertically(-1)) {
            returnTopOfList()
        } else {
            flip(VF_LOADING)
            viewModel.requestChange(requireActivity().packageManager, true)
        }
    }
}
