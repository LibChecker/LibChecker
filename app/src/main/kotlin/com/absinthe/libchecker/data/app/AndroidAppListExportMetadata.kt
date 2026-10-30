package com.absinthe.libchecker.data.app

import android.content.Context
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.domain.app.list.export.AppListExportMetadata
import com.absinthe.libchecker.domain.app.model.PackageInfoFormatter

class AndroidAppListExportMetadata(context: Context) : AppListExportMetadata {

  private val appContext = context.applicationContext

  override val versionName: String
    get() = BuildConfig.VERSION_NAME

  override val versionCode: Long
    get() = BuildConfig.VERSION_CODE.toLong()

  override fun formatAbi(abi: Short): String {
    return PackageInfoFormatter.getAbiString(appContext, abi.toInt(), false)
  }
}
