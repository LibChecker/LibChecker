package com.absinthe.libchecker.provider

import android.content.*
import android.database.Cursor
import android.net.Uri
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.database.LCDao
import com.absinthe.libchecker.database.LCDatabase
import timber.log.Timber
import java.util.concurrent.Callable

class CoreProvider : ContentProvider() {

    companion object {
        /** The authority of this content provider.  */
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.coreprovider"
        const val RULES_TABLE = "rules_table"

        /** The URI for the rules table.  */
        val URI_MENU = Uri.parse("content://$AUTHORITY/$RULES_TABLE")

        /**The match code for some items in the rules table.  */
        private const val CODE_RULES_DIR = 1

        /** The match code for an item in the rules table.  */
        private const val CODE_RULES_ITEM = 2

        /** The URI matcher.  */
        private val MATCHER = UriMatcher(UriMatcher.NO_MATCH)

        init {
            MATCHER.addURI(AUTHORITY, RULES_TABLE, CODE_RULES_DIR)
            MATCHER.addURI(AUTHORITY, "$RULES_TABLE/*", CODE_RULES_ITEM)
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        val code = MATCHER.match(uri)
        Timber.d("Query: code=$code")
        return if (code == CODE_RULES_DIR || code == CODE_RULES_ITEM) {
            val context = context ?: return null
            val lcDao: LCDao = LCDatabase.getDatabase(context).lcDao()
            val cursor: Cursor? = if (code == CODE_RULES_DIR) {
                lcDao.selectAllRules()
            } else {
//                lcDao.selectRuleByName(ContentUris.parseId(uri))
                null
            }
            cursor?.setNotificationUri(context.contentResolver, uri)
            cursor
        } else {
            throw java.lang.IllegalArgumentException("Unknown URI: $uri")
        }
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

    override fun getType(uri: Uri): String {
        return when (MATCHER.match(uri)) {
            CODE_RULES_DIR -> "vnd.android.cursor.dir/${AUTHORITY}.$RULES_TABLE"
            CODE_RULES_ITEM -> "vnd.android.cursor.item/${AUTHORITY}.$RULES_TABLE"
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