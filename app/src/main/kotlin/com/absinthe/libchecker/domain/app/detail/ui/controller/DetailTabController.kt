package com.absinthe.libchecker.domain.app.detail.ui.controller

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.SIGNATURES
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.domain.app.detail.ui.DetailTabSpec
import com.absinthe.libchecker.domain.app.detail.ui.impl.AbilityAnalysisFragment
import com.absinthe.libchecker.domain.app.detail.ui.impl.ComponentsAnalysisFragment
import com.absinthe.libchecker.domain.app.detail.ui.impl.MetaDataAnalysisFragment
import com.absinthe.libchecker.domain.app.detail.ui.impl.NativeAnalysisFragment
import com.absinthe.libchecker.domain.app.detail.ui.impl.PermissionAnalysisFragment
import com.absinthe.libchecker.domain.app.detail.ui.impl.SignaturesAnalysisFragment
import com.absinthe.libchecker.domain.app.detail.ui.impl.StaticAnalysisFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class DetailTabController(
  private val activity: FragmentActivity,
  private val viewPager: ViewPager2,
  private val tabLayout: TabLayout,
  private val onTabSelected: (Int) -> Unit,
  private val onProcessTooltipTextChanged: (Int) -> Unit
) {

  private val mutableTypes = mutableListOf<Int>()
  private val titles = mutableListOf<CharSequence>()
  private var tabLayoutMediator: TabLayoutMediator? = null
  private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

  val types: List<Int>
    get() = mutableTypes

  val selectedType: Int?
    get() = mutableTypes.getOrNull(tabLayout.selectedTabPosition)

  fun setup(
    packageName: String,
    isHarmonyMode: Boolean,
    tabSpec: DetailTabSpec
  ) {
    reset()
    mutableTypes.addAll(tabSpec.types)
    titles.addAll(tabSpec.titles)

    viewPager.adapter = object : FragmentStateAdapter(activity) {
      override fun getItemCount(): Int {
        return mutableTypes.size
      }

      override fun createFragment(position: Int): Fragment {
        return createFragment(
          packageName = packageName,
          isHarmonyMode = isHarmonyMode,
          type = mutableTypes.getOrElse(position) { NATIVE }
        )
      }

      override fun getItemId(position: Int): Long {
        return mutableTypes.getOrElse(position) { NATIVE }.toLong()
      }

      override fun containsItem(itemId: Long): Boolean {
        return mutableTypes.any { it.toLong() == itemId }
      }
    }
    pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
      override fun onPageSelected(position: Int) {
        super.onPageSelected(position)
        onProcessTooltipTextChanged(getProcessTooltipTextRes(position))
      }
    }.also { viewPager.registerOnPageChangeCallback(it) }

    tabLayout.removeAllTabs()
    titles.forEach {
      tabLayout.addTab(tabLayout.newTab().apply { text = it })
    }
    tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab) {
        val type = mutableTypes.getOrNull(tab.position) ?: return
        onTabSelected(type)
      }

      override fun onTabUnselected(tab: TabLayout.Tab?) {}

      override fun onTabReselected(tab: TabLayout.Tab?) {}
    })

    tabLayoutMediator =
      TabLayoutMediator(tabLayout, viewPager, true, false) { tab, position ->
        tab.text = titles.getOrNull(position)
      }.also { it.attach() }
  }

  fun insertStaticLibraryTab(title: CharSequence): Boolean {
    if (STATIC in mutableTypes) {
      return false
    }
    mutableTypes.add(1, STATIC)
    titles.add(1, title)
    viewPager.adapter?.notifyItemInserted(1)
    return true
  }

  fun reset() {
    tabLayoutMediator?.detach()
    tabLayoutMediator = null
    pageChangeCallback?.let { viewPager.unregisterOnPageChangeCallback(it) }
    pageChangeCallback = null
    viewPager.adapter = null
    tabLayout.clearOnTabSelectedListeners()
    tabLayout.removeAllTabs()
    mutableTypes.clear()
    titles.clear()
  }

  @StringRes
  private fun getProcessTooltipTextRes(position: Int): Int {
    return if (mutableTypes.getOrNull(position) == NATIVE) {
      R.string.menu_split
    } else {
      R.string.menu_process
    }
  }

  private fun createFragment(
    packageName: String,
    isHarmonyMode: Boolean,
    type: Int
  ): Fragment {
    return when (type) {
      NATIVE -> NativeAnalysisFragment.newInstance(packageName)

      STATIC -> StaticAnalysisFragment.newInstance(packageName)

      PERMISSION -> PermissionAnalysisFragment.newInstance(packageName)

      METADATA -> MetaDataAnalysisFragment.newInstance(packageName)

      // DEX -> DexAnalysisFragment.newInstance(packageName)

      SIGNATURES -> SignaturesAnalysisFragment.newInstance(packageName)

      else -> if (!isHarmonyMode) {
        ComponentsAnalysisFragment.newInstance(type)
      } else {
        AbilityAnalysisFragment.newInstance(type)
      }
    }
  }
}
