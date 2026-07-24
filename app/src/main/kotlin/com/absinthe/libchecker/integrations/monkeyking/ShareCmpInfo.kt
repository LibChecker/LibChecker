package com.absinthe.libchecker.integrations.monkeyking

import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ShareCmpInfo(
  val pkg: String,
  val components: List<Component>
) {

  companion object {
    internal fun componentType(@LibType type: Int): String = when (type) {
      ACTIVITY -> TYPE_ACTIVITY
      SERVICE -> TYPE_SERVICE
      RECEIVER -> TYPE_RECEIVER
      PROVIDER -> TYPE_PROVIDER
      else -> throw IllegalStateException("wrong type")
    }
  }

  @JsonClass(generateAdapter = true)
  data class Component(
    val type: String,
    val name: String,
    val block: Boolean
  )
}
