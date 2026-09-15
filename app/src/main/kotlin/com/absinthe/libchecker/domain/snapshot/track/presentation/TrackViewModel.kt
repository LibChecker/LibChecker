package com.absinthe.libchecker.domain.snapshot.track.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.domain.app.repository.PackageListLoadException
import com.absinthe.libchecker.domain.snapshot.track.model.TrackedAppListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class TrackViewModel(
  private val trackWorkflow: TrackWorkflow
) : ViewModel() {

  private val _uiState = MutableStateFlow(TrackListUiState())
  val uiState = _uiState.asStateFlow()

  private var loadJob: Job? = null
  private var allItems = emptyList<TrackedAppListItem>()
  private var query = ""
  private var isLoaded = false

  fun loadTrackList() {
    if (isLoaded || loadJob?.isActive == true) {
      return
    }

    loadJob = viewModelScope.launch(Dispatchers.IO) {
      allItems = try {
        trackWorkflow.getItems()
      } catch (e: PackageListLoadException) {
        Timber.w(e)
        _uiState.value = _uiState.value.copy(isLoading = false, loadFailed = true)
        return@launch
      }
      isLoaded = true
      _uiState.value = TrackListUiState(
        items = filterTrackItems(allItems, query),
        isLoading = false,
        isSearchVisible = true
      )
    }
  }

  fun setQuery(query: String) {
    this.query = query
    _uiState.value = _uiState.value.copy(
      items = filterTrackItems(allItems, query)
    )
  }

  fun setPackageTracked(packageName: String, tracked: Boolean) {
    allItems = updateTrackedState(allItems, packageName, tracked)
    _uiState.value = _uiState.value.copy(
      items = updateTrackedState(_uiState.value.items, packageName, tracked)
    )

    viewModelScope.launch(Dispatchers.IO) {
      trackWorkflow.setPackageTracked(packageName, tracked)
    }
  }

  private fun filterTrackItems(
    items: List<TrackedAppListItem>,
    query: String
  ): List<TrackedAppListItem> {
    return items.asSequence()
      .filter {
        it.label.contains(query, ignoreCase = true) || it.packageName.contains(query)
      }
      .sortedByDescending { it.switchState }
      .toList()
  }

  private fun updateTrackedState(
    items: List<TrackedAppListItem>,
    packageName: String,
    tracked: Boolean
  ): List<TrackedAppListItem> {
    return items.map {
      if (it.packageName == packageName) {
        it.copy(switchState = tracked)
      } else {
        it
      }
    }
  }
}

data class TrackListUiState(
  val items: List<TrackedAppListItem> = emptyList(),
  val isLoading: Boolean = true,
  val loadFailed: Boolean = false,
  val isSearchVisible: Boolean = false
)
