package com.absinthe.libchecker.features.chart

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.features.chart.impl.MarketDistributionChartDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChartViewModel : ViewModel() {
  val dbItems: LiveData<List<LCItem>> = Repositories.lcRepository.allDatabaseItems
  val filteredList: MutableLiveData<List<LCItem>> = MutableLiveData()
  val dialogTitle: MutableLiveData<String> = MutableLiveData()
  val androidVersion: MutableLiveData<Triple<Int, String, Int?>?> = MutableLiveData()
  private var queryJob: Job? = null

  private val _isLoading = MutableStateFlow(false)
  val isLoading = _isLoading.asStateFlow()

  private val _distributionLastUpdateTime = MutableStateFlow("")
  val distributionLastUpdateTime = _distributionLastUpdateTime.asStateFlow()

  fun setLoading(loading: Boolean) {
    _isLoading.value = loading
  }

  fun <T : View> applyChartData(
    root: ViewGroup,
    currentChartView: View?,
    newChartView: T,
    source: IChartDataSource<T>
  ) {
    queryJob?.cancel()
    queryJob = viewModelScope.launch(Dispatchers.Default) {
      source.fillChartView(newChartView)

      withContext(Dispatchers.Main) {
        if (currentChartView != null) {
          root.removeView(currentChartView)
        }
        root.addView(newChartView)
        if (dbItems.value?.isNotEmpty() == true) {
          setLoading(false)
        }
        if (source is MarketDistributionChartDataSource) {
          _distributionLastUpdateTime.value = source.distribution
            ?.get(0)
            ?.descriptionBlocks
            ?.find { it.title.isEmpty() }
            ?.body
            ?.removePrefix("Last updated: ")
            .orEmpty()
        }
      }
    }
  }
}
