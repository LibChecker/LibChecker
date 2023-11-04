package com.absinthe.libchecker.features.chart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.LCItem

class ChartViewModel : ViewModel() {
  val dbItems: LiveData<List<LCItem>> = Repositories.lcRepository.allDatabaseItems
  val filteredList: MutableLiveData<List<LCItem>> = MutableLiveData()
  val dialogTitle: MutableLiveData<String> = MutableLiveData()
  val androidVersion: MutableLiveData<Triple<Int, String, Int?>?> = MutableLiveData()
}
