package com.absinthe.libchecker.features.album.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.database.entity.TrackItem
import com.absinthe.libchecker.domain.snapshot.GetTrackListItemsUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotTrackChangeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TrackViewModel(
  private val snapshotRepository: SnapshotRepository,
  private val getTrackListItemsUseCase: GetTrackListItemsUseCase,
  private val snapshotTrackChangeRepository: SnapshotTrackChangeRepository
) : ViewModel() {

  suspend fun getTrackListItems() = getTrackListItemsUseCase()

  fun setPackageTracked(packageName: String, tracked: Boolean) {
    snapshotTrackChangeRepository.markChanged()
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
