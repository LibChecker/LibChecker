package com.absinthe.libchecker.annotation

import androidx.annotation.IntDef

const val JAVA = 0
const val KOTLIN = 1
const val ANDROID = 2

@IntDef(JAVA, KOTLIN, ANDROID)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class RxType
