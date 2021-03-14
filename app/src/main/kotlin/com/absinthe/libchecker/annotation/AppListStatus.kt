package com.absinthe.libchecker.annotation

import androidx.annotation.IntDef

const val STATUS_NOT_START = -1
const val STATUS_START = 0
const val STATUS_END = 1

@IntDef(STATUS_NOT_START, STATUS_START, STATUS_END)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class AppListStatus