package com.absinthe.libchecker.data.rules

import android.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailBottomSheetState
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailContentDisplay
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailHeaderDisplay
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailLocaleDisplay
import com.absinthe.libchecker.domain.app.detail.presentation.DetailContentState
import com.absinthe.libchecker.domain.app.detail.ui.view.LibDetailBottomSheetView
import com.absinthe.libchecker.utils.SPUtils
import com.google.android.material.tabs.TabLayout
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuleLanguageInstrumentedTest {
  @Test fun bindingFallbackDoesNotSaveLanguageButUserSelectionDoes() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val choices = mutableListOf<String>()
      val view = LibDetailBottomSheetView(ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme), onLocaleSelected = choices::add)
      view.bind(
        LibraryDetailBottomSheetState.Content(
          "test",
          LibraryDetailHeaderDisplay(com.absinthe.lc.rulesbundle.R.drawable.ic_sdk_placeholder, true),
          LibraryDetailContentDisplay(
            listOf(
              LibraryDetailLocaleDisplay("zh-Hans", "中文", emptyList()),
              LibraryDetailLocaleDisplay("en", "English", emptyList())
            ),
            "zh-Hans"
          )
        )
      )
      assertEquals(emptyList<String>(), choices)
      // RecyclerView has not attached its header in this isolated view test.
      val tabs = LibDetailBottomSheetView::class.java.getDeclaredField("tabLayout").let {
        it.isAccessible = true
        it.get(view) as TabLayout
      }
      tabs.selectTab(tabs.getTabAt(1))
      tabs.selectTab(tabs.getTabAt(0))
      tabs.selectTab(tabs.getTabAt(0))
      assertEquals(listOf("en", "zh-Hans", "zh-Hans"), choices)
    }
  }

  @Test fun defaultsToEnglishAndRefreshesExistingLabelsAndNativeCache() = runBlocking {
    val saved = SPUtils.sp.getString(Constants.PREF_RULE_LANGUAGE, null)
    val systemLocale = Locale.getDefault()
    try {
      SPUtils.sp.edit().remove(Constants.PREF_RULE_LANGUAGE).commit()
      Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
      assertEquals("en", GlobalValues.preferredRuleLanguage)
      val state = DetailContentState()
      val english = requireNotNull(RulesRepository.getRule("libxguardian.so", NATIVE, true))
      assertEquals("Xinge Push", english.label)
      val items = listOf(LibStringItemChip(LibStringItem("libxguardian.so"), english))
      state.nativeLibItems.value = items
      state.cacheNativeLibItems("arm64-v8a", true, items)
      for ((language, label) in listOf("zh-Hans" to "信鸽推送", "ja" to "Xinge Push", "zh_Hant" to "信鸽推送", "en" to "Xinge Push")) {
        GlobalValues.preferredRuleLanguage = language
        state.refreshRuleLabels()
        assertEquals(language, SPUtils.sp.getString(Constants.PREF_RULE_LANGUAGE, null))
        assertEquals(label, state.nativeLibItems.value!!.single().rule!!.label)
        assertEquals(label, RulesRepository.getRule("libxguardian.so", NATIVE, true)!!.label)
        assertNull(state.cachedNativeLibItems("arm64-v8a", true))
      }
    } finally {
      Locale.setDefault(systemLocale)
      GlobalValues.preferredRuleLanguage = saved ?: "en"
      if (saved == null) SPUtils.sp.edit().remove(Constants.PREF_RULE_LANGUAGE).commit()
    }
  }
}
