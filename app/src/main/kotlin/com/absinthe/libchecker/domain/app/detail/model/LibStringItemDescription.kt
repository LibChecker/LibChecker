package com.absinthe.libchecker.domain.app.detail.model

fun buildLibStringItemDescription(vararg parts: CharSequence?): String {
  return parts
    .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
    .joinToString()
}
