package com.absinthe.libchecker.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.annotation.IntRange
import androidx.collection.LruCache
import com.absinthe.libchecker.R
import kotlinx.coroutines.*
import me.zhanghai.android.appiconloader.AppIconLoader
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/**
 * Frome Shizuku
 */
object AppIconCache : CoroutineScope {

    private class AppIconLruCache(@IntRange(from = 1) maxSize: Int) :
        LruCache<Triple<String, Int, Int>, Bitmap>(maxSize) {

        override fun sizeOf(key: Triple<String, Int, Int>, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    override val coroutineContext: CoroutineContext get() = Dispatchers.Main

    private val lruCache: LruCache<Triple<String, Int, Int>, Bitmap>

    private val dispatcher: CoroutineDispatcher

    private val appIconLoaders = mutableMapOf<Int, AppIconLoader>()

    init {
        // Initialize app icon lru cache
        val maxMemory = Runtime.getRuntime().maxMemory() / 1024
        val availableCacheSize = (maxMemory / 4).toInt()
        lruCache = AppIconLruCache(availableCacheSize)

        // Initialize load icon scheduler
        val availableProcessorsCount = try {
            Runtime.getRuntime().availableProcessors()
        } catch (ignored: Exception) {
            1
        }
        val threadCount = 1.coerceAtLeast(availableProcessorsCount / 2)
        val loadIconExecutor: Executor = Executors.newFixedThreadPool(threadCount)
        dispatcher = loadIconExecutor.asCoroutineDispatcher()
    }

    fun dispatcher(): CoroutineDispatcher {
        return dispatcher
    }

    private fun get(packageName: String, userId: Int, size: Int): Bitmap? {
        return lruCache[Triple(packageName, userId, size)]
    }

    private fun put(packageName: String, userId: Int, size: Int, bitmap: Bitmap) {
        if (get(packageName, userId, size) == null) {
            lruCache.put(Triple(packageName, userId, size), bitmap)
        }
    }

    private fun remove(packageName: String, userId: Int, size: Int) {
        lruCache.remove(Triple(packageName, userId, size))
    }

    private fun getOrLoadBitmap(
        context: Context,
        info: ApplicationInfo,
        userId: Int,
        size: Int
    ): Bitmap {
        val cachedBitmap = get(info.packageName, userId, size)
        if (cachedBitmap != null) {
            return cachedBitmap
        }
        var loader = appIconLoaders[size]
        if (loader == null) {
            val shrinkNonAdaptiveIcons = false
            loader = AppIconLoader(size, shrinkNonAdaptiveIcons, context)
            appIconLoaders[size] = loader
        }
        val bitmap = loader.loadIcon(info, false)
        put(info.packageName, userId, size, bitmap)
        return bitmap
    }

    @JvmStatic
    fun loadIconBitmapAsync(
        context: Context,
        info: ApplicationInfo, userId: Int,
        view: ImageView
    ): Job {
        return launch {
            val size = view.measuredWidth.let {
                if (it > 0) it else context.resources.getDimensionPixelSize(R.dimen.app_icon_size)
            }
            val cachedBitmap = get(info.packageName, userId, size)
            if (cachedBitmap != null) {
                view.setImageBitmap(cachedBitmap)
                return@launch
            }

            val bitmap = try {
                withContext(dispatcher) {
                    getOrLoadBitmap(context, info, userId, size)
                }
            } catch (e: CancellationException) {
                // do nothing if canceled
                return@launch
            } catch (e: Throwable) {
                null
            }

            if (bitmap != null) {
                view.setImageBitmap(bitmap)
            } else {
                view.setImageDrawable(info.loadIcon(context.packageManager))
            }
        }
    }
}
