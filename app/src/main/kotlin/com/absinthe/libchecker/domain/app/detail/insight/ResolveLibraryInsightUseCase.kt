package com.absinthe.libchecker.domain.app.detail.insight

import android.content.pm.PackageInfo
import android.icu.util.ULocale
import java.util.Locale
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class ResolveLibraryInsightUseCase(
  private val repository: LibraryInsightRepository,
  private val validator: LibraryInsightDefinitionValidator,
  private val probeEngine: LibraryInsightProbeEngine,
  private val resolveLanguageAndScript: (Locale) -> String? = ::resolveLikelyLanguageAndScript
) {

  suspend operator fun invoke(
    libraryUuid: String,
    packageInfo: PackageInfo,
    localeTag: String,
    onSupported: () -> Unit = {}
  ): LibraryInsightResult {
    val catalog = when (val result = repository.getCatalog()) {
      is RemoteDocumentResult.Success -> result.value

      RemoteDocumentResult.Failure,
      RemoteDocumentResult.NotFound -> return LibraryInsightResult.NotSupported
    }
    if (!validator.isValid(catalog)) return LibraryInsightResult.NotSupported
    val entry = catalog.entries.firstOrNull { libraryUuid in it.libraryUuids }
      ?: return LibraryInsightResult.NotSupported

    onSupported()
    var definition = when (val result = repository.getDefinition(entry.definition)) {
      is RemoteDocumentResult.Success -> result.value

      RemoteDocumentResult.Failure,
      RemoteDocumentResult.NotFound -> return LibraryInsightResult.Unavailable
    }
    // Fixed-index definitions are staged separately while older clients still use URL templates.
    if (definition.lookups.any { LibraryInsightDefinitionValidator.VALUE_PLACEHOLDER in it.pathTemplate }) {
      definition = when (val result = repository.getDefinition("sdk-details/candidates/${entry.sdkId}/definition.json")) {
        is RemoteDocumentResult.Success -> result.value

        RemoteDocumentResult.Failure,
        RemoteDocumentResult.NotFound -> return LibraryInsightResult.Unavailable
      }
    }
    if (!validator.isValid(definition, entry.sdkId, libraryUuid)) {
      return LibraryInsightResult.Unavailable
    }

    val values = linkedMapOf<String, LinkedHashSet<String>>()
    val evidenceFound = coroutineScope {
      // Paths are fixed and validated; downloading them does not depend on local fingerprints.
      val documents = definition.lookups.map { it.indexPath ?: it.pathTemplate }.distinct()
        .associateWith { path -> async { repository.getLookup(path) } }
      try {
        val probeResult = probeEngine.probe(packageInfo, definition)
        if (probeResult.evidenceFound) {
          probeResult.values.forEach { (key, result) -> values.getOrPut(key, ::linkedSetOf).addAll(result) }
          resolveLookups(definition.lookups, values, documents)
        }
        probeResult.evidenceFound
      } finally {
        documents.values.forEach { it.cancel() }
      }
    }
    if (!evidenceFound) return LibraryInsightResult.NotSupported

    val summary = definition.presentation.summary.mapNotNull { it.toDisplayField(values, localeTag) }
    if (summary.isEmpty()) return LibraryInsightResult.Unavailable
    val details = definition.presentation.details.mapNotNull { it.toDisplayField(values, localeTag) }
    return LibraryInsightResult.Content(
      LibraryInsightContent(
        sdkId = definition.sdkId,
        summary = summary,
        details = details
      )
    )
  }

  private suspend fun resolveLookups(
    lookups: List<LibraryInsightDefinition.Lookup>,
    values: MutableMap<String, LinkedHashSet<String>>,
    documents: Map<String, Deferred<RemoteDocumentResult<Map<String, Any?>>>>
  ) {
    lookups.forEach { lookup ->
      val inputs = values[lookup.input]
        .orEmpty()
        .asSequence()
        .filter(LibraryInsightDefinitionValidator.LOOKUP_VALUE::matches)
        .take(lookup.maxRequests)
        .toList()
      if (inputs.isEmpty()) return@forEach

      val path = lookup.indexPath ?: lookup.pathTemplate
      val document = (documents.getValue(path).await() as? RemoteDocumentResult.Success)?.value ?: return@forEach
      resolveLocalIndexLookup(lookup, inputs, values, document)
    }
  }

  private suspend fun resolveLocalIndexLookup(
    lookup: LibraryInsightDefinition.Lookup,
    inputs: List<String>,
    values: MutableMap<String, LinkedHashSet<String>>,
    document: Map<String, Any?>
  ) = withContext(Dispatchers.Default) {
    val expectedField = lookup.expectedField ?: return@withContext
    val entriesField = lookup.entriesField ?: lookup.itemsField ?: return@withContext
    val entries = document[entriesField] as? List<*> ?: return@withContext
    val matched = entries.asSequence().filter { raw ->
      val entry = raw as? Map<*, *> ?: return@filter false
      entry[expectedField] in inputs
    }
    val items = if (lookup.indexPath != null && lookup.itemsField != null) {
      matched.flatMap { ((it as Map<*, *>)[lookup.itemsField] as? List<*>).orEmpty().asSequence() }
    } else {
      matched
    }
    appendLookupOutputs(lookup, items, values)
  }

  private fun appendLookupOutputs(
    lookup: LibraryInsightDefinition.Lookup,
    items: Sequence<*>,
    values: MutableMap<String, LinkedHashSet<String>>
  ) {
    items.take(lookup.maxItems).forEach itemLoop@{ rawItem ->
      val item = rawItem as? Map<*, *> ?: return@itemLoop
      lookup.outputs.forEach { mapping ->
        val output = values.getOrPut(mapping.output, ::linkedSetOf)
        when (val value = item[mapping.field]) {
          is String -> value.takeIf(String::isNotBlank)?.let(output::add)
          is List<*> -> value.filterIsInstance<String>().filter(String::isNotBlank).forEach(output::add)
        }
      }
    }
  }

  private fun LibraryInsightDefinition.Field.toDisplayField(
    values: Map<String, LinkedHashSet<String>>,
    localeTag: String
  ): LibraryInsightField? {
    val sourceValues = values[source].orEmpty().toList()
    if (sourceValues.isEmpty()) return null
    return LibraryInsightField(
      label = label.localized(localeTag),
      values = sourceValues.take(maxValues),
      totalCount = sourceValues.size
    )
  }

  private fun Map<String, String>.localized(localeTag: String): String {
    val locale = Locale.forLanguageTag(localeTag.replace('_', '-'))
    val languageAndScript = resolveLanguageAndScript(locale)
    return this[locale.toLanguageTag()]
      ?: languageAndScript?.let(::get)
      ?: this[locale.language]
      ?: this[LibraryInsightDefinitionValidator.DEFAULT_LABEL]
      ?: values.first()
  }
}

private fun resolveLikelyLanguageAndScript(locale: Locale): String? {
  val likelyLocale = ULocale.addLikelySubtags(ULocale.forLocale(locale))
  return likelyLocale.script.takeIf(String::isNotBlank)?.let { "${likelyLocale.language}-$it" }
}
