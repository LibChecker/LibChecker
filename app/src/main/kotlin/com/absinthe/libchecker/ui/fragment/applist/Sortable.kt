package com.absinthe.libchecker.ui.fragment.applist

import java.lang.ref.WeakReference

interface Sortable {
    fun sort()

    companion object {
        var currentReference: WeakReference<Sortable>? = null
    }
}