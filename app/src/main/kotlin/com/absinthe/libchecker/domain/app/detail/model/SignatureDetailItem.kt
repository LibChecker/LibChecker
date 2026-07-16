package com.absinthe.libchecker.domain.app.detail.model

data class SignatureDetailItem(
  val type: String,
  val content: String,
  val contentDescription: String = buildDetailItemDescription(type, content)
)
