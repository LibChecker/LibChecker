package com.absinthe.libchecker.ui.app

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import android.os.SystemClock
import android.view.RoundedCorner
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.list.ui.AdvancedMenuBSDFragment
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.view.app.BottomSheetBackgroundDrawable
import com.absinthe.libchecker.view.drawable.setG2Shape
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.ShapeAppearancePathProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@SdkSuppress(minSdkVersion = 31)
class BottomSheetAppearanceInstrumentedTest {
  @Test
  fun systemCornersAndTopOnlyStrokeWithMissingCornerFallback() {
    val context = ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().targetContext, R.style.AppTheme)
    val source = MaterialShapeDrawable(
      ShapeAppearanceModel.builder().setTopLeftCornerSize(16f).setTopRightCornerSize(16f).build()
    ).apply { fillColor = ColorStateList.valueOf(Color.BLACK) }
    val drawable = BottomSheetBackgroundDrawable(context, source)
    drawable.setBounds(0, 0, 240, 160)
    drawable.updateCorners(
      WindowInsets.Builder(WindowInsets.Builder().build())
        .setRoundedCorner(RoundedCorner.POSITION_TOP_LEFT, RoundedCorner(RoundedCorner.POSITION_TOP_LEFT, 40, 40, 40))
        .setRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT, RoundedCorner(RoundedCorner.POSITION_TOP_RIGHT, 24, 216, 24))
        .build()
    )
    val bounds = RectF(drawable.bounds)
    assertEquals(40f, drawable.shapeAppearanceModel.topLeftCornerSize.getCornerSize(bounds), 0f)
    assertEquals(24f, drawable.shapeAppearanceModel.topRightCornerSize.getCornerSize(bounds), 0f)
    val actualPath = Path()
    ShapeAppearancePathProvider.getInstance().calculatePath(drawable.shapeAppearanceModel, 1f, bounds, actualPath)
    val sharedPath = Path().apply { setG2Shape(0f, 0f, 240f, 160f, 40f, rightCornerRadius = 24f) }
    val topHalf = Region(0, 0, 240, 80)
    val difference = Region().apply { setPath(actualPath, topHalf) }
    difference.op(Region().apply { setPath(sharedPath, topHalf) }, Region.Op.XOR)
    assertTrue("Sheet corners must match the existing G2 shape", difference.isEmpty)
    val bitmap = Bitmap.createBitmap(240, 160, Bitmap.Config.ARGB_8888)
    drawable.draw(Canvas(bitmap))
    assertEquals("Rounded corner must remain transparent", 0, Color.alpha(bitmap.getPixel(0, 0)))
    assertTrue("Top edge must have a visible stroke", bitmap.getPixel(120, 1) != Color.BLACK)
    val edgeColor = bitmap.getPixel(120, 1)
    assertTrue("Stroke must stay close to the sheet background", maxOf(Color.red(edgeColor), Color.green(edgeColor), Color.blue(edgeColor)) <= 11)
    val centerBrightness = maxOf(Color.red(edgeColor), Color.green(edgeColor), Color.blue(edgeColor))
    for (x in listOf(1, 238)) {
      val edgeBrightness = (0 until 80).maxOf { y ->
        val pixel = bitmap.getPixel(x, y)
        maxOf(Color.red(pixel), Color.green(pixel), Color.blue(pixel)) * Color.alpha(pixel) / 255
      }
      assertTrue("Stroke must fade away at both screen edges", edgeBrightness <= centerBrightness / 3)
    }
    assertTrue("Left arc must have a visible stroke", (10..14).any { bitmap.getPixel(it, it) != Color.BLACK && Color.alpha(bitmap.getPixel(it, it)) > 0 })
    assertEquals("Side must not be outlined", Color.BLACK, bitmap.getPixel(1, 100))
    assertEquals("Bottom must not be outlined", Color.BLACK, bitmap.getPixel(120, 159))
    bitmap.recycle()
    drawable.updateCorners(
      WindowInsets.Builder()
        .setRoundedCorner(RoundedCorner.POSITION_TOP_LEFT, null)
        .setRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT, null)
        .build()
    )
    assertEquals(16f, drawable.shapeAppearanceModel.topLeftCornerSize.getCornerSize(bounds), 0f)
    assertEquals(16f, drawable.shapeAppearanceModel.topRightCornerSize.getCornerSize(bounds), 0f)
  }

  @Test
  fun collapsedAndExpandedSheetKeepSystemCorners() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      lateinit var fragment: AdvancedMenuBSDFragment
      lateinit var sheet: View
      lateinit var background: BottomSheetBackgroundDrawable
      scenario.onActivity {
        fragment = AdvancedMenuBSDFragment()
        fragment.showNow(it.supportFragmentManager, "appearance-test")
      }
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      scenario.onActivity {
        sheet = fragment.root.parent as View
        background = sheet.background as BottomSheetBackgroundDrawable
        val insets = sheet.rootWindowInsets
        val bounds = RectF(background.bounds)
        insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.let { corner ->
          assertEquals(corner.radius.toFloat(), background.shapeAppearanceModel.topLeftCornerSize.getCornerSize(bounds), 0f)
        }
        insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.let { corner ->
          assertEquals(corner.radius.toFloat(), background.shapeAppearanceModel.topRightCornerSize.getCornerSize(bounds), 0f)
        }
      }
      for (state in listOf(BottomSheetBehavior.STATE_EXPANDED, BottomSheetBehavior.STATE_COLLAPSED)) {
        scenario.onActivity { BottomSheetBehavior.from(sheet).state = state }
        SystemClock.sleep(800)
        scenario.onActivity {
          assertEquals(state, BottomSheetBehavior.from(sheet).state)
          assertSame(background, sheet.background)
          assertEquals("Corners must not flatten during expansion", 1f, background.interpolation, 0f)
        }
      }
      scenario.onActivity { fragment.dismissNow() }
    }
  }
}
