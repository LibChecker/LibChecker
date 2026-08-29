package com.absinthe.libchecker.domain.statistics.reference.model

import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.SHARED_UID
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibReferenceMenuBottomSheetStateTest {

  @Test
  fun `builds ordered reference options and checked state`() {
    val state = buildLibReferenceMenuBottomSheetState(
      currentOptions = LibReferenceOptions.NATIVE_LIBS or LibReferenceOptions.PERMISSIONS
    )

    assertEquals(
      listOf(
        LibReferenceOptions.NATIVE_LIBS,
        LibReferenceOptions.SERVICES,
        LibReferenceOptions.ACTIVITIES,
        LibReferenceOptions.RECEIVERS,
        LibReferenceOptions.PROVIDERS,
        LibReferenceOptions.ACTION,
        LibReferenceOptions.PERMISSIONS,
        LibReferenceOptions.METADATA,
        LibReferenceOptions.PACKAGES,
        LibReferenceOptions.SHARED_UID,
        LibReferenceOptions.ONLY_NOT_MARKED
      ),
      state.options.map { it.option }
    )
    assertTrue(state.options.first().isChecked)
    assertTrue(state.options[6].isChecked)
    assertFalse(state.options.last().isChecked)
  }

  @Test
  fun `builds one demo item for every selected reference type`() {
    val allTypes =
      LibReferenceOptions.NATIVE_LIBS or
        LibReferenceOptions.SERVICES or
        LibReferenceOptions.ACTIVITIES or
        LibReferenceOptions.RECEIVERS or
        LibReferenceOptions.PROVIDERS or
        LibReferenceOptions.ACTION or
        LibReferenceOptions.PERMISSIONS or
        LibReferenceOptions.METADATA or
        LibReferenceOptions.PACKAGES or
        LibReferenceOptions.SHARED_UID

    val state = buildLibReferenceMenuBottomSheetState(
      currentOptions = allTypes,
      colorfulRuleIcon = false
    )

    assertEquals(
      listOf(NATIVE, SERVICE, ACTIVITY, RECEIVER, PROVIDER, ACTION, PERMISSION, METADATA, PACKAGE, SHARED_UID),
      state.demoItems.map { it.type }
    )
    assertEquals(10, state.demoItems.map { it.type }.distinct().size)
    assertEquals(false, state.colorfulRuleIcon)
  }

  @Test
  fun `uses marked demos only for reference types supported by rule matching`() {
    val state = buildLibReferenceMenuBottomSheetState(
      currentOptions =
      LibReferenceOptions.NATIVE_LIBS or
        LibReferenceOptions.SERVICES or
        LibReferenceOptions.ACTIVITIES or
        LibReferenceOptions.RECEIVERS or
        LibReferenceOptions.PROVIDERS or
        LibReferenceOptions.ACTION or
        LibReferenceOptions.PERMISSIONS or
        LibReferenceOptions.METADATA or
        LibReferenceOptions.PACKAGES or
        LibReferenceOptions.SHARED_UID
    )

    val demosByType = state.demoItems.associateBy { it.type }
    listOf(NATIVE, SERVICE, ACTIVITY, RECEIVER, PROVIDER, ACTION).forEach { type ->
      assertTrue(demosByType.getValue(type).rule != null)
    }
    listOf(PERMISSION, METADATA, PACKAGE, SHARED_UID).forEach { type ->
      assertTrue(demosByType.getValue(type).rule == null)
    }
  }

  @Test
  fun `only not marked stays a modifier and does not create or replace a demo type`() {
    val state = buildLibReferenceMenuBottomSheetState(
      currentOptions = LibReferenceOptions.NATIVE_LIBS or LibReferenceOptions.ONLY_NOT_MARKED
    )

    assertEquals(listOf(NATIVE), state.demoItems.map { it.type })
    assertTrue(state.demoItems.single().rule != null)
  }

  @Test
  fun `hides demo list when no reference type is selected`() {
    val state = buildLibReferenceMenuBottomSheetState(
      currentOptions = LibReferenceOptions.ONLY_NOT_MARKED
    )

    assertTrue(state.demoItems.isEmpty())
  }
}
