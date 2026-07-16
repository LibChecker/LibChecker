package com.absinthe.libchecker.domain.app.list.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AdvancedMenuLayoutItemTest {

  @Test
  fun buildsAdvancedMenuLayoutItemsInDisplayOrder() {
    assertEquals(
      listOf(
        AdvancedMenuLayoutItem.AppDemo,
        AdvancedMenuLayoutItem.Sort,
        AdvancedMenuLayoutItem.FilterOptions,
        AdvancedMenuLayoutItem.ViewOptions,
        AdvancedMenuLayoutItem.ComponentStyleDemo,
        AdvancedMenuLayoutItem.ComponentStyleOptions
      ),
      buildAdvancedMenuLayoutItems()
    )
  }
}
