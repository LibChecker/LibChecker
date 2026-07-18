package com.absinthe.libchecker.data.statistics

import android.content.SharedPreferences
import com.absinthe.libchecker.utils.JsonUtil
import com.squareup.moshi.Types

interface StatisticSelectionStore {
  fun getSelectedStatisticIds(): List<String>?

  fun setSelectedStatisticIds(ids: List<String>)
}

class SharedPreferencesStatisticSelectionStore(
  private val preferences: SharedPreferences
) : StatisticSelectionStore {

  private val adapter = JsonUtil.moshi.adapter<List<String>>(
    Types.newParameterizedType(List::class.java, String::class.java)
  )

  override fun getSelectedStatisticIds(): List<String>? {
    val serializedIds = preferences.getString(PREFERENCE_KEY, null) ?: return null
    return runCatching { adapter.fromJson(serializedIds) }
      .getOrNull()
      ?.distinct()
  }

  override fun setSelectedStatisticIds(ids: List<String>) {
    preferences.edit()
      .putString(PREFERENCE_KEY, adapter.toJson(ids.distinct()))
      .apply()
  }

  private companion object {
    const val PREFERENCE_KEY = "selectedStatisticIdsV1"
  }
}
