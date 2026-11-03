package com.absinthe.libchecker.domain.app.detail.feature

internal fun formatR8Versions(versions: Collection<String>): String {
  return versions.sortedWith { left, right ->
    val a = left.substringBefore(' ').split('-', limit = 2)
    val b = right.substringBefore(' ').split('-', limit = 2)
    val aParts = a[0].split('.')
    val bParts = b[0].split('.')
    var comparison = 0
    for (index in 0 until maxOf(aParts.size, bParts.size)) {
      val x = aParts.getOrElse(index) { "0" }
      val y = bParts.getOrElse(index) { "0" }
      val xNumber = x.toBigIntegerOrNull()
      val yNumber = y.toBigIntegerOrNull()
      comparison = if (xNumber != null && yNumber != null) yNumber.compareTo(xNumber) else y.compareTo(x)
      if (comparison != 0) break
    }
    if (comparison != 0) comparison else comparePrereleases(a.getOrNull(1), b.getOrNull(1))
  }.joinToString(" / ")
}

private val prereleaseTokens = Regex("[0-9]+|[^0-9]+")
private val prereleaseStages = listOf("dev", "alpha", "beta", "rc")

private fun comparePrereleases(left: String?, right: String?): Int {
  if (left == null || right == null) return compareValues(left, right)
  val a = prereleaseTokens.findAll(left).map { it.value }.toList()
  val b = prereleaseTokens.findAll(right).map { it.value }.toList()
  for (index in 0 until minOf(a.size, b.size)) {
    val x = a[index]
    val y = b[index]
    val xNumber = x.toBigIntegerOrNull()
    val yNumber = y.toBigIntegerOrNull()
    val comparison = when {
      xNumber != null && yNumber != null -> yNumber.compareTo(xNumber)

      index == 0 -> prereleaseStages.indexOf(y).compareTo(prereleaseStages.indexOf(x))
        .takeIf { it != 0 } ?: y.compareTo(x)

      else -> y.compareTo(x)
    }
    if (comparison != 0) return comparison
  }
  return b.size.compareTo(a.size)
}
