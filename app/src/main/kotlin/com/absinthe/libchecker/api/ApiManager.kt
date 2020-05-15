package com.absinthe.libchecker.api

import com.absinthe.libchecker.api.bean.CONFIGURATION_VERSION

object ApiManager {

    const val ROOT_URL = "https://raw.githubusercontent.com/zhaobozhen/LibChecker-Rules/master/"
    const val GITEE_ROOT_URL = "https://gitee.com/zhaobozhen/LibChecker-Rules/raw/master/"
    const val CONFIGURATION_URL = "configuration/configuration_v$CONFIGURATION_VERSION.json"
}