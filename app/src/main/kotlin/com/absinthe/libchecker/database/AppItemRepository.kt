package com.absinthe.libchecker.database

import androidx.lifecycle.MutableLiveData
import com.absinthe.libchecker.bean.AppItem

object AppItemRepository {
    val allItems: MutableLiveData<List<AppItem>> = MutableLiveData()
}