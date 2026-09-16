package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailRuleChipDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailStatusCount
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitleDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitlePackageSizeData
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.SnapshotDetailAdapter
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.CHANGED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import com.google.android.material.chip.Chip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegacySnapshotDetailInstrumentedTest {
  @Test
  fun sectionArrowStaysCenteredAcrossQuarterTurn() = onMain { context ->
    val section = LegacySnapshotDetailSectionView(context)
    measure(section, (360 * context.resources.displayMetrics.density).toInt())
    val arrow = descendants(section).filterIsInstance<ImageView>().single()
    listOf(0f, 90f).forEach { rotation ->
      val bitmap = Bitmap.createBitmap(arrow.width, arrow.height, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)
      canvas.rotate(rotation, arrow.pivotX, arrow.pivotY)
      arrow.draw(canvas)
      val xs = (0 until bitmap.width).filter { x ->
        (0 until bitmap.height).any { y -> Color.alpha(bitmap.getPixel(x, y)) >= 128 }
      }
      val ys = (0 until bitmap.height).filter { y ->
        (0 until bitmap.width).any { x -> Color.alpha(bitmap.getPixel(x, y)) >= 128 }
      }
      assertEquals(arrow.width / 2f, (xs.first() + xs.last() + 1) / 2f, 1f)
      assertEquals(arrow.height / 2f, (ys.first() + ys.last() + 1) / 2f, 1f)
      bitmap.recycle()
    }
  }

  @Test
  fun rippleMaskMatchesCardBoundsAndSectionHasNoFill() = onMain { context ->
    val section = LegacySnapshotDetailSectionView(context)
    assertTrue(section.background == null)
    assertTrue(section.children.all { it.background == null })
    val itemView = LegacySnapshotDetailItemView(context).apply { render(display(NATIVE, CHANGED)) }
    listOf(section, itemView).forEach { view ->
      measure(view, (360 * context.resources.displayMetrics.density).toInt())
      val ripple = view.foreground as RippleDrawable
      ripple.setBounds(0, 0, view.width, view.height)
      val mask = ripple.findDrawableByLayerId(android.R.id.mask)
      val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
      mask.draw(Canvas(bitmap))
      val card = view.getChildAt(0)
      assertEquals(0, Color.alpha(bitmap.getPixel(card.left - 1, card.top + card.height / 2)))
      assertEquals(0, Color.alpha(bitmap.getPixel(card.left, card.top)))
      assertEquals(255, Color.alpha(bitmap.getPixel(card.left + card.width / 2, card.top + card.height / 2)))
      assertEquals(0, Color.alpha(bitmap.getPixel(card.right, card.top + card.height / 2)))
      card.background?.let { background ->
        val fill = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        background.setBounds(card.left, card.top, card.right, card.bottom)
        background.draw(Canvas(fill))
        for (y in 0 until card.height) {
          for (x in 0 until card.width) {
            assertEquals(Color.alpha(fill.getPixel(card.left + x, card.top + y)), Color.alpha(bitmap.getPixel(card.left + x, card.top + y)))
          }
        }
        fill.recycle()
      }
      bitmap.recycle()
    }
  }

  @Test
  fun bothStylesPreserveAllRowsReportsAndExpansion() = onMain { context ->
    val sections = listOf(NATIVE, SERVICE, METADATA, DEX).map { type ->
      val items = listOf(ADDED, REMOVED, CHANGED, MOVED).map { status -> display(type, status) }
      SnapshotDetailSection(type, "Section", "[Section]\n", "Expanded", "Collapsed", items, items.map { SnapshotDetailStatusCount(it.item.diffType, 1, "1", "Status", it.status) })
    }
    val modern = SnapshotDetailAdapter()
    val legacy = SnapshotDetailAdapter(legacyDetail = true)
    listOf(modern, legacy).forEach { it.submitSections(sections) }
    assertEquals(modern.data, legacy.data)
    assertEquals(modern.reportText(), legacy.reportText())
    val parent = FrameLayout(context)
    listOf(modern, legacy).forEach { adapter ->
      adapter.data.indices.forEach { position ->
        val holder = adapter.createViewHolder(parent, adapter.getItemViewType(position))
        adapter.bindViewHolder(holder, position)
        assertTrue(holder.itemView.contentDescription.isNotBlank())
      }
      adapter.toggleSectionAt(0)
    }
    assertEquals(modern.data, legacy.data)
    assertEquals(modern.reportText(), legacy.reportText())
    listOf(modern, legacy).forEach { it.toggleSectionAt(0) }
    assertEquals(modern.data, legacy.data)
  }

  @Test
  fun recycledLegacyCardClearsExtraAndChipAndKeepsLongTextWithinBounds() = onMain { context ->
    val view = LegacySnapshotDetailItemView(context)
    val data = display(SERVICE, MOVED).copy(
      ruleChip = SnapshotDetailRuleChipDisplayData("Rule", R.drawable.ic_add, null, true, false)
    )
    view.render(data)
    var clicks = 0
    view.setChipOnClickListener { clicks++ }
    val chip = descendants(view).filterIsInstance<Chip>().single()
    chip.performClick()
    assertEquals(1, clicks)
    assertEquals(data.description, view.contentDescription)
    val texts = descendants(view).filterIsInstance<TextView>()
    assertEquals(Typeface.create("sans-serif-medium", Typeface.NORMAL), texts.single { it.text.toString().replace("\u200B", "") == data.title.toString() }.typeface)
    assertEquals(Typeface.create("sans-serif-condensed", Typeface.NORMAL), texts.single { it.text.toString() == data.extra.toString() }.typeface)
    view.render(data.copy(title = "very.long.package.name.".repeat(12), extra = "", ruleChip = null))
    view.setChipOnClickListener(null)
    assertFalse(chip.isVisible)
    measure(view, (320 * context.resources.displayMetrics.density).toInt())
    assertTrue(descendants(view).filterIsInstance<TextView>().any { it.isVisible && it.lineCount > 1 })
    descendants(view).filter { it.isVisible }.forEach {
      assertTrue("${it.javaClass.simpleName} exceeds parent width", it.right <= (it.parent as View).width)
    }
    chip.performClick()
    assertEquals(1, clicks)
  }

  @Test
  fun compactHeaderKeepsMetadataAndDoesNotChangeDefaultHeader() = onMain { context ->
    val data = SnapshotTitleDisplayData(
      "Example",
      "com.example.application",
      "1.0 → 2.0",
      SnapshotTitlePackageSizeData("10 MB → 20 MB", -1),
      "Target 37  Min 24"
    )
    val modern = SnapshotTitleView(context)
    val legacy = SnapshotTitleView(context).apply { useLegacyLayout = true }
    listOf(modern, legacy).forEach {
      it.render(data)
      measure(it, (360 * context.resources.displayMetrics.density).toInt())
      assertEquals(data.versionInfo, it.versionInfoView.text.toString())
      assertTrue(it.apisView.isVisible)
      assertTrue(it.packageSizeView.isVisible)
    }
    assertFalse(modern.useLegacyLayout)
    assertTrue(legacy.height < modern.height)
    legacy.render(data.copy(versionInfo = "", packageSize = null, apis = ""))
    assertFalse(legacy.versionInfoView.isVisible)
    assertFalse(legacy.packageSizeView.isVisible)
    assertFalse(legacy.apisView.isVisible)
  }

  private fun onMain(block: (ContextThemeWrapper) -> Unit) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES).forEach { nightMode ->
        val config = Configuration(instrumentation.targetContext.resources.configuration).apply {
          uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
          fontScale = 1.3f
        }
        block(ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(config), R.style.AppTheme))
      }
    }
  }

  private fun measure(view: View, width: Int) {
    view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
    view.layout(0, 0, view.measuredWidth, view.measuredHeight)
  }

  private fun descendants(group: ViewGroup): List<View> = group.children.flatMap {
    sequenceOf(it) + if (it is ViewGroup) descendants(it).asSequence() else emptySequence()
  }.toList()

  private fun display(type: Int, status: Int): SnapshotDetailItemDisplayData {
    val item = SnapshotDetailItem("entry", "old.entry → new.entry", "10 → 20", status, type)
    return SnapshotDetailItemDisplayData(
      item,
      item.title,
      item.extra,
      "Description",
      "Report $type $status\n",
      SnapshotDetailItemStatusDisplayData(R.drawable.ic_changed, R.color.snapshot_status_changed, R.string.snapshot_indicator_changed),
      null
    )
  }
}
