package com.absinthe.libchecker.api.offline

import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.api.bean.CloudRuleInfo
import com.absinthe.libchecker.api.bean.LibDetailBean
import com.absinthe.libchecker.utils.fromJson
import java.io.File

object OfflineRulesRequests {
  private val rules = File(LibCheckerApp.app.filesDir, "rules/LibChecker-Rules-master")

  fun getVersion(): Int {
    check(rules.exists())
    return File(rules, "cloud/md5/v3").readText().fromJson<CloudRuleInfo>()!!.version
  }

  fun getCount(): Int {
    return rules.listFiles { file -> file.absolutePath.contains("libs") }?.sumOf { libs ->
      libs.listFiles()?.toMutableList()?.apply {
        this.filter { it.isDirectory }.forEach {
          remove(it)
          addAll(it.listFiles() ?: arrayOf())
        }
      }?.size ?: 0
    } ?: 0
  }

  fun getLibDetail(categoryDir: String, libName: String): LibDetailBean {
    return File(rules, "$categoryDir/$libName.json").readText().fromJson()!!
  }
}
