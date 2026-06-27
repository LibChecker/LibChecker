package com.absinthe.libchecker.domain.app.detail.ui

interface Sortable {
  suspend fun sort()
  suspend fun setItemsWithFilter(searchWords: String?, process: String?)
}
