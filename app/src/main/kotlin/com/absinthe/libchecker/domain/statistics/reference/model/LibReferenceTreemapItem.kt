package com.absinthe.libchecker.domain.statistics.reference.model

data class LibReferenceTreemapItem(
  val id: String,
  val reference: LibReference?,
  val count: Long,
  val libraryCount: Int = 1
)

fun buildLibReferenceTreemapData(references: List<LibReference>): List<LibReferenceTreemapItem> {
  val items = ArrayList<LibReferenceTreemapItem>()
  var otherCount = 0L
  var otherLibraries = 0
  references.forEach { reference ->
    val count = reference.referredList.size
    if (count <= 5) {
      otherCount += count
      otherLibraries++
    } else {
      items += LibReferenceTreemapItem("${reference.type}:${reference.libName}", reference, count.toLong())
    }
  }
  if (otherLibraries > 0) {
    items += LibReferenceTreemapItem("other", null, otherCount, otherLibraries)
  }
  return items
}
