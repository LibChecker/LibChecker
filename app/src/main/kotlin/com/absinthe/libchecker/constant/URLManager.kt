package com.absinthe.libchecker.constant

import com.absinthe.libchecker.BuildConfig

object URLManager {
    const val MARKET_PAGE = "market://details?id=${BuildConfig.APPLICATION_ID}"
    const val COOLAPK_APP_PAGE = "coolmarket://apk/${BuildConfig.APPLICATION_ID}"

    const val COOLAPK_HOME_PAGE = "coolmarket://u/482045"
    const val GITHUB_PAGE = "https://github.com/zhaobozhen"

    const val GITHUB_REPO_PAGE = "https://github.com/zhaobozhen/LibChecker"
}