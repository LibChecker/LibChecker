package com.absinthe.libchecker.features.album.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.domain.snapshot.GetTrackListItemsUseCase
import com.absinthe.libchecker.domain.snapshot.SetPackageTrackedUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TrackViewModel(
  private val getTrackListItemsUseCase: GetTrackListItemsUseCase,
  private val setPackageTrackedUseCase: SetPackageTrackedUseCase
) : ViewModel() {

  suspend fun getTrackListItems() = getTrackListItemsUseCase()

  fun setPackageTracked(packageName: String, tracked: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      setPackageTrackedUseCase(packageName, tracked)
    }
  }
}
