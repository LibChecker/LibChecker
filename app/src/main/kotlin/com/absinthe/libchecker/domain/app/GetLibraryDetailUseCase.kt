package com.absinthe.libchecker.domain.app

import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.api.bean.LibDetailBean
import com.absinthe.libchecker.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Locale
import timber.log.Timber

class GetLibraryDetailUseCase(
  private val libraryDetailRepository: LibraryDetailRepository
) {
  suspend operator fun invoke(
    libName: String,
    @LibType type: Int,
    isRegex: Boolean = false
  ): LibDetailBean? {
    val categoryDir = type.toCategoryDir(isRegex)
    val libPath = if (type in listOf(SERVICE, ACTIVITY, RECEIVER, PROVIDER, STATIC)) {
      libName.replace(".", "/")
    } else {
      libName
    }
    Timber.d("requestLibDetail: categoryDir = $categoryDir, libPath = $libPath")

    return runCatching {
      libraryDetailRepository.requestLibraryDetail(categoryDir, libPath)
    }.onFailure {
      Timber.w(it, "Failed to request lib detail: $categoryDir/$libPath")
    }.getOrNull()
  }

  suspend fun getRepoUpdatedTime(owner: String, repo: String): String? {
    val pushedAt = DateUtils.parseIso8601DateTime(
      libraryDetailRepository.getRepoPushedAt(owner, repo) ?: return null
    ) ?: return null
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(pushedAt)
  }

  private fun Int.toCategoryDir(isRegex: Boolean): String {
    var categoryDir = when (this) {
      NATIVE -> "native-libs"
      SERVICE -> "services-libs"
      ACTIVITY -> "activities-libs"
      RECEIVER -> "receivers-libs"
      PROVIDER -> "providers-libs"
      DEX -> "dex-libs"
      STATIC -> "static-libs"
      ACTION -> "actions-libs"
      else -> throw IllegalArgumentException("Illegal LibType: $this.")
    }
    if (isRegex) {
      categoryDir += "/regex"
    }
    return categoryDir
  }
}
