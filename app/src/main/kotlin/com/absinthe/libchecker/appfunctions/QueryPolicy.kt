package com.absinthe.libchecker.appfunctions

import androidx.appfunctions.AppFunctionInvalidArgumentException
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.database.entity.Features

internal fun validatePage(offset: Int, limit: Int) {
  if (offset < 0 || limit !in 1..100) {
    throw AppFunctionInvalidArgumentException("offset must be >= 0; limit must be between 1 and 100")
  }
}

internal fun validateQuery(query: String, required: Boolean = false) {
  if (query.length > 200 || (required && query.isBlank())) {
    throw AppFunctionInvalidArgumentException("Query must be at most 200 characters and required identifiers must not be blank")
  }
}

internal fun nextOffset(offset: Int, limit: Int, total: Int): Int {
  val next = offset.toLong() + limit
  return if (next < total) next.toInt() else -1
}

internal val featureNames = mapOf(
  "kotlin" to Features.KOTLIN_USED,
  "compose" to Features.JETPACK_COMPOSE,
  "compose_resources" to Features.COMPOSE_MULTIPLATFORM_RESOURCES,
  "agp" to Features.AGP,
  "split_apks" to Features.SPLIT_APKS,
  "xposed" to Features.XPOSED_MODULE,
  "pwa" to Features.PWA
)

internal fun featureFlag(feature: String): Int? {
  if (feature.isEmpty()) return null
  return featureNames[feature]
    ?: throw AppFunctionInvalidArgumentException("Unsupported feature. Use: ${featureNames.keys.joinToString()}")
}

internal fun referenceOption(category: String): Int = when (category) {
  "native" -> LibReferenceOptions.NATIVE_LIBS
  "services" -> LibReferenceOptions.SERVICES
  "activities" -> LibReferenceOptions.ACTIVITIES
  "receivers" -> LibReferenceOptions.RECEIVERS
  "providers" -> LibReferenceOptions.PROVIDERS
  "permissions" -> LibReferenceOptions.PERMISSIONS
  else -> throw AppFunctionInvalidArgumentException("Unsupported library category: $category")
}
