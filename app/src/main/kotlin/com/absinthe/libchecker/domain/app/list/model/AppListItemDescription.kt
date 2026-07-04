package com.absinthe.libchecker.domain.app.list.model

fun buildAppListItemDescription(vararg parts: CharSequence?): String {
  return parts
    .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
    .joinToString()
}
