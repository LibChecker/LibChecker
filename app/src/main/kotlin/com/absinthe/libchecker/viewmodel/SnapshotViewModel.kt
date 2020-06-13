package com.absinthe.libchecker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SnapshotViewModel(application: Application) :AndroidViewModel(application) {

    fun computeSnapshots() = viewModelScope.launch(Dispatchers.IO) {
        
    }

}