package com.absinthe.libchecker.model

data class TrackListItem(
  val label: String,
  val packageName: String,
  var switchState: Boolean = false
)
