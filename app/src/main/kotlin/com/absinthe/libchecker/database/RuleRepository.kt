package com.absinthe.libchecker.database

import com.absinthe.libchecker.database.entity.RuleEntity

class RuleRepository(private val ruleDao: RuleDao) {

    suspend fun getRule(name: String) = ruleDao.getRule(name)

    suspend fun insertRules(rules: List<RuleEntity>) {
        ruleDao.insertRules(rules)
    }

    fun deleteAllRules() {
        ruleDao.deleteAllRules()
    }

    suspend fun getAllRules() = ruleDao.getAllRules()

    suspend fun getRegexRules() = ruleDao.getRegexRules()
}