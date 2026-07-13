package com.absinthe.libchecker.domain.app.detail.insight

import android.content.pm.PackageInfo
import android.icu.util.ULocale
import java.util.Locale

class ResolveLibraryInsightUseCase(
  private val repository: LibraryInsightRepository,
  private val validator: LibraryInsightDefinitionValidator,
  private val probeEngine: LibraryInsightProbeEngine
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
    if (catalog.schemaVersion != LibraryInsightDefinitionValidator.SUPPORTED_SCHEMA_VERSION) {
      return LibraryInsightResult.NotSupported
    }
    val entry = catalog.entries.firstOrNull { libraryUuid in it.libraryUuids }
      ?: return LibraryInsightResult.NotSupported
    if (!validator.isSafeRemotePath(entry.definition)) return LibraryInsightResult.NotSupported

    onSupported()
    val definition = when (val result = repository.getDefinition(entry.definition)) {
      is RemoteDocumentResult.Success -> result.value

      RemoteDocumentResult.Failure,
      RemoteDocumentResult.NotFound -> return LibraryInsightResult.Unavailable
    }
    if (!validator.isValid(definition, entry.sdkId, libraryUuid)) {
      return LibraryInsightResult.Unavailable
    }

    val probeResult = probeEngine.probe(packageInfo, definition)
    if (!probeResult.evidenceFound) return LibraryInsightResult.NotSupported
    val values = linkedMapOf<String, LinkedHashSet<String>>()
    probeResult.values.forEach { (key, result) -> values.getOrPut(key, ::linkedSetOf).addAll(result) }
    resolveLookups(definition.lookups, values)

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
    values: MutableMap<String, LinkedHashSet<String>>
  ) {
    lookups.forEach { lookup ->
      values[lookup.input]
        .orEmpty()
        .asSequence()
        .filter(LibraryInsightDefinitionValidator.LOOKUP_VALUE::matches)
        .take(lookup.maxRequests)
        .forEach { input ->
          val path = lookup.pathTemplate.replace(LibraryInsightDefinitionValidator.VALUE_PLACEHOLDER, input)
          if (!validator.isSafeRemotePath(path)) return@forEach
          val document = (repository.getLookup(path) as? RemoteDocumentResult.Success)?.value ?: return@forEach
          if (lookup.expectedField != null && document[lookup.expectedField] != input) return@forEach
          val items = lookup.itemsField?.let { document[it] as? List<*> } ?: listOf(document)
          items.asSequence().take(lookup.maxItems).forEach { rawItem ->
            val item = rawItem as? Map<*, *> ?: return@forEach
            lookup.outputs.forEach { mapping ->
              val output = values.getOrPut(mapping.output, ::linkedSetOf)
              when (val value = item[mapping.field]) {
                is String -> value.takeIf(String::isNotBlank)?.let(output::add)
                is List<*> -> value.filterIsInstance<String>().filter(String::isNotBlank).forEach(output::add)
              }
            }
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
    val likelyLocale = ULocale.addLikelySubtags(ULocale.forLocale(locale))
    val languageAndScript = likelyLocale.script.takeIf(String::isNotBlank)?.let { "${likelyLocale.language}-$it" }
    return this[locale.toLanguageTag()]
      ?: languageAndScript?.let(::get)
      ?: this[locale.language]
      ?: this[LibraryInsightDefinitionValidator.DEFAULT_LABEL]
      ?: values.first()
  }
}
