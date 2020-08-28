package com.absinthe.libchecker.annotation

import androidx.annotation.IntDef

/**
 * <pre>
 * author : Absinthe
 * time : 2020/08/28
 * </pre>
 */

const val SPRING = 0
const val SUMMER = 1
const val AUTUMN = 2
const val WINTER = 3

@IntDef(SPRING, SUMMER, AUTUMN, WINTER)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Season