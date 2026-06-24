package com.absinthe.libchecker.domain.app

import androidx.annotation.DrawableRes
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.api.bean.LibDetailBean
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.database.RulesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetLibraryDetailDialogDataUseCase(
  private val getLibraryDetailUseCase: GetLibraryDetailUseCase
) {

  suspend fun getHeader(request: HeaderRequest): LibraryDetailDialogHeader =
    withContext(Dispatchers.IO) {
      val rule = if (request.isValidLib) {
        RulesRepository.getRule(request.libName, request.type, true)
      } else {
        null
      }
      LibraryDetailDialogHeader(
        iconRes = rule?.iconRes ?: com.absinthe.lc.rulesbundle.R.drawable.ic_sdk_placeholder,
        isSimpleColorIcon = rule?.isSimpleColorIcon == true
      )
    }

  suspend operator fun invoke(request: Request): Result =
    withContext(Dispatchers.IO) {
      if (!request.isValidLib) {
        return@withContext Result.NotFound
      }

      val regexName = request.regexName?.takeIf { it.isNotEmpty() }
      val detail = getLibraryDetailUseCase(
        libName = regexName ?: request.libName,
        type = request.type,
        isRegex = regexName != null
      ) ?: return@withContext Result.NotFound

      Result.Found(
        detail = detail,
        repoUpdatedTime = detail.getRepoUpdatedTime()
      )
    }

  private suspend fun LibDetailBean.getRepoUpdatedTime(): String? {
    if (!GlobalValues.isGitHubReachable) {
      return null
    }

    val sourceLink = data.firstOrNull()?.data?.source_link.orEmpty()
    if (!sourceLink.startsWith(URLManager.GITHUB_HOST)) {
      return null
    }

    val splits = sourceLink.removePrefix(URLManager.GITHUB_HOST).split("/")
    if (splits.size < 2) {
      return null
    }
    return getLibraryDetailUseCase.getRepoUpdatedTime(splits[0], splits[1])
  }

  data class HeaderRequest(
    val libName: String,
    @LibType val type: Int,
    val isValidLib: Boolean
  )

  data class Request(
    val libName: String,
    @LibType val type: Int,
    val regexName: String?,
    val isValidLib: Boolean
  )

  sealed interface Result {
    data object NotFound : Result

    data class Found(
      val detail: LibDetailBean,
      val repoUpdatedTime: String?
    ) : Result
  }
}

data class LibraryDetailDialogHeader(
  @DrawableRes val iconRes: Int,
  val isSimpleColorIcon: Boolean
)
