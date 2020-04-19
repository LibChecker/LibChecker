package com.absinthe.libchecker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "native_lib_table")
data class NativeLibItem(
    @PrimaryKey @ColumnInfo(name = "libName") val libName: String,
    @ColumnInfo(name = "count") val count: Int
)