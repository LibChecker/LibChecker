package com.absinthe.libchecker.features.applist

interface Sortable {
  suspend fun sort()
  fun filterList(text: String)
}
