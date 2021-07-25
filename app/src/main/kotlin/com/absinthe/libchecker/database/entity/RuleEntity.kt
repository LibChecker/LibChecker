package com.absinthe.libchecker.database.entity

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.absinthe.libchecker.annotation.LibType

@Entity(tableName = "rules_table")
data class RuleEntity(
  @PrimaryKey @ColumnInfo(name = BaseColumns._ID) val id: Int,
  @ColumnInfo(name = "name") val name: String,
  @ColumnInfo(name = "label") val label: String,
  @ColumnInfo(name = "type") @LibType val type: Int,
  @ColumnInfo(name = "iconIndex") val iconIndex: Int,
  @ColumnInfo(name = "isRegexRule") val isRegexRule: Boolean,
  @ColumnInfo(name = "regexName") val regexName: String?
)
