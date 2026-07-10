package com.absinthe.libchecker.domain.app.detail.model

sealed interface LibStringItemDisplay {
  val name: LibStringItemNameDisplay
  val contentDescription: String
}
