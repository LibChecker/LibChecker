package com.absinthe.libchecker.api.bean

import androidx.annotation.Keep

@Keep
data class CloudRuleInfo(
    val version: Int,
    val count: Int,
    val bundles: Int
)