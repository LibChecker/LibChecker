package com.absinthe.libchecker.domain.statistics.reference.model

sealed interface LibReferenceLoadingState {
  // Fixed work-phase ranges, not an estimate of elapsed or remaining time.
  // Reserve the last percent until the result list is presented.
  val overallProgress: Int?
    get() = when (this) {
      Preparing -> null
      is Scanning -> progress.coerceIn(0, 100) * 80 / 100
      is Matching -> 80 + progress.coerceIn(0, 100) * 15 / 100
      is Organizing -> 95 + progress.coerceIn(0, 100) * 4 / 100
    }

  data object Preparing : LibReferenceLoadingState
  data class Scanning(val progress: Int) : LibReferenceLoadingState
  data class Matching(val progress: Int = 0) : LibReferenceLoadingState
  data class Organizing(val progress: Int = 0) : LibReferenceLoadingState
}
