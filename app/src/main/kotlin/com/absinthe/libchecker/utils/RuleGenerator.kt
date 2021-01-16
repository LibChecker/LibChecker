package com.absinthe.libchecker.utils

import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.librarymap.BaseMap
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
        var cloudRule: CloudRule

        rules.forEach {
            logd("sasa", it.name)
            ruleBuilder.apply {
                name = it.name
                label = it.label
                type = it.type
                isRegexRule = it.isRegexRule
                iconIndex = if (it.iconIndex != 0) {
                    it.iconIndex
                } else {
                    if (BaseMap.getMap(it.type).getChip(it.name)!!.iconRes == R.drawable.ic_lib_360) {
                        0
                    } else {
                        -1
                    }
                }
            }

            newRules.add(ruleBuilder.build())
        }
        logd("sasa", "size = ${rules.size}")

        rulesListBuilder.addAllCloudRules(newRules)
        bundleBuilder.rulesList = rulesListBuilder.build()
        return bundleBuilder.build().toByteArray()
    }
}