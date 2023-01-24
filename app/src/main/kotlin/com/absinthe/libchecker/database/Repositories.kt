package com.absinthe.libchecker.database

import android.app.Application
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.rulesbundle.LCRules
import com.jakewharton.processphoenix.ProcessPhoenix
import java.io.File

object Repositories {
  private lateinit var context: Application

  val lcRepository by lazy { LCRepository(LCDatabase.getDatabase(context).lcDao()) }

  fun init(application: Application) {
    context = application
  }

  fun getLCDatabaseFile(): File {
    val databaseDir = context.getDatabasePath(Constants.RULES_DATABASE_NAME).parent
    return File(databaseDir, Constants.RULES_DATABASE_NAME)
  }

  fun checkRulesDatabase() {
    if (GlobalValues.localRulesVersion < LCRules.getVersion()) {
      deleteRulesDatabase()
      GlobalValues.localRulesVersion = LCRules.getVersion()
      GlobalValues.localRulesCount = LCRules.getItemCounts()
      ProcessPhoenix.triggerRebirth(context)
    }
  }

  fun deleteRulesDatabase() {
    val databaseDir = context.getDatabasePath(Constants.RULES_DATABASE_NAME).parent
    FileUtils.delete(File(databaseDir, Constants.RULES_DATABASE_NAME))
    FileUtils.delete(File(databaseDir, "${Constants.RULES_DATABASE_NAME}-shm"))
    FileUtils.delete(File(databaseDir, "${Constants.RULES_DATABASE_NAME}-wal"))
  }
}
