package com.absinthe.libchecker.api.bean

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AndroidDistribution(
  val name: String,
  val version: String,
  val apiLevel: Int,
  val distributionPercentage: Double,
  val url: String,
  val descriptionBlocks: List<DescriptionBlock>
) {
  @JsonClass(generateAdapter = true)
  data class DescriptionBlock(val title: String, val body: String)
}
