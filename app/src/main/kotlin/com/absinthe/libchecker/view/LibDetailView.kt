package com.absinthe.libchecker.view

import android.content.Context
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.absinthe.libchecker.R

class LibDetailView(context: Context) : ConstraintLayout(context) {

    init {
        View.inflate(context, R.layout.layout_dialog_lib_detail, this)
    }

}