package com.absinthe.libchecker.database

import com.absinthe.libchecker.database.entity.RuleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class RuleRepository(private val ruleDao: RuleDao) {

  init {
    GlobalScope.launch(Dispatchers.IO) {
      if (rules == null) {
        rules = getAllRules().asSequence()
          .map {
            it.name to it
          }
          .toMap()
      }
    }
  }

  suspend fun getRule(name: String) = rules?.get(name) ?: ruleDao.getRule(name)

  suspend fun insertRules(rules: List<RuleEntity>) {
    ruleDao.insertRules(rules)
  }

  fun deleteAllRules() {
    ruleDao.deleteAllRules()
  }

  suspend fun getAllRules() = ruleDao.getAllRules()

  suspend fun getRegexRules() = ruleDao.getRegexRules()

  companion object {
    var rules: Map<String, RuleEntity>? = null
  }
}
