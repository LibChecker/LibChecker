package com.absinthe.libchecker.domain.statistics.reference.repository

fun interface PermissionLabelResolver {
  fun resolve(permissionName: String): String?
}
