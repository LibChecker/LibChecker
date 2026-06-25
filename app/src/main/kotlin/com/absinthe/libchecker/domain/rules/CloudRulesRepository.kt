package com.absinthe.libchecker.domain.rules

import java.io.File

interface CloudRulesRepository {
  suspend fun getVersionInfo(): CloudRulesVersionInfo?

  fun getDownloadRequest(): CloudRulesDownloadRequest

  fun installDownloadedRules(downloadRequest: CloudRulesDownloadRequest, remoteVersion: Int): Boolean
}

data class CloudRulesVersionInfo(
  val localVersion: Int,
  val remoteVersion: Int
) {
  val updateAvailable: Boolean
    get() = localVersion < remoteVersion
}

data class CloudRulesDownloadRequest(
  val url: String,
  val destination: File
)
