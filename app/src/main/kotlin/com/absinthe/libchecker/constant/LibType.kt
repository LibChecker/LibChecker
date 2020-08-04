package com.absinthe.libchecker.constant

import androidx.annotation.IntDef

/**
 * <pre>
 * author : Absinthe
 * time : 2020/08/04
 * </pre>
 */
const val ALL = -1
const val NATIVE = 0
const val SERVICE = 1
const val ACTIVITY = 2
const val RECEIVER = 3
const val PROVIDER = 4

@IntDef(ALL, NATIVE, SERVICE, ACTIVITY, RECEIVER, PROVIDER)
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class LibType