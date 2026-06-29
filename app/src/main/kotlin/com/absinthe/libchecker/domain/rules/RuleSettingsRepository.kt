package com.absinthe.libchecker.domain.rules

interface RuleSettingsRepository {
  fun selectRemoteRepository(repository: String)
}
