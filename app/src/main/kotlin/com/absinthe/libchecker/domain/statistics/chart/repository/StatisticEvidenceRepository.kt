package com.absinthe.libchecker.domain.statistics.chart.repository

interface StatisticEvidenceRepository {

  fun hasNativeLibrary(packageName: String, libraryName: String): Boolean
}
