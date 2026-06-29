package com.absinthe.libchecker.domain.app

fun stableAppListItemIdForKey(key: String): Long {
  return key.hashCode().toLong()
}
