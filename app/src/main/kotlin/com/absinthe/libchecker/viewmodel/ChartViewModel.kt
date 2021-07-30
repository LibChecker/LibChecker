package com.absinthe.libchecker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.LCItem

class ChartViewModel(application: Application) : AndroidViewModel(application) {
  val dbItems: LiveData<List<LCItem>> = Repositories.lcRepository.allDatabaseItems
  val filteredList: MutableLiveData<List<LCItem>> = MutableLiveData()
  val dialogTitle: MutableLiveData<String> = MutableLiveData()
}
