package com.absinthe.libchecker.domain.snapshot.timenode.model

import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libchecker.utils.toJson

object SnapshotRepresentativeApps {

  fun encode(packageNames: List<String>): String {
    return FORMAT_PREFIX + packageNames.toJson()
  }

  fun decode(value: String?): List<String> {
    val json = value
      ?.removePrefix(FORMAT_PREFIX)
      ?: return emptyList()
    return json.fromJson<List<String>>(List::class.java, String::class.java).orEmpty()
  }

  fun needsRefresh(value: String?): Boolean {
    return value?.startsWith(FORMAT_PREFIX) != true
  }

  private const val FORMAT_PREFIX = "representative:v1:"
}
