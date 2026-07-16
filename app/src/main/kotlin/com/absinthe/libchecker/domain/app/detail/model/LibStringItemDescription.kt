package com.absinthe.libchecker.domain.app.detail.model

fun buildLibStringItemDescription(vararg parts: CharSequence?): String {
  return buildDetailItemDescription(*parts)
}
