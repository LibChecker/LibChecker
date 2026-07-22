package com.absinthe.libchecker.data.app.insight

import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.request.RulesDocumentRequest
import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightCatalog
import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightDefinition
import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightRepository
import com.absinthe.libchecker.domain.app.detail.insight.RemoteDocumentResult
import com.absinthe.libchecker.utils.JsonUtil
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RemoteLibraryInsightRepository(
  private val request: RulesDocumentRequest
) : LibraryInsightRepository {

  private val catalogAdapter = JsonUtil.moshi.adapter(LibraryInsightCatalog::class.java)
  private val definitionAdapter = JsonUtil.moshi.adapter(LibraryInsightDefinition::class.java)
  private val lookupAdapter: JsonAdapter<Map<String, Any?>> = JsonUtil.moshi.adapter(
    Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
  )
  private val catalogMutex = Mutex()
  private var catalogCache: LibraryInsightCatalog? = null

  override suspend fun getCatalog(): RemoteDocumentResult<LibraryInsightCatalog> = catalogMutex.withLock {
    catalogCache?.let { return RemoteDocumentResult.Success(it) }
    fetch(CATALOG_PATH, MAX_CATALOG_BYTES, catalogAdapter).also { result ->
      if (result is RemoteDocumentResult.Success) {
        catalogCache = result.value
      }
    }
  }

  override suspend fun getDefinition(path: String): RemoteDocumentResult<LibraryInsightDefinition> {
    if (!isSafePath(path)) return RemoteDocumentResult.Failure
    return fetch(path, MAX_DEFINITION_BYTES, definitionAdapter)
  }

  override suspend fun getLookup(path: String): RemoteDocumentResult<Map<String, Any?>> {
    if (!isSafePath(path)) return RemoteDocumentResult.Failure
    return fetch(path, MAX_LOOKUP_BYTES, lookupAdapter)
  }

  private suspend fun <T> fetch(
    path: String,
    maxBytes: Long,
    adapter: JsonAdapter<T>
  ): RemoteDocumentResult<T> {
    if (!isSafePath(path)) return RemoteDocumentResult.Failure
    var foundFailure = false
    for (root in ApiManager.rulesRootsInPreferenceOrder) {
      val response = try {
        request.get(root + path)
      } catch (exception: CancellationException) {
        throw exception
      } catch (_: Exception) {
        foundFailure = true
        continue
      }
      if (!response.isSuccessful) {
        response.errorBody()?.close()
        if (response.code() != 404) foundFailure = true
        continue
      }
      val body = response.body()
      if (body == null) {
        foundFailure = true
        continue
      }
      if (body.contentLength() > maxBytes) {
        body.close()
        return RemoteDocumentResult.Failure
      }
      val bytes = try {
        body.use {
          it.source().readByteArray(maxBytes + 1)
        }
      } catch (exception: CancellationException) {
        throw exception
      } catch (_: Exception) {
        foundFailure = true
        continue
      }
      if (bytes.size > maxBytes) return RemoteDocumentResult.Failure
      val value = runCatching { adapter.fromJson(bytes.decodeToString()) }.getOrNull()
      if (value != null) return RemoteDocumentResult.Success(value)
      foundFailure = true
    }
    return if (foundFailure) RemoteDocumentResult.Failure else RemoteDocumentResult.NotFound
  }

  private fun isSafePath(path: String): Boolean {
    return path.startsWith(SDK_DETAILS_PREFIX) &&
      !path.startsWith('/') &&
      !path.contains("..") &&
      !path.contains('\\') &&
      !path.contains("://")
  }

  private companion object {
    const val SDK_DETAILS_PREFIX = "sdk-details/"
    const val CATALOG_PATH = "sdk-details/catalog.json"
    const val MAX_CATALOG_BYTES = 64L * 1024
    const val MAX_DEFINITION_BYTES = 128L * 1024
    const val MAX_LOOKUP_BYTES = 64L * 1024
  }
}
