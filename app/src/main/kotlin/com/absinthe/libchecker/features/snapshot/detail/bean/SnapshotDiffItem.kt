package com.absinthe.libchecker.features.snapshot.detail.bean

import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class SnapshotDiffItem(
  val packageName: String,
  val updateTime: Long,
  val labelDiff: DiffNode<String>,
  val versionNameDiff: DiffNode<String>,
  val versionCodeDiff: DiffNode<Long>,
  val abiDiff: DiffNode<Short>,
  val targetApiDiff: DiffNode<Short>,
  val compileSdkDiff: DiffNode<Short>,
  val minSdkDiff: DiffNode<Short>,
  val nativeLibsDiff: DiffNode<String>,
  val servicesDiff: DiffNode<String>,
  val activitiesDiff: DiffNode<String>,
  val receiversDiff: DiffNode<String>,
  val providersDiff: DiffNode<String>,
  val permissionsDiff: DiffNode<String>,
  val metadataDiff: DiffNode<String>,
  val packageSizeDiff: DiffNode<Long>,
  var added: Boolean = false,
  var removed: Boolean = false,
  var changed: Boolean = false,
  var moved: Boolean = false,
  var newInstalled: Boolean = false,
  var deleted: Boolean = false,
  var isTrackItem: Boolean = false
) : Serializable {
  @JsonClass(generateAdapter = true)
  data class DiffNode<T>(val old: T, val new: T? = null) : Serializable

  fun isNothingChanged() = !added && !removed && !changed && !moved
}
