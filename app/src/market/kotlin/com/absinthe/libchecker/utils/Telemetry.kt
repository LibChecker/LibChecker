package com.absinthe.libchecker.utils

import androidx.core.os.bundleOf
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics

object Telemetry {

  fun setEnable(enable: Boolean) {
    Firebase.analytics.setAnalyticsCollectionEnabled(enable)
    Firebase.crashlytics.setCrashlyticsCollectionEnabled(enable)
  }

  fun recordException(t: Throwable) {
    Firebase.crashlytics.recordException(t)
  }

  fun recordEvent(name: String, params: Map<String, Any>) {
    Firebase.analytics.logEvent(name.replace(" ", "_"), bundleOf(*params.toList().toTypedArray()))
  }

  object Event {
    const val APP_OPEN = FirebaseAnalytics.Event.APP_OPEN
    const val GENERATE_LEAD = FirebaseAnalytics.Event.GENERATE_LEAD
    const val JOIN_GROUP = FirebaseAnalytics.Event.JOIN_GROUP
    const val LEVEL_END = FirebaseAnalytics.Event.LEVEL_END
    const val LEVEL_START = FirebaseAnalytics.Event.LEVEL_START
    const val LEVEL_UP = FirebaseAnalytics.Event.LEVEL_UP
    const val LOGIN = FirebaseAnalytics.Event.LOGIN
    const val POST_SCORE = FirebaseAnalytics.Event.POST_SCORE
    const val SEARCH = FirebaseAnalytics.Event.SEARCH
    const val SELECT_CONTENT = FirebaseAnalytics.Event.SELECT_CONTENT
    const val SHARE = FirebaseAnalytics.Event.SHARE
    const val SIGN_UP = FirebaseAnalytics.Event.SIGN_UP
    const val TUTORIAL_BEGIN = FirebaseAnalytics.Event.TUTORIAL_BEGIN
    const val TUTORIAL_COMPLETE = FirebaseAnalytics.Event.TUTORIAL_COMPLETE
    const val UNLOCK_ACHIEVEMENT = FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT
    const val VIEW_ITEM = FirebaseAnalytics.Event.VIEW_ITEM
    const val VIEW_ITEM_LIST = FirebaseAnalytics.Event.VIEW_ITEM_LIST
    const val VIEW_SEARCH_RESULTS = FirebaseAnalytics.Event.VIEW_SEARCH_RESULTS
    const val SCREEN_VIEW = FirebaseAnalytics.Event.SCREEN_VIEW
    const val SELECT_ITEM = FirebaseAnalytics.Event.SELECT_ITEM
  }

  object Param {
    const val ACHIEVEMENT_ID = FirebaseAnalytics.Param.ACHIEVEMENT_ID
    const val CHARACTER = FirebaseAnalytics.Param.CHARACTER
    const val TRAVEL_CLASS = FirebaseAnalytics.Param.TRAVEL_CLASS
    const val CONTENT_TYPE = FirebaseAnalytics.Param.CONTENT_TYPE
    const val START_DATE = FirebaseAnalytics.Param.START_DATE
    const val END_DATE = FirebaseAnalytics.Param.END_DATE
    const val EXTEND_SESSION = FirebaseAnalytics.Param.EXTEND_SESSION
    const val GROUP_ID = FirebaseAnalytics.Param.GROUP_ID
    const val ITEM_CATEGORY = FirebaseAnalytics.Param.ITEM_CATEGORY
    const val ITEM_ID = FirebaseAnalytics.Param.ITEM_ID
    const val ITEM_NAME = FirebaseAnalytics.Param.ITEM_NAME
    const val LOCATION = FirebaseAnalytics.Param.LOCATION
    const val LEVEL = FirebaseAnalytics.Param.LEVEL
    const val LEVEL_NAME = FirebaseAnalytics.Param.LEVEL_NAME
    const val METHOD = FirebaseAnalytics.Param.METHOD
    const val NUMBER_OF_NIGHTS = FirebaseAnalytics.Param.NUMBER_OF_NIGHTS
    const val NUMBER_OF_PASSENGERS = FirebaseAnalytics.Param.NUMBER_OF_PASSENGERS
    const val NUMBER_OF_ROOMS = FirebaseAnalytics.Param.NUMBER_OF_ROOMS
    const val DESTINATION = FirebaseAnalytics.Param.DESTINATION
    const val ORIGIN = FirebaseAnalytics.Param.ORIGIN
    const val SCORE = FirebaseAnalytics.Param.SCORE
    const val SEARCH_TERM = FirebaseAnalytics.Param.SEARCH_TERM
    const val SUCCESS = FirebaseAnalytics.Param.SUCCESS
    const val VALUE = FirebaseAnalytics.Param.VALUE
    const val CAMPAIGN = FirebaseAnalytics.Param.CAMPAIGN
    const val SOURCE = FirebaseAnalytics.Param.SOURCE
    const val MEDIUM = FirebaseAnalytics.Param.MEDIUM
    const val TERM = FirebaseAnalytics.Param.TERM
    const val CONTENT = FirebaseAnalytics.Param.CONTENT
    const val CAMPAIGN_ID = FirebaseAnalytics.Param.CAMPAIGN_ID
    const val SOURCE_PLATFORM = FirebaseAnalytics.Param.SOURCE_PLATFORM
    const val CREATIVE_FORMAT = FirebaseAnalytics.Param.CREATIVE_FORMAT
    const val MARKETING_TACTIC = FirebaseAnalytics.Param.MARKETING_TACTIC
    const val ITEM_BRAND = FirebaseAnalytics.Param.ITEM_BRAND
    const val ITEM_VARIANT = FirebaseAnalytics.Param.ITEM_VARIANT
    const val CREATIVE_NAME = FirebaseAnalytics.Param.CREATIVE_NAME
    const val CREATIVE_SLOT = FirebaseAnalytics.Param.CREATIVE_SLOT
    const val AFFILIATION = FirebaseAnalytics.Param.AFFILIATION
    const val INDEX = FirebaseAnalytics.Param.INDEX
    const val ITEM_CATEGORY2 = FirebaseAnalytics.Param.ITEM_CATEGORY2
    const val ITEM_CATEGORY3 = FirebaseAnalytics.Param.ITEM_CATEGORY3
    const val ITEM_CATEGORY4 = FirebaseAnalytics.Param.ITEM_CATEGORY4
    const val ITEM_CATEGORY5 = FirebaseAnalytics.Param.ITEM_CATEGORY5
    const val ITEM_LIST_ID = FirebaseAnalytics.Param.ITEM_LIST_ID
    const val ITEM_LIST_NAME = FirebaseAnalytics.Param.ITEM_LIST_NAME
    const val ITEMS = FirebaseAnalytics.Param.ITEMS
    const val LOCATION_ID = FirebaseAnalytics.Param.LOCATION_ID
    const val SCREEN_CLASS = FirebaseAnalytics.Param.SCREEN_CLASS
    const val SCREEN_NAME = FirebaseAnalytics.Param.SCREEN_NAME
    const val SHIPPING_TIER = FirebaseAnalytics.Param.SHIPPING_TIER
  }
}
