package com.absinthe.libchecker.data.rules

import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.rules.RuleSettingsRepository

class GlobalRuleSettingsRepository : RuleSettingsRepository {
  override fun selectRemoteRepository(repository: String) {
    GlobalValues.repo = repository
    RulesRepository.setRemoteRepo(repository)
  }
}
