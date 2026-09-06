package com.absinthe.libchecker.domain.app.detail.ui

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.constant.AbilityType
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.adapter.LibStringAdapter
import com.absinthe.libchecker.domain.app.detail.ui.impl.AbilityAnalysisFragment
import com.absinthe.libchecker.domain.app.detail.ui.impl.ComponentsAnalysisFragment
import com.absinthe.libchecker.domain.app.detail.ui.impl.DexAnalysisFragment
import com.absinthe.libchecker.domain.app.detail.ui.impl.MetaDataAnalysisFragment
import com.absinthe.libchecker.domain.app.detail.ui.impl.NativeAnalysisFragment
import com.absinthe.libchecker.domain.app.detail.ui.impl.PermissionAnalysisFragment
import com.absinthe.libchecker.domain.app.detail.ui.impl.SignaturesAnalysisFragment
import com.absinthe.libchecker.domain.app.detail.ui.impl.StaticAnalysisFragment
import com.absinthe.libchecker.view.app.EmptyListView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailListInitializationInstrumentedTest {
  @Test
  fun cachedEmptyItemsKeepPageSpecificMessagesAfterInitialization() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val activity = instrumentation.startActivitySync(
      Intent(instrumentation.targetContext, AppDetailActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ) as AppDetailActivity
    try {
      instrumentation.runOnMainSync {
        val state = ViewModelProvider(activity)[DetailViewModel::class.java].contentState
        state.resetAbilities()
        state.abilitiesMap[AbilityType.PAGE].value = emptyList()
        listOf(state.nativeLibItems, state.staticLibItems, state.metaDataItems, state.permissionsItems, state.dexLibItems, state.signaturesLibItems, state.componentsMap[ACTIVITY])
          .forEach { it.value = emptyList() }
      }
      listOf(
        AbilityAnalysisFragment.newInstance(AbilityType.PAGE) to R.string.empty_list,
        NativeAnalysisFragment.newInstance("") to R.string.empty_list,
        StaticAnalysisFragment.newInstance("") to R.string.empty_list,
        MetaDataAnalysisFragment.newInstance("") to R.string.empty_list,
        PermissionAnalysisFragment.newInstance("") to R.string.empty_list,
        ComponentsAnalysisFragment.newInstance("", ACTIVITY) to R.string.empty_list,
        DexAnalysisFragment.newInstance("") to R.string.uncharted_territory,
        SignaturesAnalysisFragment.newInstance("") to R.string.uncharted_territory
      ).forEach { (fragment, message) ->
        instrumentation.runOnMainSync {
          activity.supportFragmentManager.beginTransaction().add(android.R.id.content, fragment).commitNow()
        }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
          val adapter = fragment.getRecyclerView().adapter as LibStringAdapter
          assertFalse(adapter.animationEnable)
          assertTrue(adapter.isStateViewEnable)
          assertEquals(activity.getString(message), (adapter.stateView as EmptyListView).text.text.toString())
          activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()
        }
      }
    } finally {
      instrumentation.runOnMainSync { activity.finish() }
    }
  }
}
