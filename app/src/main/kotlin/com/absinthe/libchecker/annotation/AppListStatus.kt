package com.absinthe.libchecker.annotation

import androidx.annotation.IntDef

const val STATUS_NOT_START = -1
const val STATUS_START_INIT = 0
const val STATUS_INIT_END = 1
const val STATUS_START_REQUEST_CHANGE = 3
const val STATUS_START_REQUEST_CHANGE_END = 4

@IntDef(
  STATUS_NOT_START,
  STATUS_START_INIT,
  STATUS_INIT_END,
  STATUS_START_REQUEST_CHANGE,
  STATUS_START_REQUEST_CHANGE_END
)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class AppListStatus
