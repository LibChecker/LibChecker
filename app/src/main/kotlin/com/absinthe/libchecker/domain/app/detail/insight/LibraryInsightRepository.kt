package com.absinthe.libchecker.domain.app.detail.insight

interface LibraryInsightRepository {
  suspend fun getCatalog(): RemoteDocumentResult<LibraryInsightCatalog>

  suspend fun getDefinition(path: String): RemoteDocumentResult<LibraryInsightDefinition>

  suspend fun getLookup(path: String): RemoteDocumentResult<Map<String, Any?>>
}
