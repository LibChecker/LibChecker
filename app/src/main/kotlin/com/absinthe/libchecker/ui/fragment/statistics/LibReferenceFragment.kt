package com.absinthe.libchecker.ui.fragment.statistics

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Space
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.ALL
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.NOT_MARKED
import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.SHARED_UID
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.bean.LibReference
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.FragmentLibReferenceBinding
import com.absinthe.libchecker.recyclerview.adapter.statistics.LibReferenceAdapter
import com.absinthe.libchecker.recyclerview.diff.RefListDiffUtil
import com.absinthe.libchecker.ui.fragment.BaseListControllerFragment
import com.absinthe.libchecker.ui.main.ChartActivity
import com.absinthe.libchecker.ui.main.EXTRA_REF_LIST
import com.absinthe.libchecker.ui.main.EXTRA_REF_NAME
import com.absinthe.libchecker.ui.main.EXTRA_REF_TYPE
import com.absinthe.libchecker.ui.main.INavViewContainer
import com.absinthe.libchecker.ui.main.LibReferenceActivity
import com.absinthe.libchecker.utils.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libchecker.view.detail.EmptyListView
import com.absinthe.libchecker.view.drawable.RoundedRectDrawable
import com.absinthe.libchecker.viewmodel.HomeViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.cascade.CascadePopupMenu
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import rikka.widget.borderview.BorderView
import java.lang.ref.WeakReference

const val VF_LOADING = 0
const val VF_LIST = 1

class LibReferenceFragment :
  BaseListControllerFragment<FragmentLibReferenceBinding>(),
  SearchView.OnQueryTextListener {

  private val refAdapter by unsafeLazy { LibReferenceAdapter(lifecycleScope) }
  private var popup: CascadePopupMenu? = null
  private var delayShowNavigationJob: Job? = null
  private var category = GlobalValues.currentLibRefType
  private var firstScrollFlag = false
  private var keyword: String = ""
  private var searchUpdateJob: Job? = null

  override fun init() {
    val context = (context as? BaseActivity<*>) ?: return

    binding.apply {
      list.apply {
        adapter = refAdapter
        layoutManager = LinearLayoutManager(context)
        borderDelegate = borderViewDelegate
        borderVisibilityChangedListener =
          BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
            context.appBar?.setRaised(!top)
          }
        FastScrollerBuilder(this).useMd2Style().build()
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
          override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dx == 0 && dy == 0) {
              // scrolled by dragging scrolling bar
              if (!firstScrollFlag) {
                firstScrollFlag = true
                return
              }
              if (delayShowNavigationJob?.isActive == true) {
                delayShowNavigationJob?.cancel()
                delayShowNavigationJob = null
              }
              if (isListCanScroll(refAdapter.data.size)) {
                (activity as? INavViewContainer)?.hideNavigationView()
              }

              val position = when (layoutManager) {
                is LinearLayoutManager -> {
                  (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                }
                is StaggeredGridLayoutManager -> {
                  val counts = IntArray(4)
                  (layoutManager as StaggeredGridLayoutManager).findLastVisibleItemPositions(counts)
                  counts[0]
                }
                else -> {
                  0
                }
              }
              if (position < refAdapter.itemCount - 1) {
                delayShowNavigationJob = lifecycleScope.launch(Dispatchers.Default) {
                  delay(400)
                  withContext(Dispatchers.Main) {
                    (activity as? INavViewContainer)?.showNavigationView()
                  }
                }.also {
                  it.start()
                }
              }
            }
          }
        })
      }
      vfContainer.apply {
        setInAnimation(activity, R.anim.anim_fade_in)
        setOutAnimation(activity, R.anim.anim_fade_out)
      }
    }

    refAdapter.apply {
      animationEnable = true
      setDiffCallback(RefListDiffUtil())
      setOnItemClickListener { _, view, position ->
        if (AntiShakeUtils.isInvalidClick(view)) {
          return@setOnItemClickListener
        }

        val item = refAdapter.data[position] as? LibReference ?: return@setOnItemClickListener
        val intent = Intent(context, LibReferenceActivity::class.java)
          .putExtra(EXTRA_REF_NAME, item.libName)
          .putExtra(EXTRA_REF_TYPE, item.type)
          .putExtra(EXTRA_REF_LIST, item.referredList.toTypedArray())
        startActivity(intent)
      }
      setEmptyView(
        EmptyListView(context).apply {
          layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
          )
        }
      )
      setFooterView(
        Space(context).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            96.dp
          )
        }
      )
    }

    homeViewModel.apply {
      lifecycleScope.launchWhenStarted {
        effect.collect {
          when (it) {
            is HomeViewModel.Effect.PackageChanged -> {
              computeRef(false)
            }
            else -> {}
          }
        }
      }
      libReference.observe(viewLifecycleOwner) {
        if (it == null) {
          return@observe
        }
        refAdapter.setList(it)

        flip(VF_LIST)
        isListReady = true
      }
    }
    GlobalValues.isShowSystemApps.observe(viewLifecycleOwner) {
      if (homeViewModel.libRefSystemApps == null || homeViewModel.libRefSystemApps != it) {
        computeRef(true)
        homeViewModel.libRefSystemApps = it
      }
    }
    GlobalValues.libReferenceThresholdLiveData.observe(viewLifecycleOwner) {
      homeViewModel.refreshRef()
    }

    lifecycleScope.launch {
      if (refAdapter.data.isEmpty()) {
        if (homeViewModel.libRefType == null) {
          computeRef(true)
          homeViewModel.libRefType = category
        }
      }
    }
  }

  override fun onPause() {
    super.onPause()
    popup?.dismiss()
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.lib_ref_menu, menu)
    this.menu = menu

    val context = (context as? BaseActivity<*>) ?: return
    val searchView = SearchView(context).apply {
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
    val context = (context as? BaseActivity<*>) ?: return false
    val contextRef = WeakReference(context)
    if (item.itemId == R.id.filter) {
      val color = context.getColorByAttr(com.google.android.material.R.attr.colorSurface)
      val styler = CascadePopupMenu.Styler(
        background = {
          RoundedRectDrawable(color, radius = 6.dp.toFloat())
        }
      )
      popup = CascadePopupMenu(
        contextRef.get()!!,
        context.findViewById(R.id.filter),
        defStyleAttr = R.style.Widget_LC_PopupMenu,
        styler = styler
      ).apply {
        menu.also {
          it.add(R.string.ref_category_all).initMenu(ALL)
          it.add(R.string.ref_category_native).initMenu(NATIVE)
          it.addSubMenu(R.string.submenu_title_component).also { componentMenu ->
            componentMenu.setHeaderTitle(R.string.submenu_title_component)
            componentMenu.add(R.string.ref_category_service).initMenu(SERVICE)
            componentMenu.add(R.string.ref_category_activity).initMenu(ACTIVITY)
            componentMenu.add(R.string.ref_category_br).initMenu(RECEIVER)
            componentMenu.add(R.string.ref_category_cp).initMenu(PROVIDER)
          }
          it.addSubMenu(R.string.submenu_title_manifest).also { manifestMenu ->
            manifestMenu.setHeaderTitle(R.string.submenu_title_manifest)
            manifestMenu.add(R.string.ref_category_perm).initMenu(PERMISSION)
            manifestMenu.add(R.string.ref_category_metadata).initMenu(METADATA)
          }
          it.add(R.string.ref_category_package).initMenu(PACKAGE)
          it.add(R.string.ref_category_shared_uid).initMenu(SHARED_UID)
          it.add(R.string.ref_category_dex).also { dexMenu ->
            dexMenu.onlyVisibleInDebugMode()
            dexMenu.initMenu(DEX)
          }
          it.add(R.string.not_marked_lib).also { notMarkedMenu ->
            notMarkedMenu.onlyVisibleInDebugMode()
            notMarkedMenu.initMenu(NOT_MARKED)
          }
        }
      }
      popup?.show()
    } else if (item.itemId == R.id.chart) {
      startActivity(Intent(context, ChartActivity::class.java))
    }

    return super.onOptionsItemSelected(item)
  }

  private fun MenuItem.initMenu(@LibType type: Int) {
    this.title?.let {
      if (GlobalValues.currentLibRefType == type) {
        val title = SpannableStringBuilder(it)
        title.setSpan(
          StyleSpan(Typeface.BOLD),
          0,
          it.length,
          Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        this.title = title
      }
    }
    this.setOnMenuItemClickListener {
      doSaveLibRefType(type)
    }
  }

  private fun MenuItem.onlyVisibleInDebugMode() {
    isVisible = BuildConfig.DEBUG || GlobalValues.debugMode
  }

  private fun doSaveLibRefType(@LibType type: Int): Boolean {
    category = type
    GlobalValues.currentLibRefType = type
    computeRef(true)
    Analytics.trackEvent(
      Constants.Event.LIB_REFERENCE_FILTER_TYPE,
      EventProperties().set(
        "Type",
        category.toLong()
      )
    )
    return true
  }

  private fun computeRef(needShowLoading: Boolean) {
    isListReady = false
    if (needShowLoading) {
      flip(VF_LOADING)
    }
    homeViewModel.cancelComputingLibReference()
    homeViewModel.computeLibReference(category)
  }

  override fun onQueryTextSubmit(query: String?): Boolean {
    return false
  }

  override fun onQueryTextChange(newText: String): Boolean {
    if (keyword != newText) {
      keyword = newText

      searchUpdateJob?.cancel()
      searchUpdateJob = lifecycleScope.launch(Dispatchers.IO) {
        homeViewModel.libReference.value?.let { list ->
          val filter = list.filter {
            it.libName.contains(newText, ignoreCase = true) || it.chip?.name?.contains(
              newText,
              ignoreCase = true
            ) ?: false
          }
          LibReferenceAdapter.highlightText = newText

          withContext(Dispatchers.Main) {
            (activity as? INavViewContainer)?.showProgressBar()
            refAdapter.setDiffNewData(filter.toMutableList()) {
              doOnMainThreadIdle {
                //noinspection NotifyDataSetChanged
                refAdapter.notifyDataSetChanged()
              }
              doOnMainThreadIdle {
                (activity as? INavViewContainer)?.hideProgressBar()
              }
            }
          }

          if (newText.equals("Easter Egg", true)) {
            context?.showToast("🥚")
            Analytics.trackEvent(
              Constants.Event.EASTER_EGG,
              EventProperties().set("EASTER_EGG", "Lib Reference Search")
            )
          }
        }
      }.also {
        it.start()
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

  override fun getSuitableLayoutManager(): RecyclerView.LayoutManager? = binding.list.layoutManager

  private fun flip(child: Int) {
    val context = (context as? BaseActivity<*>) ?: return
    allowRefreshing = child == VF_LIST
    if (binding.vfContainer.displayedChild == child) {
      return
    }
    if (child == VF_LOADING) {
      menu?.findItem(R.id.search)?.isVisible = false
      binding.loadingView.loadingView.resumeAnimation()
      context.appBar?.setRaised(false)
    } else {
      menu?.findItem(R.id.search)?.isVisible = true
      binding.loadingView.loadingView.pauseAnimation()
      binding.list.scrollToPosition(0)
    }

    binding.vfContainer.displayedChild = child
  }
}
