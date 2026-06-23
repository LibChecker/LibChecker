package com.absinthe.libchecker.features.album.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.entity.TrackItem
import com.absinthe.libchecker.domain.snapshot.GetTrackListItemsUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TrackViewModel(
  private val snapshotRepository: SnapshotRepository,
  private val getTrackListItemsUseCase: GetTrackListItemsUseCase
) : ViewModel() {

  suspend fun getTrackListItems() = getTrackListItemsUseCase()

  fun setPackageTracked(packageName: String, tracked: Boolean) {
    GlobalValues.trackItemsChanged = true
    viewModelScope.launch(Dispatchers.IO) {
      val item = TrackItem(packageName)
      if (tracked) {
        snapshotRepository.insertTrackItem(item)
      } else {
        snapshotRepository.deleteTrackItem(item)
        snapshotRepository.deleteSnapshotDiff(packageName)
      }
    }
  }
}
