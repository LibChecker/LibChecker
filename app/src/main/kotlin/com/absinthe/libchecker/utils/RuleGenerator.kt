package com.absinthe.libchecker.utils

import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.entity.RuleEntity
import com.absinthe.libchecker.extensions.logd
import com.absinthe.libchecker.protocol.CloudRule
import com.absinthe.libchecker.protocol.CloudRulesBundle
import com.absinthe.libchecker.protocol.CloudRulesList

object RuleGenerator {
    fun generateRulesByteArray(): ByteArray {
        val bundleBuilder: CloudRulesBundle.Builder = CloudRulesBundle.newBuilder()

        val inputStream = LibCheckerApp.context.resources.assets.open("rules.lcr.1")
        val rulesBundle = CloudRulesBundle.parseFrom(inputStream)
        val rulesList = mutableListOf<RuleEntity>()
        rulesBundle.rulesList.cloudRulesList.forEach {
            it?.let {
                rulesList.add(RuleEntity(it.name, it.label, it.type, it.iconIndex, it.isRegexRule, it.regexName))
            }
        }

        bundleBuilder.version = GlobalValues.localRulesVersion + 1
        bundleBuilder.count = rulesList.size

        val newRules = mutableListOf<CloudRule>()
        val ruleBuilder: CloudRule.Builder = CloudRule.newBuilder()
        val rulesListBuilder: CloudRulesList.Builder = CloudRulesList.newBuilder()

        rulesList.forEach {
            ruleBuilder.apply {
                name = it.name
                label = it.label
                type = it.type
                isRegexRule = it.isRegexRule
                iconIndex = it.iconIndex
                regexName = it.regexName ?: ""
            }

            newRules.add(ruleBuilder.build())
        }
//        ruleBuilder.apply {
//            name = "libhpplayae.so"
//            label = "乐播 SDK"
//            type = NATIVE
//            isRegexRule = false
//            iconIndex = -1
//            regexName = ""
//        }
//        newRules.add(ruleBuilder.build())

        logd("RuleGenerator",newRules.size.toString())

        rulesListBuilder.addAllCloudRules(newRules)
        bundleBuilder.rulesList = rulesListBuilder.build()
        return bundleBuilder.build().toByteArray()
    }
}