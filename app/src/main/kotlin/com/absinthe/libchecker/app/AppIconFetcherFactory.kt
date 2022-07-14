package com.absinthe.libchecker.app

import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.absinthe.libchecker.utils.extensions.dp
import me.zhanghai.android.appiconloader.AppIconLoader

class AppIconFetcherFactory(private val context: Context) : Fetcher.Factory<PackageInfo> {

  private val loader = AppIconLoader(40.dp, false, context)

  override fun create(data: PackageInfo, options: Options, imageLoader: ImageLoader): Fetcher {
    return AppIconFetcher(context, data, loader)
  }

  class AppIconFetcher(
    private val context: Context,
    private val info: PackageInfo,
    private val loader: AppIconLoader
  ) : Fetcher {
    override suspend fun fetch(): FetchResult {
      val icon = loader.loadIcon(info.applicationInfo)
      return DrawableResult(BitmapDrawable(context.resources, icon), true, DataSource.DISK)
    }
  }
}
