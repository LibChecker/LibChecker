@file:Suppress("unused")

package com.absinthe.libchecker.utils

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

object JsonUtil {
  internal val moshi: Moshi = Moshi.Builder().build()

  fun <T> fromJson(json: String, clazz: Class<T>): T? = try {
    moshi.adapter(clazz).fromJson(json)
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }

  fun <T> fromJson(json: String, typeOfT: Type): T? = try {
    moshi.adapter<T>(typeOfT).fromJson(json)
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }

  fun <T> fromJson(json: String, rawType: Class<*>, vararg typeArguments: Class<*>): T? = try {
    fromJson(json, Types.newParameterizedType(rawType, *typeArguments))
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }

  fun <T> toJson(o: T?, clazz: Class<T>): String? = try {
    moshi.adapter(clazz).toJson(o)
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }

  inline fun <reified T> toJson(o: T?): String? = toJson(o, T::class.java)
}

inline fun <reified T> String.fromJson(): T? = JsonUtil.fromJson(this, T::class.java)

inline fun <reified T> String.fromJson(typeOfT: Type): T? = JsonUtil.fromJson(this, typeOfT)

inline fun <reified T> String.fromJson(rawType: Class<*>, vararg typeArguments: Class<*>): T? =
  JsonUtil.fromJson(this, rawType, *typeArguments)

fun Any?.toJson(): String? = JsonUtil.toJson(this)
