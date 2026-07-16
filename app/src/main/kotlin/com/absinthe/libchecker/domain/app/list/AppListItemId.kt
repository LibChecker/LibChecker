package com.absinthe.libchecker.domain.app.list

fun stableAppListItemIdForKey(key: String): Long {
  return key.hashCode().toLong()
}
