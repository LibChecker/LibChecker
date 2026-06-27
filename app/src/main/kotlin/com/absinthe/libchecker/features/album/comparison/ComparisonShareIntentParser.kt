package com.absinthe.libchecker.features.album.comparison

import android.content.Intent
import android.net.Uri
import androidx.core.os.BundleCompat

internal object ComparisonShareIntentParser {

  fun parse(intent: Intent): Result {
    if (intent.action != Intent.ACTION_SEND_MULTIPLE) {
      return Result.None
    }

    val uriList = intent.extras?.let {
      BundleCompat.getParcelableArrayList(it, Intent.EXTRA_STREAM, Uri::class.java)
    }

    if (uriList?.size != 2) {
      return Result.InvalidSharedItems
    }

    val leftUri = uriList[0].takeIf { it.isPackageArchiveUri() }
    val rightUri = uriList[1].takeIf { it.isPackageArchiveUri() }
    return Result.PackagePair(
      leftUri = leftUri,
      rightUri = rightUri,
      invalidItemCount = leftUri.invalidCount() + rightUri.invalidCount()
    )
  }

  private fun Uri.isPackageArchiveUri(): Boolean {
    return encodedPath?.endsWith(".apk") == true || encodedPath?.endsWith(".apks") == true
  }

  private fun Uri?.invalidCount(): Int = if (this == null) 1 else 0

  sealed interface Result {
    data object None : Result
    data object InvalidSharedItems : Result

    data class PackagePair(
      val leftUri: Uri?,
      val rightUri: Uri?,
      val invalidItemCount: Int
    ) : Result
  }
}
