package com.absinthe.libchecker.domain.app.detail.insight

import com.absinthe.libchecker.utils.JsonUtil
import com.squareup.moshi.Types
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProducerIndexCompatibilityTest {
  @Test fun actualProducerCandidatesResolveLocallyIncludingLateIndexEntries() = runBlocking {
    for (sdk in listOf("androidx_test", "flutter")) {
      val definition = requireNotNull(JsonUtil.moshi.adapter(LibraryInsightDefinition::class.java).fromJson(resource(sdk, "definition.json")))
      val validator = LibraryInsightDefinitionValidator()
      assertTrue(validator.isValid(definition, sdk, definition.targetUuids.first()))
      val adapter = JsonUtil.moshi.adapter<Map<String, Any?>>(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))
      val document = requireNotNull(adapter.fromJson(resource(sdk, "index.json")))
      val lookup = definition.lookups.single()
      val entries = document.getValue(requireNotNull(lookup.entriesField)) as List<*>
      val entry = entries.last() as Map<*, *>
      val fingerprint = entry[lookup.expectedField] as String
      val requests = mutableListOf<String>()
      val repository = object : LibraryInsightRepository {
        override suspend fun getCatalog() = RemoteDocumentResult.NotFound
        override suspend fun getDefinition(path: String) = RemoteDocumentResult.NotFound
        override suspend fun getLookup(path: String): RemoteDocumentResult<Map<String, Any?>> {
          requests += path
          return RemoteDocumentResult.Success(document)
        }
      }
      val values = linkedMapOf(lookup.input to linkedSetOf(fingerprint))
      ResolveLibraryInsightUseCase(repository, validator, LibraryInsightProbeEngine())
        .resolveLocalIndexLookup(lookup, listOf(fingerprint), values)
      assertEquals(listOf(lookup.indexPath), requests)
      assertFalse(requests.single().contains(fingerprint))
      assertTrue(lookup.outputs.any { !values[it.output].isNullOrEmpty() })
      val unsafe = definition.copy(lookups = listOf(lookup.copy(indexPath = null, pathTemplate = "sdk-details/sdks/$sdk/data/{value}.json")))
      assertFalse(validator.isValid(unsafe, sdk, definition.targetUuids.first()))
    }
  }

  private fun resource(sdk: String, name: String): String = requireNotNull(javaClass.getResourceAsStream("/rules-sdk-candidates/$sdk/$name"))
    .bufferedReader().use { it.readText() }
}
