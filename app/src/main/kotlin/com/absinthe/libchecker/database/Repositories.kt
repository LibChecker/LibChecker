package com.absinthe.libchecker.database

import android.app.Application
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.rulesbundle.LCRules
import com.jakewharton.processphoenix.ProcessPhoenix
import java.io.File

object Repositories {
  private lateinit var context: Application

  val lcRepository by lazy { LCRepository(LCDatabase.getDatabase(context).lcDao()) }

  fun init(application: Application) {
    context = application
  }

  fun checkRulesDatabase() {
    if (GlobalValues.localRulesVersion < LCRules.getVersion()) {
      deleteRulesDatabase()
      GlobalValues.localRulesVersion = LCRules.getVersion()
      ProcessPhoenix.triggerRebirth(context)
    }
  }

  fun deleteRulesDatabase() {
    val databaseDir = File(context.filesDir.parent, "databases")
    if (databaseDir.exists()) {
      var file = File(databaseDir, Constants.RULES_DATABASE_NAME)
      if (file.exists()) {
        file.delete()
      }
      file = File(databaseDir, "${Constants.RULES_DATABASE_NAME}-shm")
      if (file.exists()) {
        file.delete()
      }
      file = File(databaseDir, "${Constants.RULES_DATABASE_NAME}-wal")
      if (file.exists()) {
        file.delete()
      }
    }
  }
}
