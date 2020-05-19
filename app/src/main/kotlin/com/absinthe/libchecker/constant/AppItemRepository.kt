package com.absinthe.libchecker.constant

import androidx.lifecycle.MutableLiveData
import com.absinthe.libchecker.bean.AppItem

object AppItemRepository {
    val allItems: MutableLiveData<List<AppItem>> = MutableLiveData()
}