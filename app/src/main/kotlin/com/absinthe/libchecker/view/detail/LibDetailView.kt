package com.absinthe.libchecker.view.detail

import android.content.Context
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.absinthe.libchecker.databinding.LayoutDialogLibDetailBinding

class LibDetailView constructor(context: Context) : ConstraintLayout(context) {
    val binding: LayoutDialogLibDetailBinding = LayoutDialogLibDetailBinding
        .inflate(LayoutInflater.from(context), this, true)
}