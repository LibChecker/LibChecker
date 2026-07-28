package com.absinthe.libchecker.domain.app.detail.presentation

import android.content.pm.PackageInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightResult
import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightUiState
import com.absinthe.libchecker.domain.app.detail.insight.ResolveLibraryInsightUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibraryInsightViewModel(
  private val resolveLibraryInsight: ResolveLibraryInsightUseCase
) : ViewModel() {

  private val _state = MutableStateFlow<LibraryInsightUiState>(LibraryInsightUiState.Hidden)
  val state: StateFlow<LibraryInsightUiState> = _state.asStateFlow()
  private var request: Request? = null
  private var job: Job? = null

  fun load(libraryUuid: String, packageInfo: PackageInfo, localeTag: String) {
    request = Request(libraryUuid, packageInfo, localeTag)
    resolve(request ?: return)
  }

  fun retry() {
    request?.let(::resolve)
  }

  private fun resolve(request: Request) {
    job?.cancel()
    _state.value = LibraryInsightUiState.Hidden
    job = viewModelScope.launch {
      var supported = false
      try {
        when (
          val result = resolveLibraryInsight(
            libraryUuid = request.libraryUuid,
            packageInfo = request.packageInfo,
            localeTag = request.localeTag,
            onSupported = {
              supported = true
              _state.value = LibraryInsightUiState.Loading
            }
          )
        ) {
          is LibraryInsightResult.Content -> _state.value = LibraryInsightUiState.Content(result.content)
          LibraryInsightResult.NotSupported -> _state.value = LibraryInsightUiState.Hidden
          LibraryInsightResult.Unavailable -> _state.value = LibraryInsightUiState.Unavailable
        }
      } catch (exception: CancellationException) {
        throw exception
      } catch (_: Exception) {
        _state.value = if (supported) LibraryInsightUiState.Unavailable else LibraryInsightUiState.Hidden
      }
    }
  }

  private data class Request(
    val libraryUuid: String,
    val packageInfo: PackageInfo,
    val localeTag: String
  )
}
