package com.absinthe.libchecker.database

import android.app.Application

object Repositories {
  private lateinit var context: Application

  val lcRepository by lazy { LCRepository(LCDatabase.getDatabase(context).lcDao()) }
  val ruleRepository by lazy { RuleRepository(RuleDatabase.getDatabase(context).ruleDao()) }

  fun init(application: Application) {
    context = application
  }
}
