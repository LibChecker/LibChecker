package com.absinthe.libchecker.domain.settings.ui

internal const val LIB_REFERENCE_THRESHOLD_MIN = 1
internal const val LIB_REFERENCE_THRESHOLD_MAX = 50

internal fun normalizeLibReferenceThreshold(value: Int): Int {
  return value.coerceIn(
    minimumValue = LIB_REFERENCE_THRESHOLD_MIN,
    maximumValue = LIB_REFERENCE_THRESHOLD_MAX
  )
}
