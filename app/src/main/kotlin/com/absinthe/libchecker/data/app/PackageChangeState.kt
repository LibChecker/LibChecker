package com.absinthe.libchecker.data.app

sealed class PackageChangeState {
  abstract val packageName: String

  data class Added(override val packageName: String) : PackageChangeState()
  data class Removed(override val packageName: String) : PackageChangeState()
  data class Replaced(override val packageName: String) : PackageChangeState()
}
