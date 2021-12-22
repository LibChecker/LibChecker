package com.absinthe.libchecker.bean

import androidx.annotation.DrawableRes

data class FeatureItem(@DrawableRes val res: Int, val action: () -> Unit)
