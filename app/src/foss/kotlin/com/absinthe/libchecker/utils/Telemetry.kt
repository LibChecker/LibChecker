package com.absinthe.libchecker.utils

object Telemetry {

  fun setEnable(enable: Boolean) {
    // Firebase.analytics.setAnalyticsCollectionEnabled(false)
    // Firebase.crashlytics.setCrashlyticsCollectionEnabled(false)
  }

  fun recordException(t: Throwable) {
    // Firebase.crashlytics.recordException(t)
  }

  fun recordEvent(name: String, params: Map<String, Any>) {
    // Firebase.analytics.logEvent(name, params)
  }

  object Event {
    const val APP_OPEN = ""
    const val GENERATE_LEAD = ""
    const val JOIN_GROUP = ""
    const val LEVEL_END = ""
    const val LEVEL_START = ""
    const val LEVEL_UP = ""
    const val LOGIN = ""
    const val POST_SCORE = ""
    const val SEARCH = ""
    const val SELECT_CONTENT = ""
    const val SHARE = ""
    const val SIGN_UP = ""
    const val TUTORIAL_BEGIN = ""
    const val TUTORIAL_COMPLETE = ""
    const val UNLOCK_ACHIEVEMENT = ""
    const val VIEW_ITEM = ""
    const val VIEW_ITEM_LIST = ""
    const val VIEW_SEARCH_RESULTS = ""
    const val SCREEN_VIEW = ""
    const val SELECT_ITEM = ""
  }

  object Param {
    const val ACHIEVEMENT_ID = ""
    const val CHARACTER = ""
    const val TRAVEL_CLASS = ""
    const val CONTENT_TYPE = ""
    const val START_DATE = ""
    const val END_DATE = ""
    const val EXTEND_SESSION = ""
    const val GROUP_ID = ""
    const val ITEM_CATEGORY = ""
    const val ITEM_ID = ""
    const val ITEM_NAME = ""
    const val LOCATION = ""
    const val LEVEL = ""
    const val LEVEL_NAME = ""
    const val METHOD = ""
    const val NUMBER_OF_NIGHTS = ""
    const val NUMBER_OF_PASSENGERS = ""
    const val NUMBER_OF_ROOMS = ""
    const val DESTINATION = ""
    const val ORIGIN = ""
    const val SCORE = ""
    const val SEARCH_TERM = ""
    const val SUCCESS = ""
    const val VALUE = ""
    const val CAMPAIGN = ""
    const val SOURCE = ""
    const val MEDIUM = ""
    const val TERM = ""
    const val CONTENT = ""
    const val CAMPAIGN_ID = ""
    const val SOURCE_PLATFORM = ""
    const val CREATIVE_FORMAT = ""
    const val MARKETING_TACTIC = ""
    const val ITEM_BRAND = ""
    const val ITEM_VARIANT = ""
    const val CREATIVE_NAME = ""
    const val CREATIVE_SLOT = ""
    const val AFFILIATION = ""
    const val INDEX = ""
    const val ITEM_CATEGORY2 = ""
    const val ITEM_CATEGORY3 = ""
    const val ITEM_CATEGORY4 = ""
    const val ITEM_CATEGORY5 = ""
    const val ITEM_LIST_ID = ""
    const val ITEM_LIST_NAME = ""
    const val ITEMS = ""
    const val LOCATION_ID = ""
    const val SCREEN_CLASS = ""
    const val SCREEN_NAME = ""
    const val SHIPPING_TIER = ""
  }
}
