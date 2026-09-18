package com.absinthe.libchecker.data.rules

import android.content.Context
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.request.CloudRuleBundleRequest
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.rules.CloudRulesDownloadRequest
import com.absinthe.libchecker.domain.rules.CloudRulesRepository
import com.absinthe.libchecker.domain.rules.CloudRulesVersionInfo
import com.absinthe.libchecker.domain.rules.RuleBundleManifest

class AndroidCloudRulesRepository(
  context: Context,
  request: CloudRuleBundleRequest? = null
) : CloudRulesRepository {

  private val appContext = context.applicationContext
  private val request by lazy { request ?: ApiManager.create<CloudRuleBundleRequest>() }

  private var manifest: RuleBundleManifest? = null

  override suspend fun getVersionInfo(): CloudRulesVersionInfo? {
    manifest = null
    val remote = request.requestV5Manifest()?.also { it.androidArtifact() } ?: return null
    manifest = remote
    return CloudRulesVersionInfo(RulesRepository.getLocalVersion(appContext), remote.dataVersion)
  }

  override fun getDownloadRequest(): CloudRulesDownloadRequest {
    val selected = checkNotNull(manifest)
    return CloudRulesDownloadRequest(
      url = ApiManager.rulesV5Root + selected.androidArtifact().path,
      destination = RulesRepository.getDownloadFile(appContext),
      manifest = selected
    )
  }

  override fun installDownloadedRules(downloadRequest: CloudRulesDownloadRequest, remoteVersion: Int): Boolean {
    val selected = downloadRequest.manifest
    return selected.dataVersion == remoteVersion && RulesRepository.installBundle(appContext, selected, downloadRequest.destination)
  }

  override fun reinitializeRules() {
    RulesRepository.reinitialize()
  }
}
