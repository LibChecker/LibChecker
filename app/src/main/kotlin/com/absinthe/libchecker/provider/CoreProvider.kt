package com.absinthe.libchecker.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import com.absinthe.libchecker.database.LCDatabase

class CoreProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        return false
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        if (context == null) {
            return null
        }
        val builder = SQLiteQueryBuilder()
        builder.tables = "rules_table"
        val query = builder.buildQuery(projection, selection, null, null, sortOrder, null)
        val cursor = LCDatabase.getDatabase(context!!)
            .openHelper
            .writableDatabase
            .query(query, selectionArgs)
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String {
        return "vnd.android.cursor.item/vnd.com.absinthe.libchecker.provider.rules_table"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }
}