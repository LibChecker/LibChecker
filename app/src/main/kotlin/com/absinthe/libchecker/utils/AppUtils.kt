package com.absinthe.libchecker.utils

import com.absinthe.libchecker.annotation.AUTUMN
import com.absinthe.libchecker.annotation.SPRING
import com.absinthe.libchecker.annotation.SUMMER
import com.absinthe.libchecker.annotation.WINTER
import java.util.*

object AppUtils {

    fun getCurrentSeason(): Int {
        return when(Calendar.getInstance(Locale.getDefault()).get(Calendar.MONTH) + 1) {
            3, 4, 5 -> SPRING
            6, 7, 8 -> SUMMER
            9, 10, 11 -> AUTUMN
            12, 1, 2 -> WINTER
            else -> -1
        }
    }

}