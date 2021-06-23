package com.absinthe.libchecker.ui.fragment.statistics

import android.content.Intent
import android.graphics.Color
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.databinding.FragmentLibReferenceBinding
import com.absinthe.libchecker.extensions.tintHighlightText
import com.absinthe.libchecker.recyclerview.adapter.LibReferenceAdapter
import com.absinthe.libchecker.recyclerview.diff.RefListDiffUtil
import com.absinthe.libchecker.ui.fragment.BaseListControllerFragment
import com.absinthe.libchecker.ui.fragment.detail.LibDetailDialogFragment
import com.absinthe.libchecker.ui.main.EXTRA_REF_NAME
import com.absinthe.libchecker.ui.main.EXTRA_REF_TYPE
import com.absinthe.libchecker.ui.main.LibReferenceActivity
import com.absinthe.libchecker.ui.main.MainActivity
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.doOnMainThreadIdle
import com.absinthe.libchecker.view.detail.EmptyListView
import com.absinthe.libchecker.view.statistics.LibReferenceItemView
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import rikka.widget.borderview.BorderView

class LibReferenceFragment : BaseListControllerFragment<FragmentLibReferenceBinding>(R.layout.fragment_lib_reference), SearchView.OnQueryTextListener {

    private val adapter = LibReferenceAdapter()
    private var popup: PopupMenu? = null
    private var category = GlobalValues.currentLibRefType
    private lateinit var layoutManager: LinearLayoutManager

    override fun initBinding(view: View): FragmentLibReferenceBinding = FragmentLibReferenceBinding.bind(view)

    override fun init() {
        setHasOptionsMenu(true)

        layoutManager = LinearLayoutManager(requireContext())
        binding.apply {
            list.apply {
                adapter = this@LibReferenceFragment.adapter
                layoutManager = this@LibReferenceFragment.layoutManager
                borderDelegate = borderViewDelegate
                borderVisibilityChangedListener =
                    BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
                        (requireActivity() as MainActivity).appBar?.setRaised(!top)
                    }
                FastScrollerBuilder(this).useMd2Style().build()
            }
            vfContainer.apply {
                setInAnimation(activity, R.anim.anim_fade_in)
                setOutAnimation(activity, R.anim.anim_fade_out)
            }
            lottie.apply {
                imageAssetsFolder = "/"

                val assetName = when (GlobalValues.season) {
                    SPRING -> "lib_reference_spring.json"
                    SUMMER -> "lib_reference_summer.json"
                    AUTUMN -> "lib_reference_autumn.json"
                    WINTER -> "lib_reference_winter.json"
                    else -> "lib_reference_summer.json"
                }

                setAnimation(assetName)
            }
        }

        adapter.apply {
            setDiffCallback(RefListDiffUtil())
            setOnItemClickListener { _, view, position ->
                if (AntiShakeUtils.isInvalidClick(view)) {
                    return@setOnItemClickListener
                }

                val intent = Intent(requireContext(), LibReferenceActivity::class.java).apply {
                    val item = this@LibReferenceFragment.adapter.data[position]
                    putExtra(EXTRA_REF_NAME, item.libName)
                    putExtra(EXTRA_REF_TYPE, item.type)
                }
                startActivity(intent)
            }
            setOnItemChildClickListener { _, view, position ->
                if (view.id == android.R.id.icon) {
                    val ref = this@LibReferenceFragment.adapter.getItem(position)
                    val name = ref.libName
                    val regexName = LCAppUtils.findRuleRegex(name, ref.type)?.regexName
                    LibDetailDialogFragment.newInstance(name, ref.type, regexName).show(childFragmentManager, tag)
                }
            }
            setEmptyView(EmptyListView(requireContext()))
        }

        homeViewModel.apply {
            libReference.observe(viewLifecycleOwner, {
                if (it == null) {
                    return@observe
                }
                adapter.setList(it)

                if (binding.vfContainer.displayedChild == 0) {
                    binding.vfContainer.displayedChild = 1
                    binding.lottie.pauseAnimation()
                }
                isListReady = true
                menu?.findItem(R.id.search)?.isVisible = true
            })
            packageChangedLiveData.observe(viewLifecycleOwner) {
                computeRef()
            }
        }
        GlobalValues.isShowSystemApps.observe(viewLifecycleOwner, {
            computeRef()
        })
        GlobalValues.libReferenceThreshold.observe(viewLifecycleOwner, {
            homeViewModel.refreshRef()
        })

        AppItemRepository.allApplicationInfoItems.observe(viewLifecycleOwner, {
            AppItemRepository.shouldRefreshAppList = true

            if (adapter.data.isEmpty()) {
                computeRef()
            }
        })
    }

    override fun onPause() {
        super.onPause()
        popup?.dismiss()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.lib_ref_menu, menu)
        this.menu = menu

        val searchView = SearchView(requireContext()).apply {
            setIconifiedByDefault(false)
            setOnQueryTextListener(this@LibReferenceFragment)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.filter) {
            popup = PopupMenu(requireContext(), requireActivity().findViewById(R.id.filter)).apply {
                menuInflater.inflate(R.menu.lib_ref_type_menu, menu)

                menu.findItem(R.id.ref_category_dex)?.apply {
                    isVisible = BuildConfig.DEBUG || GlobalValues.debugMode
                }
                menu.findItem(R.id.ref_category_not_marked)?.apply {
                    isVisible = BuildConfig.DEBUG || GlobalValues.debugMode
                }

                menu[getMenuIndex(category)].isChecked = true
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.ref_category_all -> doSaveLibRefType(ALL)
                        R.id.ref_category_native -> doSaveLibRefType(NATIVE)
                        R.id.ref_category_service -> doSaveLibRefType(SERVICE)
                        R.id.ref_category_activity -> doSaveLibRefType(ACTIVITY)
                        R.id.ref_category_br -> doSaveLibRefType(RECEIVER)
                        R.id.ref_category_cp -> doSaveLibRefType(PROVIDER)
                        R.id.ref_category_dex -> doSaveLibRefType(DEX)
                        R.id.ref_category_not_marked -> doSaveLibRefType(NOT_MARKED)
                    }
                    computeRef()
                    true
                }
                setOnDismissListener {
                    popup = null
                }
            }
            popup?.show()
        }
        Analytics.trackEvent(
            Constants.Event.LIB_REFERENCE_FILTER_TYPE, EventProperties().set(
                "Type",
                category.toLong()
            )
        )
        return super.onOptionsItemSelected(item)
    }

    private fun doSaveLibRefType(@LibType type: Int) {
        category = type
        GlobalValues.currentLibRefType = type
    }

    private fun computeRef() {
        if (binding.vfContainer.displayedChild == 1) {
            binding.vfContainer.displayedChild = 0
            binding.lottie.resumeAnimation()
        }

        homeViewModel.cancelComputingLibReference()
        homeViewModel.computeLibReference(category)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        homeViewModel.libReference.value?.let { list ->
            val filter = list.filter {
                it.libName.contains(newText, ignoreCase = true) || it.chip?.name?.contains(
                    newText,
                    ignoreCase = true
                ) ?: false
            }
            adapter.highlightText = newText
            adapter.setDiffNewData(filter.toMutableList())
            doOnMainThreadIdle({
                val first = layoutManager.findFirstVisibleItemPosition()
                val last = layoutManager.findLastVisibleItemPosition() + 3

                for (i in first..last) {
                    (layoutManager.getChildAt(i) as? LibReferenceItemView)?.container?.apply {
                        labelName.apply { tintHighlightText(newText, text.toString()) }
                        libName.apply { tintHighlightText(newText, text.toString()) }
                    }
                }
            })

            if (newText.equals("Easter Egg", true)) {
                Toasty.show(requireContext(), "🥚")
                Analytics.trackEvent(Constants.Event.EASTER_EGG, EventProperties().set("EASTER_EGG", "Lib Reference Search"))
            }
        }
        return false
    }

    override fun onReturnTop() {
        binding.list.apply {
            if (canScrollVertically(-1)) {
                smoothScrollToPosition(0)
            }
        }
    }

    private fun getMenuIndex(@LibType type: Int) = when (type) {
        ALL -> 0
        NATIVE -> 1
        SERVICE -> 2
        ACTIVITY -> 3
        RECEIVER -> 4
        PROVIDER -> 5
        DEX -> 6
        else -> 0
    }
}