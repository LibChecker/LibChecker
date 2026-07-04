package com.absinthe.libchecker.domain.snapshot.list.model

data class SnapshotSystemPropDisplayData(
  val label: String,
  val displayValue: String,
  val description: String
)

fun buildSnapshotSystemPropDisplayData(
  label: String,
  displayValue: String
): SnapshotSystemPropDisplayData {
  return SnapshotSystemPropDisplayData(
    label = label,
    displayValue = displayValue,
    description = listOf(label, displayValue)
      .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
      .joinToString()
  )
}
