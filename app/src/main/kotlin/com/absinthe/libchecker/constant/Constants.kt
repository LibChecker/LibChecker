package com.absinthe.libchecker.constant

object Constants {

  const val ERROR = -1
  const val ARMV8 = 0
  const val ARMV7 = 1
  const val ARMV5 = 2
  const val NO_LIBS = 3
  const val X86 = 4
  const val X86_64 = 5
  const val MIPS = 6
  const val MIPS64 = 7
  const val RISCV32 = 8
  const val RISCV64 = 9
  const val MULTI_ARCH = 10
  const val OVERLAY = 100

  const val VARIANT_APK: Short = 0
  const val VARIANT_HAP: Short = 1

  const val ARMV8_STRING = "arm64-v8a"
  const val ARMV8_STRING_UNDERLINE = "arm64_v8a"
  const val ARMV7_STRING = "armeabi-v7a"
  const val ARMV7_STRING_UNDERLINE = "armeabi_v7a"
  const val ARMV5_STRING = "armeabi"
  const val X86_STRING = "x86"
  const val X86_64_STRING = "x86_64"
  const val MIPS_STRING = "mips"
  const val MIPS64_STRING = "mips64"
  const val RISCV_STRING = "riscv32"
  const val RISCV64_STRING = "riscv64"
  const val OVERLAY_STRING = "Overlay"

  const val TEMP_PACKAGE = "lc_temp_package.apk"
  const val TEMP_PACKAGE_2 = "lc_temp_package_2.apk"
  const val TEMP_ICON = "lc_temp_icon.png"
  const val EXAMPLE_PACKAGE = "this.is.an.example"

  const val EXAMPLE_EXPORTED = "this.is.exported"
  const val EXAMPLE_NORMAL = "this.is.normal"
  const val EXAMPLE_DISABLED = "this.is.disabled"
  const val EXAMPLE_RULE = "Example SDK"

  const val PREF_APK_ANALYTICS = "apkAnalytics"
  const val PREF_RULES_REPO = "rulesRepository"
  const val PREF_COLORFUL_ICON = "colorfulIcon"
  const val PREF_LIB_REF_THRESHOLD = "libRefThreshold"
  const val PREF_RELOAD_APPS = "reloadApps"
  const val PREF_ABOUT = "about"
  const val PREF_GET_UPDATES = "getUpdates"
  const val PREF_TRANSLATION = "translation"
  const val PREF_HELP = "help"
  const val PREF_RATE = "rate"
  const val PREF_TELEGRAM = "tg"
  const val PREF_ANONYMOUS_ANALYTICS = "analytics"
  const val PREF_CLOUD_RULES = "cloudRules"
  const val PREF_SNAPSHOT_KEEP = "snapshotKeep"

  const val PREF_LIB_SORT_MODE = "libSortMode"
  const val PREF_PROCESS_MODE = "processMode"
  const val PREF_SNAPSHOT_TIMESTAMP = "snapshotTimestamp"
  const val PREF_DISTRIBUTION_UPDATE_TIMESTAMP = "distributionUpdateTimestamp"

  const val PREF_LOCAL_BACKUP = "localBackup"
  const val PREF_LOCAL_RESTORE = "localRestore"

  const val PREF_LOCALE = "locale"

  const val PREF_DEBUG_MODE = "debugMode"

  const val PREF_DARK_MODE = "darkMode"
  const val DARK_MODE_OFF = "off"
  const val DARK_MODE_ON = "on"
  const val DARK_MODE_FOLLOW_SYSTEM = "system"

  const val REPO_GITHUB = "github"
  const val REPO_GITLAB = "gitlab"

  const val SNAPSHOT_DEFAULT = "notify"
  const val SNAPSHOT_KEEP = "keep"
  const val SNAPSHOT_DISCARD = "discard"

  const val ACTION_APP_LIST = "com.absinthe.libchecker.intent.action.START_APP_LIST"
  const val ACTION_STATISTICS = "com.absinthe.libchecker.intent.action.START_STATISTICS"
  const val ACTION_SNAPSHOT = "com.absinthe.libchecker.intent.action.START_SNAPSHOT"

  const val COMMAND_DEBUG_MODE = "/debugmode"
  const val COMMAND_USER_MODE = "/usermode"
  const val COMMAND_DUMP_APPS_INFO_TXT = "/dumpAppsInfoTxt"
  const val COMMAND_DUMP_APPS_INFO_MD = "/dumpAppsInfoMd"

  const val RULES_DB_FILE_NAME = "rules.db"
  const val RULES_DATABASE_NAME = "rules_database"

  const val PREF_UUID = "uuid"
  const val PREF_ADVANCED_OPTIONS = "advancedOptions"
  const val PREF_ITEM_ADVANCED_OPTIONS = "itemAdvancedOptions"
  const val PREF_LIB_REF_OPTIONS = "libRefOptions"
  const val PREF_SNAPSHOT_OPTIONS = "snapshotOptions"

  const val PP_FROM_CLOUD_RULES_UPDATE = "ruleDatabaseUpdate"

  const val PREF_DETAILED_ABI_CHART = "detailedAbiChart"
  const val PREF_RULE_LANGUAGE = "ruleLanguage"
  const val PREF_SNAPSHOT_AUTO_REMOVE_THRESHOLD = "snapshotAutoRemoveThreshold"

  object Event {
    const val LAUNCH_ACTION = "Launch Action"
    const val SNAPSHOT_CLICK = "Snapshot Click"
    const val SETTINGS = "Settings"
    const val LIB_REFERENCE_FILTER_TYPE = "Lib Reference Filter Type"
    const val EASTER_EGG = "Easter Egg"
    const val SNAPSHOT_DETAIL_COMPONENT_COUNT = "Snapshot Detail Component Count"
  }

  object PackageNames {
    const val MATERIAL_FILES = "me.zhanghai.android.files"
    const val SHIZUKU = "moe.shizuku.privileged.api"
    const val SYSTEMUI = "com.android.systemui"
    const val ANYWHERE_ = "com.absinthe.anywhere_"
  }
}
