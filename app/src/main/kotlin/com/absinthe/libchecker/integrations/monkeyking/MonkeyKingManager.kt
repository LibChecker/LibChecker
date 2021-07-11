package com.absinthe.libchecker.integrations.monkeyking

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.showToast
import com.google.gson.Gson

const val TYPE_ACTIVITY = "activity"
const val TYPE_SERVICE = "service"
const val TYPE_RECEIVER = "receiver"
const val TYPE_PROVIDER = "provider"

private const val URI_AUTHORIZATION = "content://com.ext.star.wars.cfg.InnerProvider/"
private const val MONKEY_KING_APPLICATION_ID = "com.ext.star.wars"
private const val FIRST_SUPPORT_VERSION_CODE = 308047

class MonkeyKingManager {

    fun queryBlockedComponent(context: Context, packageName: String): List<ShareCmpInfo.Component> {
        val contentResolver = context.contentResolver
        val uri = Uri.parse(URI_AUTHORIZATION)
        return try {
            val bundle = contentResolver.call(uri, "cmps", packageName, null)
            val shareCmpInfoString = bundle?.getString("cmp_list")
            Gson().fromJson(shareCmpInfoString, ShareCmpInfo::class.java).components
        } catch (e: Throwable) {
            emptyList()
        }
    }

    fun addBlockedComponent(context: Context, packageName: String,
                            componentName: String, @LibType type: Int,
                            shouldBlock: Boolean) {
        val fullComponentName = if (componentName.startsWith(".")) { packageName + componentName } else { componentName }
        val shareCmpInfo = ShareCmpInfo(packageName, listOf( ShareCmpInfo.Component(
            type = getType(type),
            name = fullComponentName,
            block = shouldBlock
        ) ))
        val bundle = Bundle().apply {
            putString("cmp_list", Gson().toJson(shareCmpInfo))
        }
        val uri = Uri.parse(URI_AUTHORIZATION)
        try {
            context.contentResolver.call(uri, "blocks", packageName, bundle)
        } catch (e: Exception) {
            context.showToast(e.message.toString())
        }
    }

    private fun getType(@LibType type: Int): String = when(type) {
        ACTIVITY -> TYPE_ACTIVITY
        SERVICE -> TYPE_SERVICE
        RECEIVER -> TYPE_RECEIVER
        PROVIDER -> TYPE_PROVIDER
        else -> throw IllegalStateException("wrong type")
    }

    companion object {
        val isSupportInteraction =
            PackageUtils.isAppInstalled(MONKEY_KING_APPLICATION_ID)
                    && PackageUtils.getVersionCode(MONKEY_KING_APPLICATION_ID) >= FIRST_SUPPORT_VERSION_CODE
    }
}
