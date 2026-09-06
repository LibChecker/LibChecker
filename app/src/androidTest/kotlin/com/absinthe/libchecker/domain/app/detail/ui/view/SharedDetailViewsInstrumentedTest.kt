package com.absinthe.libchecker.domain.app.detail.ui.view

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.children
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.action.AppInstalledTimeDisplayData
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceBottomSheetDisplay
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitleDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitlePackageSizeData
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotNoDiffMode
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotNoDiffRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotNoDiffBSView
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotPackageChangeView
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotTitleView
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.DexFileOptimizationInfo
import com.google.android.material.card.MaterialCardView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedDetailViewsInstrumentedTest {
  private val instrumentation = InstrumentationRegistry.getInstrumentation()

  @Test
  fun snapshotMessagesKeepGeometryInPageAndSheet() = instrumentation.runOnMainSync {
    val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
    val sheet = SnapshotNoDiffBSView(context)
    val title = SnapshotTitleDisplayData("App", "package", "1", null, "")
    listOf(
      Triple(SnapshotNoDiffMode.New, R.drawable.ic_yes, R.string.snapshot_detail_new_install_title),
      Triple(SnapshotNoDiffMode.Deleted, R.drawable.ic_no, R.string.snapshot_detail_deleted_title)
    ).forEach { (mode, icon, message) ->
      sheet.render(SnapshotNoDiffRenderState(title, mode))
      val page = FrameLayout(context).apply { addView(SnapshotPackageChangeView(context, icon, message)) }
      listOf(page, sheet).forEach { parent ->
        parent.measureAndLayout()
        val state = parent.children.filterIsInstance<SnapshotPackageChangeView>().single()
        val image = state.getChildAt(0)
        val text = state.getChildAt(1) as TextView
        assertEquals(context.getString(message), text.text)
        assertEquals(150.dp, image.width)
        assertEquals(state.width / 8, state.paddingTop)
        assertEquals(state.width / 2f, image.left + image.width / 2f, 0.5f)
        assertEquals(image.bottom + 16.dp, text.top)
        assertEquals(text.bottom + state.paddingBottom, state.height)
      }
    }
  }

  @Test
  fun installSectionsKeepCardBoundsCopyAndHiddenHeight() = instrumentation.runOnMainSync {
    val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
    val sheet = AppInstallSourceBottomSheetView(context)
    sheet.bind(
      AppInstallSourceBottomSheetDisplay(null, null, AppInstalledTimeDisplayData("2024", "2026"), DexFileOptimizationInfo("speed", "install"))
    ) {}
    sheet.measureAndLayout()
    val expandedHeight = sheet.height
    sheet.children.filterIsInstance<AppInstallDetailSectionView>().forEach { section ->
      val title = section.getChildAt(0)
      val card = section.children.filterIsInstance<MaterialCardView>().single()
      assertEquals(section.width - sheet.paddingStart - sheet.paddingEnd, card.width)
      assertEquals(title.bottom + 8.dp, card.top)
      assertEquals(card.bottom, section.height)
      assertTrue(card.isLongClickable)
      assertFalse(card.getChildAt(0).isLongClickable)
      assertTrue(card.performLongClick())
    }
    sheet.bind(AppInstallSourceBottomSheetDisplay(null, null, null, null)) {}
    sheet.measureAndLayout()
    assertTrue(sheet.height < expandedHeight)
    assertTrue(sheet.children.filterIsInstance<AppInstallDetailSectionView>().all { it.visibility == View.GONE })
  }

  @Test
  fun xmlAdapterKeepsSelectableTextAndTrailingRow() = instrumentation.runOnMainSync {
    val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
    val view = XmlBottomSheetView(context)
    view.setText("<manifest />")
    val list = view.children.filterIsInstance<BottomSheetRecyclerView>().single()
    view.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY))
    view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    assertEquals(2, list.adapter!!.itemCount)
    val text = list.getChildAt(0) as TextView
    assertEquals("<manifest />", text.text.toString())
    assertTrue(text.isTextSelectable)
  }

  @Test
  fun snapshotTitleKeepsTextAccessibilityAndCopyPolicyWhenRebound() = instrumentation.runOnMainSync {
    val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
    val title = SnapshotTitleView(context)
    val data = SnapshotTitleDisplayData(
      "App",
      "com.example.app",
      "1 → 2",
      SnapshotTitlePackageSizeData("10 MB → 12 MB", 5),
      "Target: 35 → 37"
    )
    title.render(data)
    title.measureAndLayout()
    assertEquals(data.appName, title.appNameView.text.toString())
    assertEquals(data.packageName, title.packageNameView.text.toString())
    assertEquals(data.versionInfo, title.versionInfoView.text.toString())
    assertEquals(data.packageSize!!.text, title.packageSizeView.text.toString())
    assertEquals(data.apis, title.apisView.text.toString())
    assertEquals(data.appName, title.getChildAt(0).contentDescription)
    val primaryTextViews = listOf(title.appNameView, title.packageNameView, title.versionInfoView)
    assertTrue(primaryTextViews.all { it.isLongClickable })
    title.render(data.copy(packageSize = null, apis = ""))
    assertEquals(View.GONE, title.packageSizeView.visibility)
    assertEquals(View.GONE, title.apisView.visibility)
    title.render(data)
    assertTrue(primaryTextViews.all { it.isLongClickable })
    val sheet = SnapshotNoDiffBSView(context)
    sheet.render(SnapshotNoDiffRenderState(data, SnapshotNoDiffMode.NothingChanged))
    val sheetTitle = sheet.children.filterIsInstance<SnapshotTitleView>().single()
    assertFalse(sheetTitle.appNameView.isLongClickable)
    assertFalse(sheetTitle.packageNameView.isLongClickable)
    assertFalse(sheetTitle.versionInfoView.isLongClickable)
  }

  private val Int.dp: Int get() = (this * instrumentation.targetContext.resources.displayMetrics.density + 0.5f).toInt()

  private fun View.measureAndLayout() {
    measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
    layout(0, 0, measuredWidth, measuredHeight)
  }
}
