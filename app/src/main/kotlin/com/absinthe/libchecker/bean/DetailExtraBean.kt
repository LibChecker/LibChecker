package com.absinthe.libchecker.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DetailExtraBean(
  val isSplitApk: Boolean,
  val isKotlinUsed: Boolean?,
  val isRxJavaUsed: Boolean?,
  val isRxKotlinUsed: Boolean?,
  val variant: Short
) : Parcelable
