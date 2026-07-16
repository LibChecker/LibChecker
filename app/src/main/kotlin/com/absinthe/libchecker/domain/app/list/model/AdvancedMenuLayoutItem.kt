package com.absinthe.libchecker.domain.app.list.model

sealed interface AdvancedMenuLayoutItem {
  data object AppDemo : AdvancedMenuLayoutItem
  data object Sort : AdvancedMenuLayoutItem
  data object FilterOptions : AdvancedMenuLayoutItem
  data object ViewOptions : AdvancedMenuLayoutItem
  data object ComponentStyleDemo : AdvancedMenuLayoutItem
  data object ComponentStyleOptions : AdvancedMenuLayoutItem
}

fun buildAdvancedMenuLayoutItems(): List<AdvancedMenuLayoutItem> {
  return listOf(
    AdvancedMenuLayoutItem.AppDemo,
    AdvancedMenuLayoutItem.Sort,
    AdvancedMenuLayoutItem.FilterOptions,
    AdvancedMenuLayoutItem.ViewOptions,
    AdvancedMenuLayoutItem.ComponentStyleDemo,
    AdvancedMenuLayoutItem.ComponentStyleOptions
  )
}
