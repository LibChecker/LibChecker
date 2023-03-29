package com.absinthe.libchecker.model

import androidx.annotation.DrawableRes

data class FeatureItem(@DrawableRes val res: Int, val action: () -> Unit)
