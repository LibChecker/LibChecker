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

  private var state = DetailTabSpec()
  private var tabLayoutMediator: TabLayoutMediator? = null
  private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

  val types: List<Int>
    get() = state.types

  val selectedType: Int?
    get() = state.itemAt(tabLayout.selectedTabPosition)?.type

  fun setup(
    packageName: String,
    isHarmonyMode: Boolean,
    tabSpec: DetailTabSpec
  ) {
    reset()
    state = tabSpec

    viewPager.adapter = object : FragmentStateAdapter(activity) {
      override fun getItemCount(): Int {
        return state.items.size
      }

      override fun createFragment(position: Int): Fragment {
        return createFragment(
          packageName = packageName,
          isHarmonyMode = isHarmonyMode,
          type = state.itemAt(position)?.type ?: NATIVE
        )
      }

      override fun getItemId(position: Int): Long {
        return (state.itemAt(position)?.type ?: NATIVE).toLong()
      }

      override fun containsItem(itemId: Long): Boolean {
        return state.items.any { it.type.toLong() == itemId }
      }
    }
    pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
      override fun onPageSelected(position: Int) {
        super.onPageSelected(position)
        onProcessTooltipTextChanged(getProcessTooltipTextRes(position))
      }
    }.also { viewPager.registerOnPageChangeCallback(it) }

    tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab) {
        val type = state.itemAt(tab.position)?.type ?: return
        onTabSelected(type)
      }

      override fun onTabUnselected(tab: TabLayout.Tab?) {}

      override fun onTabReselected(tab: TabLayout.Tab?) {}
    })

    tabLayoutMediator =
      TabLayoutMediator(tabLayout, viewPager, true, true) { tab, position ->
        tab.text = state.itemAt(position)?.title
      }.also { it.attach() }
  }

  fun insertStaticLibraryTab(title: CharSequence): Boolean {
    val newState = state.withStaticLibraryTab(title)
    if (newState == state) {
      return false
    }
    val insertionPosition = newState.items.indexOfFirst { it.type == STATIC }
    state = newState
    viewPager.adapter?.notifyItemInserted(insertionPosition)
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
    state = DetailTabSpec()
  }

  @StringRes
  private fun getProcessTooltipTextRes(position: Int): Int {
    return if (state.itemAt(position)?.type == NATIVE) {
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
        ComponentsAnalysisFragment.newInstance(packageName, type)
      } else {
        AbilityAnalysisFragment.newInstance(type)
      }
    }
  }
}
