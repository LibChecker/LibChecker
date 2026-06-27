package com.absinthe.libchecker.domain.about.model

import android.net.Uri

data class DeveloperInfo(
  val name: String,
  val desc: String,
  val github: String,
  val avatarUrl: Uri
)
