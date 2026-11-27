package com.absinthe.libchecker.domain.app.detail.presentation

import android.content.pm.PackageInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightRepository
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
  private val resolveLibraryInsight: ResolveLibraryInsightUseCase,
  private val repository: LibraryInsightRepository
) : ViewModel() {

  private val _state = MutableStateFlow<LibraryInsightUiState>(LibraryInsightUiState.Hidden)
  val state: StateFlow<LibraryInsightUiState> = _state.asStateFlow()
  private var request: Request? = null
  private var job: Job? = null
  private var preloadJob: Job? = null

  fun preload() {
    if (preloadJob == null) {
      preloadJob = viewModelScope.launch { repository.getCatalog() }
    }
  }

  fun load(libraryUuid: String, packageInfo: PackageInfo, localeTag: String) {
    resolve(Request(libraryUuid, packageInfo, localeTag, nativeLibrary = false))
  }

  fun loadNativeLibrary(name: String, packageInfo: PackageInfo, localeTag: String) {
    resolve(Request(name, packageInfo, localeTag, nativeLibrary = true))
  }

  fun retry() {
    request?.let(::resolve)
  }

  private fun resolve(request: Request) {
    this.request = request
    job?.cancel()
    _state.value = LibraryInsightUiState.Hidden
    job = viewModelScope.launch {
      var supported = false
      try {
        preloadJob?.join()
        val result = if (request.nativeLibrary) {
          resolveLibraryInsight.resolveNativeLibrary(request.key, request.packageInfo, request.localeTag)
        } else {
          resolveLibraryInsight(request.key, request.packageInfo, request.localeTag) {
            supported = true
            _state.value = LibraryInsightUiState.Loading
          }
        }
        when (result) {
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
    val key: String,
    val packageInfo: PackageInfo,
    val localeTag: String,
    val nativeLibrary: Boolean
  )
}
