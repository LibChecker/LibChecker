package com.absinthe.libchecker.domain.app.detail.ui

import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.STATIC
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DetailTabSpecTest {

  @Test
  fun itemsKeepTypesAndTitlesPaired() {
    val spec = DetailTabSpec(
      items = listOf(
        DetailTabItem(NATIVE, "Native"),
        DetailTabItem(SERVICE, "Services")
      )
    )

    assertEquals(listOf(NATIVE, SERVICE), spec.types)
    assertEquals("Services", spec.itemAt(1)?.title)
  }

  @Test
  fun staticLibraryTabIsInsertedAfterNativeTab() {
    val spec = DetailTabSpec(
      items = listOf(
        DetailTabItem(NATIVE, "Native"),
        DetailTabItem(SERVICE, "Services")
      )
    )

    val updated = spec.withStaticLibraryTab("Static")

    assertEquals(listOf(NATIVE, STATIC, SERVICE), updated.types)
    assertEquals("Static", updated.itemAt(1)?.title)
  }

  @Test
  fun duplicateStaticLibraryTabKeepsExistingState() {
    val spec = DetailTabSpec(
      items = listOf(
        DetailTabItem(NATIVE, "Native"),
        DetailTabItem(STATIC, "Static")
      )
    )

    assertSame(spec, spec.withStaticLibraryTab("Another static"))
  }

  @Test
  fun staticLibraryTabCanBeInsertedIntoEmptyState() {
    val updated = DetailTabSpec().withStaticLibraryTab("Static")

    assertEquals(listOf(STATIC), updated.types)
  }
}
