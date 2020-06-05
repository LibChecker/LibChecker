package com.absinthe.libchecker.provider

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri

class ContextProvider : ContentProvider() {

    companion object {
        private lateinit var mContext: Context
        private lateinit var mApplication: Application

        fun getGlobalContext(): Context = mContext
        fun getGlobalApplication(): Application = mApplication
    }

    override fun onCreate(): Boolean {
        mContext = context!!
        mApplication = context!!.applicationContext as Application
        return true
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = -1

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = -1
    override fun getType(uri: Uri): String? = null
}