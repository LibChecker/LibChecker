package com.absinthe.libchecker.database

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.absinthe.libchecker.database.entity.RuleEntity

@Dao
interface RuleDao {
    // Rules
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(items: List<RuleEntity>)

    @Query("DELETE FROM rules_table")
    fun deleteAllRules()

    @Query("SELECT * from rules_table WHERE name LIKE :name")
    suspend fun getRule(name: String): RuleEntity?

    @Query("SELECT * from rules_table")
    suspend fun getAllRules(): List<RuleEntity>

    @Query("SELECT * from rules_table WHERE isRegexRule = 1")
    suspend fun getRegexRules(): List<RuleEntity>

    @Query("SELECT * FROM rules_table")
    fun selectAllRules(): Cursor?

    @Query("SELECT * FROM rules_table WHERE name LIKE :name")
    fun selectRuleByName(name: String): Cursor?
}
