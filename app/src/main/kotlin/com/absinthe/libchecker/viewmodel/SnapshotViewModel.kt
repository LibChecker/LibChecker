package com.absinthe.libchecker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.LCDatabase
import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.database.SnapshotItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SnapshotViewModel(application: Application) : AndroidViewModel(application) {

    val timestamp: MutableLiveData<Long> = MutableLiveData(GlobalValues.snapshotTimestamp)
    val snapshotItems: LiveData<List<SnapshotItem>>

    private val repository: LCRepository

    init {
        val lcDao = LCDatabase.getDatabase(application).lcDao()
        repository = LCRepository(lcDao)
        snapshotItems = lcDao.getSnapshots()
    }

    fun computeSnapshots() = viewModelScope.launch(Dispatchers.IO) {

        deleteAllSnapshots()
        val list = mutableListOf<SnapshotItem>()

        list.add(
            SnapshotItem(
                BuildConfig.APPLICATION_ID,
                "LibChecker",
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE.toLong(),
                0L, 0L,
                false,
                0,
                "", "", "", "", ""
            )
        )

        insertSnapshots(list)
        withContext(Dispatchers.Main) {
            val ts = System.currentTimeMillis()
            GlobalValues.snapshotTimestamp = ts
            timestamp.value = ts
        }
    }

    fun insertSnapshots(items: List<SnapshotItem>) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertSnapshots(items)
    }

    fun deleteAllSnapshots() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAllSnapshots()
    }
}