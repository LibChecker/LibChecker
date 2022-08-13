package com.absinthe.libchecker.database.entity

object Features {
  val SPLIT_APKS = 1 shl 0
  val KOTLIN_USED = 1 shl 1
  val AGP = 1 shl 2
  val XPOSED_MODULE = 1 shl 3
  val PLAY_SIGNING = 1 shl 4
  val PWA = 1 shl 5
  val JETPACK_COMPOSE = 1 shl 6
  val RX_JAVA = 1 shl 7
  val RX_KOTLIN = 1 shl 8
  val RX_ANDROID = 1 shl 9
}
