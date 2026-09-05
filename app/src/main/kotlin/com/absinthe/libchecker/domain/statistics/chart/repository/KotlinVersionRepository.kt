package com.absinthe.libchecker.domain.statistics.chart.repository

interface KotlinVersionRepository {
  suspend fun getVersion(packageName: String): String?
}
