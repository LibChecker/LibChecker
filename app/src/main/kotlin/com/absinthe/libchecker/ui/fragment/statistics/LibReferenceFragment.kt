package com.absinthe.libchecker.ui.fragment.statistics

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.bean.LibReference
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.databinding.FragmentLibReferenceBinding
import com.absinthe.libchecker.extensions.addPaddingBottom
import com.absinthe.libchecker.recyclerview.adapter.LibReferenceAdapter
import com.absinthe.libchecker.recyclerview.diff.RefListDiffUtil
import com.absinthe.libchecker.ui.fragment.BaseListControllerFragment
import com.absinthe.libchecker.ui.fragment.detail.LibDetailDialogFragment
import com.absinthe.libchecker.ui.main.EXTRA_NAME
import com.absinthe.libchecker.ui.main.EXTRA_TYPE
import com.absinthe.libchecker.ui.main.LibReferenceActivity
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.viewmodel.AppViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.libraries.utils.utils.UiUtils
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import me.zhanghai.android.fastscroll.FastScrollerBuilder

class LibReferenceFragment : BaseListControllerFragment<FragmentLibReferenceBinding>(R.layout.fragment_lib_reference), SearchView.OnQueryTextListener {

    private val viewModel by activityViewModels<AppViewModel>()
    private val adapter = LibReferenceAdapter()

    private var isListReady = false
    private var menu: Menu? = null
    private var category = NATIVE

    override fun initBinding(view: View): FragmentLibReferenceBinding = FragmentLibReferenceBinding.bind(view)

    override fun init() {
        setHasOptionsMenu(true)

        binding.apply {
            rvList.apply {
                adapter = this@LibReferenceFragment.adapter
                addPaddingBottom(UiUtils.getNavBarHeight(requireActivity().contentResolver))
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
                enableMergePathsForKitKatAndAbove(true)
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
                    putExtra(EXTRA_NAME, item.libName)
                    putExtra(EXTRA_TYPE, item.type)
                }
                startActivity(intent)
            }
            setOnItemChildClickListener { _, view, position ->
                if (view.id == R.id.iv_icon) {
                    val ref = this@LibReferenceFragment.adapter.getItem(position)
                    val name = ref.libName
                    val regexName = LCAppUtils.findRuleRegex(name, ref.type)?.regexName
                    LibDetailDialogFragment.newInstance(name, ref.type, regexName).show(childFragmentManager, tag)
                }
            }
        }

        viewModel.apply {
            libReference.observe(viewLifecycleOwner, {
                adapter.setList(it)

                if (binding.vfContainer.displayedChild == 0) {
                    binding.vfContainer.displayedChild = 1
                    binding.lottie.pauseAnimation()
                }
                isListReady = true
                menu?.findItem(R.id.search)?.isVisible = true
            })
        }
        GlobalValues.isShowSystemApps.observe(viewLifecycleOwner, {
            computeRef()
        })
        GlobalValues.libReferenceThreshold.observe(viewLifecycleOwner, {
            viewModel.refreshRef()
        })

        AppItemRepository.allApplicationInfoItems.observe(viewLifecycleOwner, {
            AppItemRepository.shouldRefreshAppList = true

            if (adapter.data.isEmpty()) {
                computeRef()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (savedInstanceState != null) {
            savedInstanceState.getParcelableArrayList<LibReference>(
                EXTRA_ITEM_LIST
            )?.toList()?.let {
                adapter.setList(it)
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(EXTRA_ITEM_LIST, ArrayList(adapter.data))
        super.onSaveInstanceState(outState)
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

        menu.findItem(R.id.ref_category_dex).apply {
            isVisible = BuildConfig.DEBUG
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.ref_category_all -> category = ALL
            R.id.ref_category_native -> category = NATIVE
            R.id.ref_category_service -> category = SERVICE
            R.id.ref_category_activity -> category = ACTIVITY
            R.id.ref_category_br -> category = RECEIVER
            R.id.ref_category_cp -> category = PROVIDER
            R.id.ref_category_dex -> category = DEX
        }

        if (item.itemId != R.id.search) {
            computeRef()
        }
        Analytics.trackEvent(
            Constants.Event.LIB_REFERENCE_FILTER_TYPE, EventProperties().set(
                "Type",
                category.toLong()
            )
        )
        return super.onOptionsItemSelected(item)
    }

    private fun computeRef() {
        if (binding.vfContainer.displayedChild == 1) {
            binding.vfContainer.displayedChild = 0
            binding.lottie.resumeAnimation()
        }

        viewModel.computeLibReference(category)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        viewModel.libReference.value?.let { list ->
            val filter = list.filter {
                it.libName.contains(newText, ignoreCase = true) || it.chip?.name?.contains(
                    newText,
                    ignoreCase = true
                ) ?: false
            }
            adapter.setDiffNewData(filter.toMutableList())

            if (newText.equals("Easter Egg", true)) {
                Toasty.show(requireContext(), "🥚")
                Analytics.trackEvent(Constants.Event.EASTER_EGG, EventProperties().set("EASTER_EGG", "Lib Reference Search"))
            }
        }
        return false
    }

    override fun onReturnTop() {
        binding.rvList.apply {
            if (canScrollVertically(-1)) {
                smoothScrollToPosition(0)
            }
        }
    }
}