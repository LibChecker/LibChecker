package com.absinthe.libchecker.domain.about.ui

import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.GitHubContributorResp
import com.absinthe.libchecker.api.request.CloudRuleBundleRequest
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.domain.about.model.CachedDeveloperInfo
import com.absinthe.libchecker.domain.about.model.DeveloperInfo
import com.absinthe.libchecker.domain.about.ui.view.DevelopersDialogView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.JsonUtil
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.squareup.moshi.Types
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber

class DevelopersDialogFragment : BaseBottomSheetViewDialogFragment<DevelopersDialogView>() {

  override fun initRootView(): DevelopersDialogView = DevelopersDialogView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.8f
    root.addPaddingTop(16.dp)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val authorization = GlobalValues.githubApiAuthorizationHeader
    val tokenCacheKey = GlobalValues.githubApiToken.toTokenCacheKey()
    val cached = cachedItems
    if (cached != null) {
      root.setItems(cached.items)
      root.setLoading(false)
      if (!cached.shouldRefresh(tokenCacheKey)) {
        return
      }
    }

    val diskCache = if (cached == null) {
      readCachedItems()?.also {
        cachedItems = it
        root.setItems(it.items)
        root.setLoading(false)
      }
    } else {
      null
    }
    if (diskCache != null && !diskCache.shouldRefresh(tokenCacheKey)) {
      return
    }

    viewLifecycleOwner.lifecycleScope.launch {
      val hasVisibleItems = cached != null || diskCache != null
      if (!hasVisibleItems) {
        root.setLoading(true)
      }
      val result = runCatching {
        withContext(Dispatchers.IO) {
          loadGitHubContributors(authorization)
        }
      }
      result.onSuccess {
        if (it.isNotEmpty()) {
          cachedItems = DeveloperInfoCache(
            items = it,
            cachedAt = System.currentTimeMillis(),
            tokenCacheKey = tokenCacheKey
          )
          cacheItems(it, tokenCacheKey)
          root.setItems(it)
        } else if (!hasVisibleItems) {
          root.setItems(fallbackItems)
        }
      }.onFailure {
        it.logGitHubRequestFailure()
        if (!hasVisibleItems) {
          root.setItems(fallbackItems)
        }
      }
      if (!hasVisibleItems) {
        root.setLoading(false)
      }
    }
  }

  private suspend fun loadGitHubContributors(authorization: String?): List<DeveloperInfo> {
    val request = ApiManager.create<CloudRuleBundleRequest>()
    return runCatching {
      request.loadContributorDeveloperInfo(authorization)
    }.recoverCatching {
      if (authorization != null && it.shouldRetryWithoutAuthorization()) {
        Timber.w(it, "Authenticated GitHub contributors request failed, retrying without token.")
        request.loadContributorDeveloperInfo(null)
      } else {
        throw it
      }
    }.getOrThrow()
  }

  private suspend fun CloudRuleBundleRequest.loadContributorDeveloperInfo(
    authorization: String?
  ): List<DeveloperInfo> {
    return getAllContributors(authorization)
      .filterNot { it.isBot() }
      .mergeContributorAliases()
      .sortedByDescending(GitHubContributorResp::contributions)
      .mapNotNull { it.toDeveloperInfo() }
  }

  private suspend fun CloudRuleBundleRequest.getAllContributors(
    authorization: String?
  ): List<GitHubContributorResp> {
    val contributors = mutableListOf<GitHubContributorResp>()
    var page = 1
    do {
      val pageItems = requestContributors(
        owner = GITHUB_OWNER,
        repo = GITHUB_REPO,
        authorization = authorization,
        page = page
      )
      contributors += pageItems
      page++
    } while (pageItems.size == GITHUB_PAGE_SIZE)
    return contributors
  }

  private fun readCachedItems(): DeveloperInfoCache? {
    val cacheVersion = SPUtils.getValue(PREF_DEVELOPERS_CACHE_VERSION, 0)
    if (cacheVersion != DEVELOPERS_CACHE_VERSION) {
      return null
    }
    val json = SPUtils.getValue(PREF_DEVELOPERS_CACHE, "")
    if (json.isEmpty()) {
      return null
    }
    val items = runCatching { developerCacheAdapter.fromJson(json) }
      .getOrNull()
      ?.map { it.toDeveloperInfo() }
      ?.takeIf { it.isNotEmpty() }
      ?: return null
    val cachedAt = SPUtils.getValue(PREF_DEVELOPERS_CACHE_TIMESTAMP, 0L)
    val tokenCacheKey = SPUtils.getValue(PREF_DEVELOPERS_CACHE_TOKEN_KEY, "")
    return DeveloperInfoCache(items, cachedAt, tokenCacheKey)
  }

  private fun cacheItems(items: List<DeveloperInfo>, tokenCacheKey: String) {
    val json = developerCacheAdapter.toJson(items.map { it.toCachedDeveloperInfo() })
    SPUtils.putValue(PREF_DEVELOPERS_CACHE, json)
    SPUtils.putValue(PREF_DEVELOPERS_CACHE_TIMESTAMP, System.currentTimeMillis())
    SPUtils.putValue(PREF_DEVELOPERS_CACHE_TOKEN_KEY, tokenCacheKey)
    SPUtils.putValue(PREF_DEVELOPERS_CACHE_VERSION, DEVELOPERS_CACHE_VERSION)
  }

  private fun Throwable.shouldRetryWithoutAuthorization(): Boolean {
    return this is HttpException && (code() == 401 || code() == 403)
  }

  private fun Throwable.logGitHubRequestFailure() {
    if (this is HttpException) {
      val headers = response()?.headers()
      Timber.e(
        this,
        "GitHub contributors request failed: code=%d, rateLimit=%s, remaining=%s, reset=%s",
        code(),
        headers?.get("X-RateLimit-Limit"),
        headers?.get("X-RateLimit-Remaining"),
        headers?.get("X-RateLimit-Reset")
      )
    } else {
      Timber.e(this)
    }
  }

  private fun String.toTokenCacheKey(): String {
    return trim()
      .takeIf { it.isNotEmpty() }
      ?.hashCode()
      ?.toString()
      .orEmpty()
  }

  private fun GitHubContributorResp.isBot(): Boolean {
    val fields = listOfNotNull(login, name, email)
      .map { it.lowercase(Locale.US) }
    return type.equals("Bot", ignoreCase = true) ||
      fields.any {
        it.endsWith("[bot]") ||
          it.endsWith("-bot") ||
          it.endsWith("bot")
      }
  }

  private fun List<GitHubContributorResp>.mergeContributorAliases(): List<GitHubContributorResp> {
    val merged = linkedMapOf<String, GitHubContributorResp>()
    forEach { contributor ->
      val key = contributor.identityKey() ?: return@forEach
      val existing = merged[key]
      merged[key] = existing?.mergeWith(contributor) ?: contributor
    }
    return merged.values.toList()
  }

  private fun GitHubContributorResp.identityKey(): String? {
    return (
      login?.takeIf { it.isNotBlank() }
        ?: name?.takeIf { it.isNotBlank() }
      )
      ?.lowercase(Locale.US)
  }

  private fun GitHubContributorResp.mergeWith(other: GitHubContributorResp): GitHubContributorResp {
    val preferredProfile = if (other.profileScore() > profileScore()) other else this
    val fallback = if (preferredProfile === this) other else this
    return preferredProfile.copy(
      name = preferredProfile.name ?: fallback.name,
      email = preferredProfile.email ?: fallback.email,
      contributions = contributions + other.contributions
    )
  }

  private fun GitHubContributorResp.profileScore(): Int {
    var score = 0
    if (!login.isNullOrBlank() && !htmlUrl.isNullOrBlank()) {
      score += 2
    }
    if (!avatarUrl.isNullOrBlank()) {
      score += 1
    }
    return score
  }

  private fun Int.toCommitText(): String {
    return if (this == 1) "1 commit" else "$this commits"
  }

  private fun GitHubContributorResp.toDescription(): String {
    return if (isMaintainer()) {
      "Maintainer, ${contributions.toCommitText()}"
    } else {
      "Contributor, ${contributions.toCommitText()}"
    }
  }

  private fun GitHubContributorResp.toDeveloperInfo(): DeveloperInfo? {
    val displayName = login?.takeIf { it.isNotBlank() }
      ?: name?.takeIf { it.isNotBlank() }
      ?: return null
    val githubUrl = htmlUrl?.takeIf { it.isNotBlank() }
      ?: login
        ?.takeIf { it.isNotBlank() }
        ?.let { "https://github.com/$it" }
      ?: URLManager.GITHUB_REPO_PAGE
    val avatar = avatarUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_AVATAR_URL
    return DeveloperInfo(
      name = displayName,
      desc = toDescription(),
      github = githubUrl,
      avatarUrl = avatar.toUri()
    )
  }

  private fun GitHubContributorResp.isMaintainer(): Boolean {
    return login.equals(MAINTAINER_LOGIN, ignoreCase = true) ||
      htmlUrl?.substringAfterLast("/")?.equals(MAINTAINER_LOGIN, ignoreCase = true) == true
  }

  private fun DeveloperInfo.toCachedDeveloperInfo(): CachedDeveloperInfo {
    return CachedDeveloperInfo(
      name = name,
      desc = desc,
      github = github,
      avatarUrl = avatarUrl.toString()
    )
  }

  private fun CachedDeveloperInfo.toDeveloperInfo(): DeveloperInfo {
    return DeveloperInfo(
      name = name,
      desc = desc,
      github = github,
      avatarUrl = avatarUrl.toUri()
    )
  }

  private data class DeveloperInfoCache(
    val items: List<DeveloperInfo>,
    val cachedAt: Long,
    val tokenCacheKey: String
  ) {
    fun shouldRefresh(currentTokenCacheKey: String): Boolean {
      return cachedAt <= 0 ||
        System.currentTimeMillis() - cachedAt > DEVELOPERS_CACHE_VALID_DURATION ||
        tokenCacheKey != currentTokenCacheKey
    }
  }

  companion object {
    private const val GITHUB_OWNER = "LibChecker"
    private const val GITHUB_REPO = "LibChecker"
    private const val GITHUB_PAGE_SIZE = 100
    private const val MAINTAINER_LOGIN = "zhaobozhen"
    private const val DEFAULT_AVATAR_URL = "https://avatars.githubusercontent.com/u/116417672"
    private const val PREF_DEVELOPERS_CACHE = "developers_cache"
    private const val PREF_DEVELOPERS_CACHE_TIMESTAMP = "developers_cache_timestamp"
    private const val PREF_DEVELOPERS_CACHE_TOKEN_KEY = "developers_cache_token_key"
    private const val PREF_DEVELOPERS_CACHE_VERSION = "developers_cache_version"
    private const val DEVELOPERS_CACHE_VERSION = 2
    private const val DEVELOPERS_CACHE_VALID_DURATION = 24 * 60 * 60 * 1000L

    private var cachedItems: DeveloperInfoCache? = null
    private val developerCacheAdapter by lazy {
      JsonUtil.moshi.adapter<List<CachedDeveloperInfo>>(
        Types.newParameterizedType(List::class.java, CachedDeveloperInfo::class.java)
      )
    }

    private val fallbackItems = listOf(
      DeveloperInfo(
        "Absinthe",
        "Maintainer",
        "https://github.com/zhaobozhen",
        "https://avatars.githubusercontent.com/u/25247117".toUri()
      ),
      DeveloperInfo(
        "Goooler",
        "Code Tidy & Optimize",
        "https://github.com/Goooler",
        "https://avatars.githubusercontent.com/u/10363352".toUri()
      ),
      DeveloperInfo(
        "qhy040404",
        "Developer",
        "https://github.com/qhy040404",
        "https://avatars.githubusercontent.com/u/45379733".toUri()
      ),
      DeveloperInfo(
        "Source Code",
        URLManager.GITHUB_REPO_PAGE,
        URLManager.GITHUB_REPO_PAGE,
        "https://avatars.githubusercontent.com/u/116417672".toUri()
      )
    )
  }
}
