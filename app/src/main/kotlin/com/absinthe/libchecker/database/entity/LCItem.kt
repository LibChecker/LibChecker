package com.absinthe.libchecker.database.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "item_table")
data class LCItem(
  @PrimaryKey val packageName: String,
  @ColumnInfo val label: String,
  @ColumnInfo val versionName: String,
  @ColumnInfo val versionCode: Long,
  @ColumnInfo val installedTime: Long,
  @ColumnInfo val lastUpdatedTime: Long,
  @ColumnInfo val isSystem: Boolean,
  @ColumnInfo val abi: Short,
  @ColumnInfo val isSplitApk: Boolean,
  @ColumnInfo val isKotlinUsed: Boolean?,
  @ColumnInfo val isRxJavaUsed: Boolean?,
  @ColumnInfo val isRxKotlinUsed: Boolean?,
  @ColumnInfo val targetApi: Short,
  @ColumnInfo val variant: Short
) : Parcelable
