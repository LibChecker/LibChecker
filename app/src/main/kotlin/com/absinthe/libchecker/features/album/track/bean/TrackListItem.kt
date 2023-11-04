package com.absinthe.libchecker.features.album.track.bean

data class TrackListItem(
  val label: String,
  val packageName: String,
  var switchState: Boolean = false
)
