package com.absinthe.libchecker.integrations.monkeyking

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.extensions.loge
import com.blankj.utilcode.util.AppUtils
import com.google.gson.Gson
import java.lang.IllegalStateException

const val TYPE_ACTIVITY = "activity"
const val TYPE_SERVICE = "service"
const val TYPE_RECEIVER = "receiver"
const val TYPE_PROVIDER = "provider"

private const val URI_AUTHORIZATION = "content://com.ext.star.wars.cfg.InnerProvider/"
private const val MONKEY_KING_APPLICATION_ID = "com.ext.star.wars"

class MonkeyKingManager {

    fun queryBlockedComponent(context: Context, packageName: String): List<ShareCmpInfo.Component> {
        val contentResolver = context.contentResolver
        val uri = Uri.parse(URI_AUTHORIZATION)
        val bundle = contentResolver.call(uri, "cmps", packageName, null)
        val shareCmpInfoString = bundle?.getString("cmp_list")
        return Gson().fromJson(shareCmpInfoString, ShareCmpInfo::class.java).components
    }

    fun addBlockedComponent(context: Context, packageName: String,
                            componentName: String, @LibType type: Int,
                            shouldBlock: Boolean) {
        val shareCmpInfo = ShareCmpInfo(packageName, listOf( ShareCmpInfo.Component(
            type = getType(type),
            name = componentName,
            block = shouldBlock
        ) ))
        val bundle = Bundle().apply {
            putString("cmp_list", Gson().toJson(shareCmpInfo))
        }
        val uri = Uri.parse(URI_AUTHORIZATION)
        try {
            context.contentResolver.call(uri, "blocks", packageName, bundle)
        } catch (e: Throwable) {
            loge(e.toString())
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
            AppUtils.isAppInstalled(MONKEY_KING_APPLICATION_ID)
                    && AppUtils.getAppVersionCode(MONKEY_KING_APPLICATION_ID) >= 308047
    }
}