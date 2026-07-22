package com.absinthe.libchecker.domain.app.detail.insight

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LibraryInsightCatalog(
  @Json(name = "schema_version") val schemaVersion: Int,
  val entries: List<Entry>
) {
  @JsonClass(generateAdapter = true)
  data class Entry(
    @Json(name = "sdk_id") val sdkId: String,
    @Json(name = "library_uuids") val libraryUuids: List<String>,
    val definition: String
  )
}

@JsonClass(generateAdapter = true)
data class LibraryInsightDefinition(
  @Json(name = "schema_version") val schemaVersion: Int,
  @Json(name = "sdk_id") val sdkId: String,
  @Json(name = "target_uuids") val targetUuids: List<String>,
  val probes: List<Probe>,
  val lookups: List<Lookup> = emptyList(),
  val presentation: Presentation
) {
  @JsonClass(generateAdapter = true)
  data class Probe(
    val id: String,
    val source: Source,
    val reader: Reader,
    val captures: List<Capture>
  )

  @JsonClass(generateAdapter = true)
  data class Source(
    val operator: String,
    @Json(name = "file_name") val fileName: String,
    @Json(name = "archive_paths") val archivePaths: List<String>
  )

  @JsonClass(generateAdapter = true)
  data class Reader(
    val operator: String,
    @Json(name = "max_bytes_per_file") val maxBytesPerFile: Long,
    @Json(name = "max_total_bytes") val maxTotalBytes: Long
  )

  @JsonClass(generateAdapter = true)
  data class Capture(
    val output: String,
    val type: String,
    @Json(name = "max_results") val maxResults: Int
  )

  @JsonClass(generateAdapter = true)
  data class Lookup(
    val input: String,
    @Json(name = "path_template") val pathTemplate: String,
    @Json(name = "expected_field") val expectedField: String? = null,
    @Json(name = "items_field") val itemsField: String? = null,
    @Json(name = "max_requests") val maxRequests: Int,
    @Json(name = "max_items") val maxItems: Int,
    val outputs: List<Output>
  )

  @JsonClass(generateAdapter = true)
  data class Output(
    val output: String,
    val field: String
  )

  @JsonClass(generateAdapter = true)
  data class Presentation(
    val summary: List<Field>,
    val details: List<Field> = emptyList()
  )

  @JsonClass(generateAdapter = true)
  data class Field(
    val label: Map<String, String>,
    val source: String,
    @Json(name = "max_values") val maxValues: Int
  )
}

sealed interface RemoteDocumentResult<out T> {
  data class Success<T>(val value: T) : RemoteDocumentResult<T>
  data object NotFound : RemoteDocumentResult<Nothing>
  data object Failure : RemoteDocumentResult<Nothing>
}

data class LibraryInsightProbeResult(
  val evidenceFound: Boolean,
  val values: Map<String, List<String>>
)

data class LibraryInsightField(
  val label: String,
  val values: List<String>,
  val totalCount: Int
)

data class LibraryInsightContent(
  val sdkId: String,
  val summary: List<LibraryInsightField>,
  val details: List<LibraryInsightField>
)

sealed interface LibraryInsightResult {
  data object NotSupported : LibraryInsightResult
  data object Unavailable : LibraryInsightResult
  data class Content(val content: LibraryInsightContent) : LibraryInsightResult
}

sealed interface LibraryInsightUiState {
  data object Hidden : LibraryInsightUiState
  data object Loading : LibraryInsightUiState
  data object Unavailable : LibraryInsightUiState
  data class Content(val content: LibraryInsightContent) : LibraryInsightUiState
}
