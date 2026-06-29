package com.absinthe.libchecker.di

import com.absinthe.libchecker.data.rules.AndroidCloudRulesRepository
import com.absinthe.libchecker.data.rules.GlobalRuleSettingsRepository
import com.absinthe.libchecker.domain.rules.CloudRulesRepository
import com.absinthe.libchecker.domain.rules.RuleSettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val rulesModule = module {
  single<CloudRulesRepository> { AndroidCloudRulesRepository(androidContext()) }
  single<RuleSettingsRepository> { GlobalRuleSettingsRepository() }
}
