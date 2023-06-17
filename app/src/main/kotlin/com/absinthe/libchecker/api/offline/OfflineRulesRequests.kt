package com.absinthe.libchecker.api.offline

import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.api.bean.CloudRuleInfo
import com.absinthe.libchecker.api.bean.LibDetailBean
import com.absinthe.libchecker.utils.fromJson
import java.io.File
import java.io.FileFilter

object OfflineRulesRequests {
  private val rules = File(LibCheckerApp.app.filesDir, "rules/LibChecker-Rules-master")

  fun getVersion(): Int {
    return File(rules, "cloud/md5/v3").readText().fromJson<CloudRuleInfo>()!!.version
  }

  fun getCount(): Int {
    return rules.listFiles(FileFilter { it.absolutePath.contains("libs") })!!
      .sumOf { it.listFiles()!!.size }
  }

  fun getLibDetail(categoryDir: String, libName: String): LibDetailBean {
    return File(rules, "$categoryDir/$libName.json").readText().fromJson()!!
  }
}
