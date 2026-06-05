package com.absinthe.libchecker.database

object Repositories {
  val lcRepository by lazy { LCRepository(LCDatabase.getDatabase().lcDao()) }
}
