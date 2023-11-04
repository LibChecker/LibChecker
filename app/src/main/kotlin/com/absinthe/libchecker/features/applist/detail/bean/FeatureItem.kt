package com.absinthe.libchecker.features.applist.detail.bean

import androidx.annotation.DrawableRes

data class FeatureItem(@DrawableRes val res: Int, val action: () -> Unit)
