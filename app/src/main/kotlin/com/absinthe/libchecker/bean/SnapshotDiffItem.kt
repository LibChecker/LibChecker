package com.absinthe.libchecker.bean

data class SnapshotDiffItem(
    val packageName: String,
    val labelDiff: DiffNode<String>,
    val versionNameDiff: DiffNode<String>,
    val versionCodeDiff: DiffNode<Long>,
    val abiDiff: DiffNode<Short>,
    val nativeLibsDiff: DiffNode<String>,
    val servicesDiff: DiffNode<String>,
    val activitiesDiff: DiffNode<String>,
    val receiversDiff: DiffNode<String>,
    val providersDiff: DiffNode<String>
) {
    data class DiffNode<T>(val old: T, val new: T? = null)
}