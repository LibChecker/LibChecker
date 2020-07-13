package com.absinthe.libchecker.bean

import java.io.Serializable

data class SnapshotDiffItem(
    val packageName: String,
    val updateTime: Long,
    val labelDiff: DiffNode<String>,
    val versionNameDiff: DiffNode<String>,
    val versionCodeDiff: DiffNode<Long>,
    val abiDiff: DiffNode<Short>,
    val targetApiDiff: DiffNode<Short>,
    val nativeLibsDiff: DiffNode<String>,
    val servicesDiff: DiffNode<String>,
    val activitiesDiff: DiffNode<String>,
    val receiversDiff: DiffNode<String>,
    val providersDiff: DiffNode<String>,
    val added: Boolean = false,
    val removed: Boolean = false,
    val changed: Boolean = false,
    val moved: Boolean = false,
    val newInstalled: Boolean = false,
    val deleted: Boolean = false
) : Serializable {
    data class DiffNode<T>(val old: T, val new: T? = null) : Serializable
}