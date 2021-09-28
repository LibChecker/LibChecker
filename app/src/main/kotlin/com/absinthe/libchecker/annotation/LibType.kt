package com.absinthe.libchecker.annotation

import androidx.annotation.IntDef

/**
 * <pre>
 * author : Absinthe
 * time : 2020/08/04
 * </pre>
 */
const val NOT_MARKED = -2
const val ALL = -1
const val NATIVE = 0
const val SERVICE = 1
const val ACTIVITY = 2
const val RECEIVER = 3
const val PROVIDER = 4
const val DEX = 5
const val STATIC = 6
const val PERMISSION = 7
const val METADATA = 8

@IntDef(ALL, NATIVE, SERVICE, ACTIVITY, RECEIVER, PROVIDER, DEX, STATIC, PERMISSION, METADATA)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class LibType
