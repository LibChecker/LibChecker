package com.absinthe.libchecker.domain.statistics.reference.model

import android.content.pm.PackageInfo
import android.os.Parcelable
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.rulesbundle.Rule
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class LibReference(
  val libName: String,
  val rule: Rule?,
  val referredList: Set<String>,
  @LibType val type: Int,
  @IgnoredOnParcel
  val iconPackages: List<PackageInfo> = emptyList(),
  val resolvedLabel: String? = null
) : Parcelable
