package com.absinthe.libchecker.constant

import com.absinthe.libchecker.BuildConfig

object URLManager {
  const val MARKET_PAGE = "market://details?id=${BuildConfig.APPLICATION_ID}"

  const val SHIZUKU_APP_GITHUB_RELEASE_PAGE = "https://github.com/RikkaApps/Shizuku/releases"
  const val PLAY_STORE_DETAIL_PAGE = "https://play.google.com/store/apps/details?id=com.absinthe.libchecker"

  const val GITHUB_HOST = "https://github.com/"
  const val GITHUB_REPO_PAGE = "https://github.com/LibChecker/LibChecker"
  const val FDROID_PAGE = "https://f-droid.org/packages/com.absinthe.libchecker/"

  const val DOCS_PAGE = "https://absinthe.life/LibChecker-Docs"
  const val CROWDIN_PAGE = "https://crowdin.com/project/libchecker"

  const val TELEGRAM_GROUP = "https://t.me/libcheckerr"
  const val TELEGRAM_RELEASES = "https://t.me/libchecker_releases"

  const val ANDROID_DEV_MANIFEST_APPLICATION = "https://developer.android.com/guide/topics/manifest/application-element"
}
