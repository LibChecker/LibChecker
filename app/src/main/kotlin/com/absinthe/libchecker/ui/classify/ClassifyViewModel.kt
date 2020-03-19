package com.absinthe.libchecker.ui.classify

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ClassifyViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Under construction"
    }
    val text: LiveData<String> = _text
}