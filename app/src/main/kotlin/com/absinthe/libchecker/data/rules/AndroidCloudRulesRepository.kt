package com.absinthe.libchecker.data.rules

import android.content.Context
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.request.CloudRuleBundleRequest
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.rules.CloudRulesDownloadRequest
import com.absinthe.libchecker.domain.rules.CloudRulesRepository
import com.absinthe.libchecker.domain.rules.CloudRulesVersionInfo

class AndroidCloudRulesRepository(
  context: Context,
  private val request: CloudRuleBundleRequest = ApiManager.create()
) : CloudRulesRepository {

  private val appContext = context.applicationContext

  override suspend fun getVersionInfo(): CloudRulesVersionInfo? {
    val remoteInfo = request.requestCloudRuleInfo() ?: return null
    return CloudRulesVersionInfo(
      localVersion = RulesRepository.getLocalVersion(appContext),
      remoteVersion = remoteInfo.version
    )
  }

  override fun getDownloadRequest(): CloudRulesDownloadRequest {
    return CloudRulesDownloadRequest(
      url = ApiManager.rulesBundleUrl,
      destination = RulesRepository.getDownloadFile(appContext)
    )
  }

  override fun installDownloadedRules(downloadRequest: CloudRulesDownloadRequest, remoteVersion: Int): Boolean {
    if (!RulesRepository.replaceDatabase(downloadRequest.destination, appContext)) {
      return false
    }
    RulesRepository.setLocalVersion(appContext, remoteVersion)
    return true
  }

  override fun reinitializeRules() {
    RulesRepository.reinitialize()
  }
}
