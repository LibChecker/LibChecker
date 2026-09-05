package com.absinthe.libchecker.ui.app

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.PermissionDetailBottomSheetState
import com.absinthe.libchecker.domain.app.detail.model.PermissionDetailContent
import com.absinthe.libchecker.domain.app.detail.ui.view.PermissionInfoBottomSheetView
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionInfoBottomSheetInstrumentedTest {

  @Test
  fun loadingThenContentThenLoadingClearsTextAndAccessibility() = withView { view ->
    val context = view.context
    view.bind(PermissionDetailBottomSheetState.Loading(PERMISSION))
    assertTrue(view.texts().contains(PERMISSION))
    assertTrue(view.texts().contains(context.getString(R.string.loading)))
    assertFalse(view.accessibleText().contains(context.getString(R.string.not_found)))

    view.bind(content("Camera", "Take pictures", "Android"))
    assertTrue(view.texts().contains("Camera"))
    assertTrue(view.texts().contains("Take pictures"))
    assertTrue(view.texts().any { it.contains(PERMISSION) && it.contains("Android") })
    assertFalse(view.accessibleText().contains(context.getString(R.string.loading)))

    view.bind(PermissionDetailBottomSheetState.Loading(PERMISSION))
    assertFalse(view.accessibleText().contains("Take pictures"))
    assertFalse(view.texts().contains("Camera"))
  }

  @Test
  fun missingFieldsKeepAvailableContent() = withView { view ->
    view.bind(content("Camera", null))
    assertTrue(view.texts().contains("Camera"))
    assertTrue(view.texts().contains(view.context.getString(R.string.lib_permission_description_not_provided)))
    assertFalse(view.accessibleText().contains(view.context.getString(R.string.not_found)))

    view.bind(content(" ", "Take pictures"))
    assertTrue(view.texts().contains(view.context.getString(R.string.lib_permission_label_not_provided)))
    assertTrue(view.texts().contains("Take pictures"))
  }

  @Test
  fun queryFailureIsDistinctFromMissingFieldsAndUnknownPermission() = withView { view ->
    view.bind(PermissionDetailBottomSheetState.Unavailable(PERMISSION, notFound = false))
    assertTrue(view.texts().contains(PERMISSION))
    assertTrue(view.texts().contains(view.context.getString(R.string.lib_permission_load_failed)))
    assertFalse(view.accessibleText().contains(view.context.getString(R.string.lib_permission_description_not_provided)))

    view.bind(PermissionDetailBottomSheetState.Unavailable(PERMISSION, notFound = true))
    assertTrue(view.texts().contains(view.context.getString(R.string.not_found)))
    assertFalse(view.accessibleText().contains(view.context.getString(R.string.lib_permission_load_failed)))
  }

  private fun withView(block: (PermissionInfoBottomSheetView) -> Unit) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      block(PermissionInfoBottomSheetView(ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)))
    }
  }

  private fun content(label: String?, description: String?, provider: String? = null) = PermissionDetailBottomSheetState.Content(PermissionDetailContent(PERMISSION, null, label, description, provider))

  private fun View.descendants(): List<View> = listOf(this) +
    if (this is ViewGroup) (0 until childCount).flatMap { getChildAt(it).descendants() } else emptyList()

  private fun View.texts(): List<String> = descendants().filterIsInstance<TextView>().map { it.text.toString() }

  private fun View.accessibleText(): String = (texts() + descendants().mapNotNull { it.contentDescription?.toString() }).joinToString("\n")

  private companion object {
    const val PERMISSION = "android.permission.CAMERA"
  }
}
