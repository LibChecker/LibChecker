package com.absinthe.libchecker.domain.statistics.chart.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

const val STATISTIC_SCHEMA_VERSION = 1

@JsonClass(generateAdapter = true)
data class StatisticBundle(
  val schemaVersion: Int,
  val definitions: List<StatisticDefinition>
)

@JsonClass(generateAdapter = true)
data class StatisticDefinition(
  val id: String,
  val revision: Int,
  val source: StatisticSource,
  val title: StatisticTitleSpec,
  val icon: StatisticIconSpec,
  val calculation: StatisticCalculationSpec,
  val availability: StatisticAvailability = StatisticAvailability.ALWAYS,
  val requiresFeatureInitialization: Boolean = false,
  val controls: List<StatisticControl> = emptyList(),
  val dashboard: StatisticDashboard = StatisticDashboard.NONE,
  val fingerprint: StatisticFingerprint = StatisticFingerprint.STANDARD
) {
  fun hasControl(control: StatisticControl): Boolean = control in controls
}

@JsonClass(generateAdapter = true)
data class StatisticTitleSpec(
  val resource: StatisticTitleResource? = null,
  val translations: Map<String, String> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class StatisticIconSpec(
  val drawable: StatisticDrawableIcon? = null,
  val asset: String? = null,
  val renderMode: StatisticIconRenderMode = StatisticIconRenderMode.MONOCHROME,
  val tintRole: StatisticIconTintRole = StatisticIconTintRole.ON_SURFACE,
  @Transient val localPath: String? = null
)

@JsonClass(generateAdapter = true)
data class StatisticCalculationSpec(
  val kind: StatisticCalculationKind,
  val nativeOperator: StatisticNativeOperator? = null,
  val predicate: StatisticPredicateSpec? = null
)

@JsonClass(generateAdapter = true)
data class StatisticPredicateSpec(
  val evidence: StatisticEvidence,
  val operator: StatisticComparisonOperator,
  val value: StatisticPredicateValue,
  val matchedTitle: StatisticTitleSpec,
  val unmatchedTitle: StatisticTitleSpec
)

@JsonClass(generateAdapter = true)
data class StatisticPredicateValue(
  val integer: Long? = null,
  val boolean: Boolean? = null,
  val string: String? = null
)

enum class StatisticSource {
  @Json(name = "builtin")
  BUILTIN,

  @Json(name = "official")
  OFFICIAL,

  @Json(name = "custom")
  CUSTOM
}

enum class StatisticTitleResource {
  @Json(name = "abi")
  ABI,

  @Json(name = "kotlin")
  KOTLIN,

  @Json(name = "target_sdk")
  TARGET_SDK,

  @Json(name = "min_sdk")
  MIN_SDK,

  @Json(name = "compile_sdk")
  COMPILE_SDK,

  @Json(name = "jetpack_compose")
  JETPACK_COMPOSE,

  @Json(name = "android_distribution")
  ANDROID_DISTRIBUTION,

  @Json(name = "app_bundle")
  APP_BUNDLE,

  @Json(name = "page_size_16kb")
  PAGE_SIZE_16_KB
}

enum class StatisticIconRenderMode {
  @Json(name = "monochrome")
  MONOCHROME,

  @Json(name = "original")
  ORIGINAL
}

enum class StatisticDrawableIcon {
  @Json(name = "abi")
  ABI,

  @Json(name = "kotlin")
  KOTLIN,

  @Json(name = "target_sdk")
  TARGET_SDK,

  @Json(name = "min_sdk")
  MIN_SDK,

  @Json(name = "compile_sdk")
  COMPILE_SDK,

  @Json(name = "jetpack_compose")
  JETPACK_COMPOSE,

  @Json(name = "android_distribution")
  ANDROID_DISTRIBUTION,

  @Json(name = "app_bundle")
  APP_BUNDLE,

  @Json(name = "page_size_16kb")
  PAGE_SIZE_16_KB
}

enum class StatisticIconTintRole {
  @Json(name = "on_surface")
  ON_SURFACE,

  @Json(name = "on_surface_variant")
  ON_SURFACE_VARIANT,

  @Json(name = "primary")
  PRIMARY,

  @Json(name = "secondary")
  SECONDARY,

  @Json(name = "tertiary")
  TERTIARY
}

enum class StatisticCalculationKind {
  @Json(name = "native")
  NATIVE,

  @Json(name = "predicate")
  PREDICATE
}

enum class StatisticEvidence {
  @Json(name = "target_sdk")
  TARGET_SDK,

  @Json(name = "native_library")
  NATIVE_LIBRARY
}

enum class StatisticComparisonOperator {
  @Json(name = "equal")
  EQUAL,

  @Json(name = "greater_than_or_equal")
  GREATER_THAN_OR_EQUAL,

  @Json(name = "less_than_or_equal")
  LESS_THAN_OR_EQUAL,

  @Json(name = "contains")
  CONTAINS
}

enum class StatisticNativeOperator {
  @Json(name = "abi")
  ABI,

  @Json(name = "kotlin")
  KOTLIN,

  @Json(name = "target_sdk")
  TARGET_SDK,

  @Json(name = "min_sdk")
  MIN_SDK,

  @Json(name = "compile_sdk")
  COMPILE_SDK,

  @Json(name = "jetpack_compose")
  JETPACK_COMPOSE,

  @Json(name = "android_distribution")
  ANDROID_DISTRIBUTION,

  @Json(name = "app_bundle")
  APP_BUNDLE,

  @Json(name = "page_size_16kb")
  PAGE_SIZE_16_KB
}

enum class StatisticAvailability {
  @Json(name = "always")
  ALWAYS,

  @Json(name = "page_size_16kb")
  PAGE_SIZE_16_KB
}

enum class StatisticControl {
  @Json(name = "detailed_abi")
  DETAILED_ABI
}

enum class StatisticDashboard {
  @Json(name = "none")
  NONE,

  @Json(name = "android_distribution")
  ANDROID_DISTRIBUTION
}

enum class StatisticFingerprint {
  @Json(name = "standard")
  STANDARD,

  @Json(name = "features")
  FEATURES,

  @Json(name = "artifact")
  ARTIFACT
}
