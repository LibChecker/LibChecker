package com.absinthe.libchecker.database.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "item_table")
data class LCItem(
  @PrimaryKey val packageName: String,
  val label: String,
  val versionName: String,
  val versionCode: Long,
  val installedTime: Long,
  val lastUpdatedTime: Long,
  val isSystem: Boolean,
  val abi: Short,
  val features: Int,
  val targetApi: Short,
  val variant: Short
) : Parcelable
