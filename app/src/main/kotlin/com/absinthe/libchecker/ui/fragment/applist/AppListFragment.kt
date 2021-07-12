package com.absinthe.libchecker.ui.fragment.applist

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.view.*
import android.widget.TextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.bean.DetailExtraBean
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.databinding.FragmentAppListBinding
import com.absinthe.libchecker.extensions.addPaddingTop
import com.absinthe.libchecker.extensions.tintHighlightText
import com.absinthe.libchecker.extensions.valueUnsafe
import com.absinthe.libchecker.recyclerview.adapter.AppAdapter
import com.absinthe.libchecker.recyclerview.diff.AppListDiffUtil
import com.absinthe.libchecker.ui.detail.AppDetailActivity
import com.absinthe.libchecker.ui.detail.EXTRA_DETAIL_BEAN
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.fragment.BaseListControllerFragment
import com.absinthe.libchecker.ui.main.MainActivity
import com.absinthe.libchecker.utils.*
import com.absinthe.libchecker.utils.harmony.HarmonyOsUtil
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.libraries.utils.utils.UiUtils
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import jonathanfinerty.once.Once
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import rikka.widget.borderview.BorderView
import timber.log.Timber

const val VF_LOADING = 0
const val VF_LIST = 1
const val VF_INIT = 2

class AppListFragment : BaseListControllerFragment<FragmentAppListBinding>(R.layout.fragment_app_list), SearchView.OnQueryTextListener {

    private val mAdapter by unsafeLazy { AppAdapter(lifecycleScope) }
    private var isFirstLaunch = !Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)
    private var popup: PopupMenu? = null

    private lateinit var layoutManager: RecyclerView.LayoutManager

    override fun initBinding(view: View): FragmentAppListBinding = FragmentAppListBinding.bind(view)

    override fun init() {
        setHasOptionsMenu(true)

        mAdapter.apply {
            setOnItemClickListener { _, view, position ->
                if (AntiShakeUtils.isInvalidClick(view)) {
                    return@setOnItemClickListener
                }
                val item = mAdapter.getItem(position)
                val intent = Intent(requireActivity(), AppDetailActivity::class.java)
                    .putExtras(
                        bundleOf(
                            EXTRA_PACKAGE_NAME to item.packageName,
                            EXTRA_DETAIL_BEAN to DetailExtraBean(
                                item.isSplitApk,
                                item.isKotlinUsed,
                                item.variant
                            )
                        )
                    )
                startActivity(intent)
            }
            setDiffCallback(AppListDiffUtil())
            setHasStableIds(true)
        }

        binding.apply {
            list.apply {
                adapter = mAdapter
                borderDelegate = borderViewDelegate
                layoutManager = getSuitableLayoutManager()
                borderVisibilityChangedListener =
                    BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
                        (requireActivity() as MainActivity).appBar?.setRaised(!top)
                    }
                setHasFixedSize(true)
                FastScrollerBuilder(this).useMd2Style().build()
                addPaddingTop(UiUtils.getStatusBarHeight())
            }
            vfContainer.apply {
                setInAnimation(activity, R.anim.anim_fade_in)
                setOutAnimation(activity, R.anim.anim_fade_out)
            }
        }

        initObserver()
    }

    override fun onResume() {
        super.onResume()

        if (!isFirstLaunch && isListReady && AppItemRepository.shouldRefreshAppList) {
            homeViewModel.dbItems.value?.let {
                updateItems(it, false)
                AppItemRepository.shouldRefreshAppList = false
            }
        }
    }

    override fun onPause() {
        super.onPause()
        popup?.dismiss()
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
        binding.list.layoutManager = getSuitableLayoutManager()
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        homeViewModel.dbItems.value?.let { allDatabaseItems ->
            val filter = allDatabaseItems.filter {
                it.label.contains(newText, ignoreCase = true) ||
                        it.packageName.contains(newText, ignoreCase = true)
            }.toMutableList()

            if (HarmonyOsUtil.isHarmonyOs() && newText.contains("Harmony", true)) {
                filter.addAll(allDatabaseItems.filter { it.variant == Constants.VARIANT_HAP })
            }

            mAdapter.highlightText = newText
            updateItems(filter)
            doOnMainThreadIdle({
                val first: Int
                val last: Int
                when (layoutManager) {
                    is LinearLayoutManager -> {
                        first = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                        last = (layoutManager as LinearLayoutManager).findLastVisibleItemPosition() + 3
                    }
                    is StaggeredGridLayoutManager -> {
                        first = (layoutManager as StaggeredGridLayoutManager).findFirstVisibleItemPositions(null).first()
                        last = (layoutManager as StaggeredGridLayoutManager).findLastVisibleItemPositions(null).last() + 3
                    }
                    else -> {
                        first = 0
                        last = 0
                    }
                }

                for (i in first..last) {
                    (mAdapter.getViewByPosition(i, R.id.tv_app_name) as? TextView)?.apply {
                        tintHighlightText(newText, text.toString())
                    }
                    (mAdapter.getViewByPosition(i, R.id.tv_package_name) as? TextView)?.apply {
                        tintHighlightText(newText, text.toString())
                    }
                }
            })

            when {
                newText.equals("Easter Egg", true) -> {
                    context?.showToast("🥚")
                    Analytics.trackEvent(Constants.Event.EASTER_EGG, EventProperties().set("EASTER_EGG", "AppList Search"))
                }
                newText == Constants.COMMAND_DEBUG_MODE -> {
                    GlobalValues.debugMode = true
                    context?.showToast("DEBUG MODE")
                }
                newText == Constants.COMMAND_DEBUG_MODE -> {
                    GlobalValues.debugMode = false
                    context?.showToast("USER MODE")
                }
                else -> {
                }
            }
        }
        return false
    }

    @SuppressLint("RestrictedApi")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> {
                popup = PopupMenu(requireContext(), requireActivity().findViewById(R.id.sort)).apply {
                    menuInflater.inflate(R.menu.sort_menu, menu)

                    if (menu is MenuBuilder) {
                        (menu as MenuBuilder).setOptionalIconsVisible(true)
                    }

                    menu[GlobalValues.appSortMode.value ?: Constants.SORT_MODE_DEFAULT].isChecked = true
                    setOnMenuItemClickListener { menuItem ->
                        val mode = when (menuItem.itemId) {
                            R.id.sort_by_update_time_desc -> Constants.SORT_MODE_UPDATE_TIME_DESC
                            R.id.sort_by_target_api_desc -> Constants.SORT_MODE_TARGET_API_DESC
                            R.id.sort_default -> Constants.SORT_MODE_DEFAULT
                            else -> Constants.SORT_MODE_DEFAULT
                        }
                        GlobalValues.appSortMode.value = mode
                        SPUtils.putInt(Constants.PREF_APP_SORT_MODE, mode)
                        true
                    }
                    setOnDismissListener {
                        popup = null
                    }
                }
                popup?.show()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun initObserver() {
        homeViewModel.apply {
            reloadAppsFlag.observe(viewLifecycleOwner, {
                if (it && appListStatusLiveData.value == STATUS_NOT_START) {
                    Once.clearDone(OnceTag.FIRST_LAUNCH)
                    isFirstLaunch = true
                    doOnMainThreadIdle({
                        flip(VF_INIT)
                        initItems()
                    })
                }
            })

            dbItems.observe(viewLifecycleOwner, {
                if (it.isNullOrEmpty()) {
                    flip(VF_INIT)
                    initItems()
                } else if (appListStatusLiveData.value != STATUS_START_INIT
                    && appListStatusLiveData.value != STATUS_START_REQUEST_CHANGE) {
                    updateItems(it)
                    homeViewModel.requestChange()
                }
            })
            appListStatusLiveData.observe(viewLifecycleOwner, { status ->
                Timber.d("AppList status updates to $status")
                when(status) {
                    STATUS_START_INIT -> {
                        flip(VF_INIT)
                    }
                    STATUS_INIT_END -> {
                        if (isFirstLaunch) {
                            Once.markDone(OnceTag.FIRST_LAUNCH)
                        }
                        requestChange()
                    }
                    STATUS_START_REQUEST_CHANGE_END -> {
                        dbItems.value?.let { updateItems(it) }
                    }
                    STATUS_NOT_START -> {
                        if ((HarmonyOsUtil.isHarmonyOs() && !Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.HARMONY_FIRST_INIT))
                            || (!isFirstLaunch && !Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.SHOULD_RELOAD_APP_LIST))) {
                            flip(VF_INIT)
                            initItems()
                            Once.markDone(OnceTag.SHOULD_RELOAD_APP_LIST)
                            Once.markDone(OnceTag.HARMONY_FIRST_INIT)
                        }
                    }
                }
            })
            initProgressLiveData.observe(viewLifecycleOwner, {
                binding.initView.progressIndicator.setProgressCompat(it, true)
            })
            packageChangedLiveData.observe(viewLifecycleOwner) {
                requestChange(true)
            }
        }

        GlobalValues.apply {
            isShowSystemApps.observe(viewLifecycleOwner, {
                if (isListReady) {
                    updateItems(homeViewModel.dbItems.value!!)
                }
            })
            appSortMode.observe(viewLifecycleOwner, { mode ->
                if (isListReady) {
                    homeViewModel.dbItems.value?.let { allDatabaseItems ->
                        val list = allDatabaseItems.toMutableList()

                        when (mode) {
                            Constants.SORT_MODE_DEFAULT -> list.sortWith(
                                compareBy(
                                    { it.abi },
                                    { it.label })
                            )
                            Constants.SORT_MODE_UPDATE_TIME_DESC -> list.sortByDescending { it.lastUpdatedTime }
                            Constants.SORT_MODE_TARGET_API_DESC -> list.sortByDescending { it.targetApi }

                        }
                        updateItems(list)
                    }
                }
            })
            shouldRequestChange.observe(viewLifecycleOwner, { should ->
                if (isListReady && !should) {
                    homeViewModel.dbItems.value?.let { updateItems(it) }
                }
            })
        }
    }

    private fun updateItems(newItems: List<LCItem>, needReturnTop: Boolean = true) {
        Timber.d("updateItems")
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
            Constants.SORT_MODE_TARGET_API_DESC -> filterList.sortByDescending { it.targetApi }
        }

        mAdapter.setDiffNewData(filterList) {
            flip(VF_LIST)

            if (shouReturnTopOfList() && needReturnTop) {
                returnTopOfList()
            }

            menu?.findItem(R.id.search)?.isVisible = true
            isListReady = true
        }
    }

    private fun shouReturnTopOfList(): Boolean {
        return binding.list.canScrollVertically(-1) &&
                (GlobalValues.appSortMode.valueUnsafe == Constants.SORT_MODE_UPDATE_TIME_DESC) &&
                binding.list.scrollState != RecyclerView.SCROLL_STATE_DRAGGING
    }

    private fun returnTopOfList() {
        binding.list.apply {
            if (canScrollVertically(-1)) {
                smoothScrollToPosition(0)
            }
        }
    }

    private fun getSuitableLayoutManager(): RecyclerView.LayoutManager {
        layoutManager = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> LinearLayoutManager(requireContext())
            Configuration.ORIENTATION_LANDSCAPE -> StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            else -> throw IllegalStateException("Wrong orientation at AppListFragment.")
        }
        return layoutManager
    }

    private fun flip(page: Int) {
        Timber.d("flip to $page")
        if (binding.vfContainer.displayedChild != page) {
            binding.vfContainer.displayedChild = page
        }
        if (page == VF_INIT) {
            binding.initView.loadingView.resumeAnimation()
        } else {
            binding.initView.loadingView.pauseAnimation()
        }
    }

    override fun onReturnTop() {
        if (binding.list.canScrollVertically(-1)) {
            returnTopOfList()
        } else {
            flip(VF_LOADING)
            homeViewModel.requestChange(true)
        }
    }
}
