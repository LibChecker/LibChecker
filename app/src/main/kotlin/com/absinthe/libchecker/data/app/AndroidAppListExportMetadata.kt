package com.absinthe.libchecker.data.app

import android.content.Context
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.domain.app.AppListExportMetadata
import com.absinthe.libchecker.utils.PackageUtils

class AndroidAppListExportMetadata(context: Context) : AppListExportMetadata {

  private val appContext = context.applicationContext

  override val versionName: String
    get() = BuildConfig.VERSION_NAME

  override val versionCode: Long
    get() = BuildConfig.VERSION_CODE.toLong()

  override fun formatAbi(abi: Short): String {
    return PackageUtils.getAbiString(appContext, abi.toInt(), false)
  }
}
