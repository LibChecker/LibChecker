package com.absinthe.libchecker.domain.app.detail.insight

class LibraryInsightDefinitionValidator {

  fun isValid(
    definition: LibraryInsightDefinition,
    expectedSdkId: String,
    expectedUuid: String
  ): Boolean {
    if (definition.schemaVersion != SUPPORTED_SCHEMA_VERSION) return false
    if (definition.sdkId != expectedSdkId || !SDK_ID.matches(definition.sdkId)) return false
    if (definition.targetUuids.isEmpty() || definition.targetUuids.size > MAX_TARGET_UUIDS) return false
    if (definition.targetUuids.distinct().size != definition.targetUuids.size) return false
    if (expectedUuid !in definition.targetUuids || definition.targetUuids.any { !UUID.matches(it) }) return false
    if (definition.probes.isEmpty() || definition.probes.size > MAX_PROBES) return false

    val outputs = mutableSetOf<String>()
    definition.probes.forEach { probe ->
      if (!SDK_ID.matches(probe.id)) return false
      if (probe.source.operator != SOURCE_PACKAGE_FILE) return false
      if (!isSafeFileName(probe.source.fileName)) return false
      if (probe.source.archivePaths.isEmpty() || probe.source.archivePaths.size > MAX_ARCHIVE_PATHS) return false
      if (probe.source.archivePaths.any { !isSafeArchivePath(it) }) return false
      if (probe.reader.operator != READER_ASCII_STRINGS) return false
      if (probe.reader.maxBytesPerFile !in 1..MAX_BYTES_PER_FILE) return false
      if (probe.reader.maxTotalBytes !in 1..MAX_TOTAL_BYTES) return false
      if (probe.captures.isEmpty() || probe.captures.size > MAX_CAPTURES) return false
      probe.captures.forEach { capture ->
        if (!SDK_ID.matches(capture.output)) return false
        if (capture.type !in CAPTURE_TYPES) return false
        if (capture.maxResults !in 1..MAX_CAPTURE_RESULTS) return false
        outputs += capture.output
      }
    }

    if (definition.lookups.size > MAX_LOOKUPS) return false
    definition.lookups.forEach { lookup ->
      if (lookup.input !in outputs) return false
      if (!isSafeRemotePath(lookup.pathTemplate) || lookup.pathTemplate.countValuePlaceholder() != 1) return false
      if (lookup.maxRequests !in 1..MAX_LOOKUP_REQUESTS) return false
      if (lookup.maxItems !in 1..MAX_LOOKUP_ITEMS) return false
      if (lookup.expectedField?.let(SDK_ID::matches) == false) return false
      if (lookup.itemsField?.let(SDK_ID::matches) == false) return false
      if (lookup.outputs.isEmpty() || lookup.outputs.size > MAX_LOOKUP_OUTPUTS) return false
      lookup.outputs.forEach { mapping ->
        if (!SDK_ID.matches(mapping.output) || !SDK_ID.matches(mapping.field)) return false
        outputs += mapping.output
      }
    }

    val summary = definition.presentation.summary
    val details = definition.presentation.details
    if (summary.isEmpty() || summary.size > MAX_SUMMARY_FIELDS || details.size > MAX_DETAIL_FIELDS) return false
    return (summary + details).all { field ->
      field.source in outputs &&
        field.maxValues in 1..MAX_PRESENTATION_VALUES &&
        field.label[DEFAULT_LABEL]?.isNotBlank() == true &&
        field.label.size <= MAX_LABELS &&
        field.label.values.all { it.isNotBlank() && it.length <= MAX_LABEL_LENGTH }
    }
  }

  fun isSafeRemotePath(path: String): Boolean {
    return path.startsWith(SDK_DETAILS_PREFIX) &&
      !path.startsWith('/') &&
      !path.contains("..") &&
      !path.contains('\\') &&
      !path.contains("://")
  }

  private fun isSafeFileName(value: String): Boolean {
    return value.isNotBlank() && value.length <= MAX_FILE_NAME_LENGTH && '/' !in value && '\\' !in value
  }

  private fun isSafeArchivePath(value: String): Boolean {
    return value.isNotBlank() &&
      value.length <= MAX_ARCHIVE_PATH_LENGTH &&
      !value.startsWith('/') &&
      !value.contains("..") &&
      !value.contains('\\')
  }

  private fun String.countValuePlaceholder(): Int = windowed(VALUE_PLACEHOLDER.length)
    .count { it == VALUE_PLACEHOLDER }

  companion object {
    const val SUPPORTED_SCHEMA_VERSION = 1
    const val SOURCE_PACKAGE_FILE = "package_file"
    const val READER_ASCII_STRINGS = "ascii_strings"
    const val CAPTURE_SHA1 = "sha1"
    const val CAPTURE_SEMVER_CHANNEL = "semver_channel"
    const val VALUE_PLACEHOLDER = "{value}"
    const val DEFAULT_LABEL = "default"
    const val MAX_BYTES_PER_FILE = 64L * 1024 * 1024
    const val MAX_TOTAL_BYTES = 128L * 1024 * 1024
    const val MAX_TOKEN_LENGTH = 256
    const val SDK_DETAILS_PREFIX = "sdk-details/"
    val LOOKUP_VALUE = Regex("^[A-Za-z0-9._-]{1,128}$")

    private val SDK_ID = Regex("^[a-z0-9][a-z0-9_-]{0,63}$")
    private val UUID = Regex("^[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}$")
    private val CAPTURE_TYPES = setOf(CAPTURE_SHA1, CAPTURE_SEMVER_CHANNEL)
    private const val MAX_TARGET_UUIDS = 32
    private const val MAX_PROBES = 8
    private const val MAX_ARCHIVE_PATHS = 16
    private const val MAX_CAPTURES = 8
    private const val MAX_CAPTURE_RESULTS = 16
    private const val MAX_LOOKUPS = 8
    private const val MAX_LOOKUP_REQUESTS = 4
    private const val MAX_LOOKUP_ITEMS = 50
    private const val MAX_LOOKUP_OUTPUTS = 16
    private const val MAX_SUMMARY_FIELDS = 2
    private const val MAX_DETAIL_FIELDS = 8
    private const val MAX_PRESENTATION_VALUES = 8
    private const val MAX_LABELS = 16
    private const val MAX_LABEL_LENGTH = 80
    private const val MAX_FILE_NAME_LENGTH = 128
    private const val MAX_ARCHIVE_PATH_LENGTH = 256
  }
}
