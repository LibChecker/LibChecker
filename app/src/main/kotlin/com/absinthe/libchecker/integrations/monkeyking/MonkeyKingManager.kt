package com.absinthe.libchecker.integrations.monkeyking

import android.content.Context
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libchecker.utils.toJson

const val TYPE_ACTIVITY = "activity"
const val TYPE_SERVICE = "service"
const val TYPE_RECEIVER = "receiver"
const val TYPE_PROVIDER = "provider"

private const val URI_AUTHORIZATION = "content://com.ext.star.wars.cfg.InnerProvider"
private const val MONKEY_KING_APPLICATION_ID = "com.ext.star.wars"
private const val FIRST_SUPPORT_VERSION_CODE = 308047

class MonkeyKingManager {

  fun queryBlockedComponent(context: Context, packageName: String): List<ShareCmpInfo.Component> {
    val contentResolver = context.contentResolver
    val uri = URI_AUTHORIZATION.toUri()
    return try {
      val bundle = contentResolver.call(uri, "cmps", packageName, null)
      val shareCmpInfoString = bundle?.getString("cmp_list") ?: return emptyList()
      shareCmpInfoString.fromJson<ShareCmpInfo>()?.components ?: emptyList()
    } catch (e: Throwable) {
      emptyList()
    }
  }

  fun addBlockedComponent(
    context: Context,
    packageName: String,
    componentName: String,
    @LibType type: Int,
    shouldBlock: Boolean
  ) {
    val fullComponentName = if (componentName.startsWith(".")) {
      packageName + componentName
    } else {
      componentName
    }
    val shareCmpInfo = ShareCmpInfo(
      packageName,
      listOf(
        ShareCmpInfo.Component(
          type = getType(type),
          name = fullComponentName,
          block = shouldBlock
        )
      )
    )
    val bundle = bundleOf(
      "cmp_list" to shareCmpInfo.toJson()
    )
    try {
      context.contentResolver.call(URI_AUTHORIZATION.toUri(), "blocks", packageName, bundle)
    } catch (e: Exception) {
      context.showToast(e.message.toString())
    }
  }

  private fun getType(@LibType type: Int): String = when (type) {
    ACTIVITY -> TYPE_ACTIVITY
    SERVICE -> TYPE_SERVICE
    RECEIVER -> TYPE_RECEIVER
    PROVIDER -> TYPE_PROVIDER
    else -> throw IllegalStateException("wrong type")
  }

  companion object {
    val isSupportInteraction =
      PackageUtils.isAppInstalled(MONKEY_KING_APPLICATION_ID) &&
        PackageUtils.getVersionCode(MONKEY_KING_APPLICATION_ID) >= FIRST_SUPPORT_VERSION_CODE
  }
}
