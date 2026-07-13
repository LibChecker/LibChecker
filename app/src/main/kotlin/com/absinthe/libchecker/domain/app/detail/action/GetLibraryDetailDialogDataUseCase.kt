package com.absinthe.libchecker.domain.app.detail.action

import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.LibDetailBean
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.app.detail.model.DetailInfoItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.DetailInfoTextStyle
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailContentDisplay
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailHeaderDisplay
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailLocaleDisplay
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetLibraryDetailDialogDataUseCase(
  private val getLibraryDetailUseCase: GetLibraryDetailUseCase
) {

  suspend fun getHeader(request: HeaderRequest): LibraryDetailHeaderDisplay = withContext(Dispatchers.IO) {
    val rule = if (request.isValidLib) {
      RulesRepository.getRule(request.libName, request.type, true)
    } else {
      null
    }
    LibraryDetailHeaderDisplay(
      iconRes = rule?.iconRes ?: com.absinthe.lc.rulesbundle.R.drawable.ic_sdk_placeholder,
      isSimpleColorIcon = rule?.isSimpleColorIcon == true
    )
  }

  suspend operator fun invoke(request: Request): Result = withContext(Dispatchers.IO) {
    if (!request.isValidLib) {
      return@withContext Result.NotFound
    }

    val regexName = request.regexName?.takeIf { it.isNotEmpty() }
    val detail = getLibraryDetailUseCase(
      libName = regexName ?: request.libName,
      type = request.type,
      isRegex = regexName != null
    ) ?: return@withContext Result.NotFound

    val content = buildLibraryDetailContentDisplay(
      detail = detail,
      repoUpdatedTime = detail.getRepoUpdatedTime(),
      preferredLocale = request.preferredLocale
    ) ?: return@withContext Result.NotFound

    Result.Found(
      content = content,
      libraryUuid = detail.uuid
    )
  }

  private suspend fun LibDetailBean.getRepoUpdatedTime(): String? {
    if (!GlobalValues.isGitHubReachable &&
      GlobalValues.githubApiAuthorizationHeaderFor(ApiManager.GITHUB_API_REPO_INFO) == null
    ) {
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
    val isValidLib: Boolean,
    val preferredLocale: String
  )

  sealed interface Result {
    data object NotFound : Result

    data class Found(
      val content: LibraryDetailContentDisplay,
      val libraryUuid: String
    ) : Result
  }
}

internal fun buildLibraryDetailContentDisplay(
  detail: LibDetailBean,
  repoUpdatedTime: String?,
  preferredLocale: String,
  localeNameResolver: (String) -> String = { Locale.forLanguageTag(it).displayName }
): LibraryDetailContentDisplay? {
  val locales = detail.data.map { localizedDetail ->
    val data = localizedDetail.data
    LibraryDetailLocaleDisplay(
      localeTag = localizedDetail.locale,
      localeName = localeNameResolver(localizedDetail.locale),
      items = buildList {
        add(
          DetailInfoItemDisplay(
            iconRes = R.drawable.ic_label,
            tipRes = R.string.lib_detail_label_tip,
            textStyle = DetailInfoTextStyle.TITLE,
            text = data.label
          )
        )
        add(
          DetailInfoItemDisplay(
            iconRes = R.drawable.ic_team,
            tipRes = R.string.lib_detail_develop_team_tip,
            textStyle = DetailInfoTextStyle.TITLE,
            text = data.dev_team
          )
        )
        add(
          DetailInfoItemDisplay(
            iconRes = R.drawable.ic_github,
            tipRes = R.string.lib_detail_rule_contributors_tip,
            textStyle = DetailInfoTextStyle.TITLE,
            text = data.rule_contributors.joinToString(separator = ", ")
          )
        )
        add(
          DetailInfoItemDisplay(
            iconRes = R.drawable.ic_content,
            tipRes = R.string.lib_detail_description_tip,
            textStyle = DetailInfoTextStyle.BODY,
            text = data.description
          )
        )
        add(
          DetailInfoItemDisplay(
            iconRes = R.drawable.ic_url,
            tipRes = R.string.lib_detail_relative_link_tip,
            textStyle = DetailInfoTextStyle.BODY,
            text = data.source_link,
            linkUrl = data.source_link
          )
        )
        repoUpdatedTime?.let { updatedAt ->
          add(
            DetailInfoItemDisplay(
              iconRes = R.drawable.ic_time,
              tipRes = R.string.lib_detail_last_update_tip,
              textStyle = DetailInfoTextStyle.BODY,
              text = updatedAt
            )
          )
        }
      }
    )
  }
  if (locales.isEmpty()) {
    return null
  }

  val selectedLocaleTag = locales
    .firstOrNull { it.localeTag == preferredLocale }
    ?.localeTag
    ?: locales.first().localeTag
  return LibraryDetailContentDisplay(
    locales = locales,
    selectedLocaleTag = selectedLocaleTag
  )
}
