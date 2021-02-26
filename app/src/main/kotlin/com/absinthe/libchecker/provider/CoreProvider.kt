package com.absinthe.libchecker.provider

import android.content.*
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import androidx.annotation.Nullable
import com.absinthe.libchecker.database.LCDatabase
import java.util.concurrent.Callable

class CoreProvider : ContentProvider() {

    companion object {
        /** The authority of this content provider.  */
        const val AUTHORITY = "com.absinthe.libchecker.coreprovider"

        /** The URI for the rules table.  */
        val URI_MENU = Uri.parse(
            "content://$AUTHORITY/rules_table"
        )

        /**The match code for some items in the rules table.  */
        private const val CODE_RULES_DIR = 1

        /** The match code for an item in the rules table.  */
        private const val CODE_RULES_ITEM = 2

        /** The URI matcher.  */
        private val MATCHER = UriMatcher(UriMatcher.NO_MATCH)

        init {
            MATCHER.addURI(
                AUTHORITY,
                "rules_table",
                CODE_RULES_DIR
            )
            MATCHER.addURI(
                AUTHORITY,
                "rules_table/*",
                CODE_RULES_ITEM
            )
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        val context = context ?: return null
        val builder = SQLiteQueryBuilder().apply { tables = "rules_table" }
        val query = builder.buildQuery(projection, selection, null, null, sortOrder, null)
        val cursor = LCDatabase.getDatabase(context)
            .openHelper
            .writableDatabase
            .query(query, selectionArgs)
        cursor.setNotificationUri(context.contentResolver, uri)
        return cursor
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

    @Nullable
    override fun getType(uri: Uri): String? {
        return when (MATCHER.match(uri)) {
            CODE_RULES_DIR -> "vnd.android.cursor.dir/${AUTHORITY}.rules_table"
            CODE_RULES_ITEM -> "vnd.android.cursor.item/${AUTHORITY}.rules_table"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    @Throws(OperationApplicationException::class)
    override fun applyBatch(operations: ArrayList<ContentProviderOperation?>): Array<ContentProviderResult?> {
        val context = context ?: return arrayOfNulls(0)
        val database: LCDatabase = LCDatabase.getDatabase(context)
        return database.runInTransaction(Callable<Array<ContentProviderResult?>> { super.applyBatch(operations) })
    }
}