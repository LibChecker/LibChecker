package com.absinthe.libchecker.utils

import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.database.entity.RuleEntity
import com.absinthe.libchecker.extensions.logd
import com.absinthe.libchecker.protocol.CloudRule
import com.absinthe.libchecker.protocol.CloudRulesBundle
import com.absinthe.libchecker.protocol.CloudRulesList

object RuleGenerator {
    fun generateRulesByteArray(rules: List<RuleEntity>, version: Int): ByteArray {
        val bundleBuilder: CloudRulesBundle.Builder = CloudRulesBundle.newBuilder()
        bundleBuilder.version = version
        bundleBuilder.count = rules.size

        val newRules = mutableListOf<CloudRule>()
        val ruleBuilder: CloudRule.Builder = CloudRule.newBuilder()
        val rulesListBuilder: CloudRulesList.Builder = CloudRulesList.newBuilder()

        rules.forEach {
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
        ruleBuilder.apply {
            name = "libmediainfo.so"
            label = "MediaInfoLib"
            type = NATIVE
            isRegexRule = false
            iconIndex = -1
            regexName = ""
        }
        logd("RuleGenerator",newRules.size.toString())

        rulesListBuilder.addAllCloudRules(newRules)
        bundleBuilder.rulesList = rulesListBuilder.build()
        return bundleBuilder.build().toByteArray()
    }
}