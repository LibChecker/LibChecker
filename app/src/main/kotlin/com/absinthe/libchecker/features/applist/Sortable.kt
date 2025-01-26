package com.absinthe.libchecker.features.applist

interface Sortable {
  suspend fun sort()
  suspend fun setItemsWithFilter(searchWords: String?, process: String?)
}
