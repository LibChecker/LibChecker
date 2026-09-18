package com.absinthe.libchecker.domain.rules

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RuleBundleMetadata(
  val schemaVersion: Int,
  val dataVersion: Int,
  val sourceRevision: String,
  val compilerRevision: String,
  val contentSha256: String,
  val ruleCount: Int,
  val minimumReader: Map<String, Int>
)

@JsonClass(generateAdapter = true)
data class RuleBundleManifest(
  val schemaVersion: Int,
  val dataVersion: Int,
  val sourceRevision: String,
  val compilerRevision: String,
  val contentSha256: String,
  val ruleCount: Int,
  val minimumReader: Map<String, Int>,
  val artifacts: Map<String, RuleBundleArtifact>
) {
  val metadata: RuleBundleMetadata
    get() = RuleBundleMetadata(schemaVersion, dataVersion, sourceRevision, compilerRevision, contentSha256, ruleCount, minimumReader)

  fun androidArtifact(): RuleBundleArtifact {
    require(schemaVersion == 5 && dataVersion > 0 && ruleCount in 1..100_000)
    require(minimumReader.getValue("android") <= 5)
    require(Regex("[a-fA-F0-9]{40,64}").matches(sourceRevision))
    require(SHA256.matches(compilerRevision) && SHA256.matches(contentSha256))
    return artifacts.getValue("android").also {
      require(it.schemaVersion == 5 && it.minimumReaderVersion <= 5)
      require(it.path == "releases/$dataVersion/android-v5.zip")
      require(SHA256.matches(it.sha256) && it.size in 1..32L * 1024 * 1024)
    }
  }

  companion object {
    internal val SHA256 = Regex("[a-f0-9]{64}")
  }
}

@JsonClass(generateAdapter = true)
data class RuleBundleArtifact(
  val path: String,
  val sha256: String,
  val size: Long,
  val schemaVersion: Int,
  val minimumReaderVersion: Int
)
