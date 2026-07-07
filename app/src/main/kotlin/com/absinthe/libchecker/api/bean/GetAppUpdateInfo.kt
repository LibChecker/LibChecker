package com.absinthe.libchecker.api.bean

import com.squareup.moshi.JsonClass

private const val FLAVOR_FOSS = "foss"
private const val FLAVOR_MARKET = "market"

@JsonClass(generateAdapter = true)
data class GetAppUpdateInfo(
  val app: App,
  val flavors: Map<String, App>? = null
) {
  fun appForFlavor(isFoss: Boolean): App? {
    return if (isFoss) {
      flavors?.get(FLAVOR_FOSS) ?: app
    } else {
      flavors?.get(FLAVOR_MARKET)
    }
  }

  @JsonClass(generateAdapter = true)
  data class App(
    val version: String,
    val versionCode: Int,
    val extra: Extra,
    val link: String,
    val note: String?
  ) {
    @JsonClass(generateAdapter = true)
    data class Extra(
      val target: Int,
      val min: Int,
      val compile: Int,
      val packageSize: Int
    )
  }
}
