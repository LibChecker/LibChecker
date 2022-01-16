package com.absinthe.libchecker.ui.fragment.detail

interface Sortable {
  suspend fun sort()
  fun filterList(text: String)
}
