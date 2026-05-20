package com.absinthe.libchecker.features.about

import android.net.Uri

data class DeveloperInfo(
  val name: String,
  val desc: String,
  val github: String,
  val avatarUrl: Uri
)
