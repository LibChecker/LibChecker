package com.absinthe.libchecker.constant

object Constants {

    const val APP_CENTER_SECRET = "5f11b856-0a27-4438-a038-9e18e4797133"

    const val ERROR = -1
    const val ARMV8 = 0
    const val ARMV7 = 1
    const val ARMV5 = 2
    const val NO_LIBS = 3
    const val X86 = 4
    const val X86_64 = 5
    const val MULTI_ARCH = 10
    const val OVERLAY = 100

    const val ARMV8_STRING = "arm64-v8a"
    const val ARMV7_STRING = "armeabi-v7a"
    const val ARMV5_STRING = "armeabi"
    const val X86_STRING = "x86"
    const val X86_64_STRING = "x86_64"

    const val PREF_SHOW_SYSTEM_APPS = "showSystemApps"
    const val PREF_APK_ANALYTICS = "apkAnalytics"
    const val PREF_RULES_REPO = "rulesRepository"
    const val PREF_COLORFUL_ICON = "colorfulIcon"
    const val PREF_LIB_REF_THRESHOLD = "libRefThreshold"
    const val PREF_RELOAD_APPS = "reloadApps"
    const val PREF_ABOUT = "about"
    const val PREF_HELP = "help"
    const val PREF_RATE = "rate"
    const val PREF_TELEGRAM = "tg"
    const val PREF_ANONYMOUS_ANALYTICS = "analytics"
    const val PREF_CLOUD_RULES = "cloudRules"

    const val PREF_APP_SORT_MODE = "appSortMode"
    const val PREF_LIB_SORT_MODE = "libSortMode"
    const val PREF_SNAPSHOT_TIMESTAMP = "snapshotTimestamp"

    const val PREF_LOCAL_BACKUP = "localBackup"
    const val PREF_LOCAL_RESTORE = "localRestore"

    const val PREF_LOCAL_RULES_VERSION = "localRulesVersion"
    const val PREF_LOCALE = "locale"

    const val SORT_MODE_UPDATE_TIME_DESC = 0
    const val SORT_MODE_TARGET_API_DESC = 1
    const val SORT_MODE_DEFAULT = 2

    const val CURRENT_LIB_REF_TYPE = "currentLibRefType"

    const val REPO_GITHUB = "github"
    const val REPO_GITEE = "gitee"

    const val ACTION_APP_LIST = "com.absinthe.libchecker.intent.action.START_APP_LIST"
    const val ACTION_STATISTICS = "com.absinthe.libchecker.intent.action.START_STATISTICS"
    const val ACTION_SNAPSHOT = "com.absinthe.libchecker.intent.action.START_SNAPSHOT"

    const val PACKAGE_NAME_COOLAPK = "com.coolapk.market"

    const val COMMAND_DEBUG_MODE = "/debugMode"

    const val RULES_DB_FILE_NAME = "rules.db"
    const val RULES_DATABASE_NAME = "rule_database"

    object Event {
        const val LAUNCH_ACTION = "Launch Action"
        const val SNAPSHOT_CLICK = "Snapshot Click"
        const val SETTINGS = "Settings"
        const val LIB_REFERENCE_FILTER_TYPE = "Lib Reference Filter Type"
        const val EASTER_EGG = "Easter Egg"
        const val SNAPSHOT_DETAIL_COMPONENT_COUNT = "Snapshot Detail Component Count"
    }
}