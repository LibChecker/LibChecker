package com.absinthe.libchecker.database.entity

object Features {
  const val SPLIT_APKS = 1 shl 0
  const val KOTLIN_USED = 1 shl 1
  const val AGP = 1 shl 2
  const val XPOSED_MODULE = 1 shl 3
  const val PLAY_SIGNING = 1 shl 4
  const val PWA = 1 shl 5
  const val JETPACK_COMPOSE = 1 shl 6
  const val RX_JAVA = 1 shl 7
  const val RX_KOTLIN = 1 shl 8
  const val RX_ANDROID = 1 shl 9
  const val KMP = 1 shl 10

  object Ext {
    const val APPLICATION_PROP = -1 shl 0
    const val APPLICATION_INSTALL_SOURCE = -1 shl 1
    const val ELF_PAGE_SIZE_16KB = -1 shl 2
    const val ELF_PAGE_SIZE_16KB_COMPAT = -1 shl 3
  }
}
