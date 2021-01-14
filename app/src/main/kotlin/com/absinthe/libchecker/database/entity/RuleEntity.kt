package com.absinthe.libchecker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules_table")
data class RuleEntity(
    @PrimaryKey @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "iconIndex") val iconIndex: Int,
    @ColumnInfo(name = "isRegexRule") val isRegexRule: Boolean
)