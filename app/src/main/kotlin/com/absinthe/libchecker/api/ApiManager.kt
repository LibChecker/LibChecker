package com.absinthe.libchecker.api

import com.absinthe.libchecker.api.bean.CONFIGURATION_VERSION
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues

object ApiManager {

    private const val GITHUB_ROOT_URL =
        "https://raw.githubusercontent.com/zhaobozhen/LibChecker-Rules/master/"
    private const val GITEE_ROOT_URL =
        "https://gitee.com/zhaobozhen/LibChecker-Rules/raw/master/"

    const val CONFIGURATION_URL = "configuration/configuration_v$CONFIGURATION_VERSION.json"
    const val GITHUB_NEW_ISSUE_URL = "https://github.com/zhaobozhen/LibChecker-Rules/issues/new?labels=&template=library-name.md&title=%5BNew+Rule%5D"

    var root = GITHUB_ROOT_URL
        get() = when (GlobalValues.repo) {
            Constants.REPO_GITHUB -> GITHUB_ROOT_URL
            Constants.REPO_GITEE -> GITEE_ROOT_URL
            else -> GITHUB_ROOT_URL
        }
        private set
}