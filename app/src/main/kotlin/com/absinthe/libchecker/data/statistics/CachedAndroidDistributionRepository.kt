package com.absinthe.libchecker.data.statistics

import android.content.Context
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.AndroidDistribution
import com.absinthe.libchecker.api.request.AndroidDistributionRequest
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.statistics.chart.repository.AndroidDistributionRepository
import com.absinthe.libchecker.utils.DateUtils
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libchecker.utils.toJson
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class CachedAndroidDistributionRepository(
  context: Context,
  private val request: AndroidDistributionRequest = ApiManager.create()
) : AndroidDistributionRepository {
  private val localFile = File(File(context.filesDir, "rules"), DISTRIBUTION_FILE_NAME)

  override suspend fun getDistribution(): List<AndroidDistribution>? = withContext(Dispatchers.IO) {
    runCatching {
      if (!localFile.exists() || !DateUtils.isTimestampThisMonth(GlobalValues.distributionUpdateTimestamp)) {
        val response = request.requestDistribution()
        localFile.parentFile?.mkdirs()
        localFile.writeText(response.toJson().orEmpty())
        GlobalValues.distributionUpdateTimestamp = System.currentTimeMillis()
        return@withContext response
      }

      localFile.readText().fromJson<List<AndroidDistribution>>(
        List::class.java,
        AndroidDistribution::class.java
      )
    }.onFailure {
      Timber.e(it)
    }.getOrNull()
  }

  private companion object {
    const val DISTRIBUTION_FILE_NAME = "android_distribution.json"
  }
}
