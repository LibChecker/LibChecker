package com.absinthe.libchecker.domain.app.list.model

sealed interface AdvancedMenuLayoutItem {
  data object AppDemo : AdvancedMenuLayoutItem
  data object Sort : AdvancedMenuLayoutItem
  data object FilterOptions : AdvancedMenuLayoutItem
  data object ViewOptions : AdvancedMenuLayoutItem
}

fun buildAdvancedMenuLayoutItems(): List<AdvancedMenuLayoutItem> = listOf(
  AdvancedMenuLayoutItem.AppDemo,
  AdvancedMenuLayoutItem.ViewOptions,
  AdvancedMenuLayoutItem.Sort,
  AdvancedMenuLayoutItem.FilterOptions
)
