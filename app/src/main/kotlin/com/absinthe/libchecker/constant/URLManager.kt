package com.absinthe.libchecker.constant

import com.absinthe.libchecker.BuildConfig
import com.absinthe.libraries.me.Absinthe

object URLManager {
  const val MARKET_PAGE = "${Absinthe.MARKET_DETAIL_SCHEME}${BuildConfig.APPLICATION_ID}"
  const val COOLAPK_APP_PAGE = "coolmarket://apk/${BuildConfig.APPLICATION_ID}"

  const val MARKET_SCHEME = Absinthe.MARKET_DETAIL_SCHEME
  const val COOLAPK_SCHEME = "coolmarket://apk/"

  const val COOLAPK_HOME_PAGE = Absinthe.COOLAPK_HOME_PAGE
  const val GITHUB_PAGE = Absinthe.GITHUB_HOME_PAGE

  const val GITHUB = "https://github.com/"
  const val GITHUB_REPO_PAGE = "https://github.com/LibChecker/LibChecker"

  const val DOCS_PAGE = "https://absinthe.life/LibChecker-Docs"
  const val CROWDIN_PAGE = "https://crowdin.com/project/libchecker"

  const val TELEGRAM_GROUP = "https://t.me/libcheckerr"

  const val ANDROID_DEV_MANIFEST_APPLICATION = "https://developer.android.com/guide/topics/manifest/application-element"
}
