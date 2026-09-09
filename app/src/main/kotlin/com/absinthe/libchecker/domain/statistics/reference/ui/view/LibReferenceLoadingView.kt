package com.absinthe.libchecker.domain.statistics.reference.ui.view

import android.content.Context
import android.util.AttributeSet
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceLoadingState
import com.absinthe.libchecker.view.app.DotLoadingView

class LibReferenceLoadingView(context: Context, attributeSet: AttributeSet? = null) : DotLoadingView(context, attributeSet) {
  init {
    setProgress(null)
  }

  fun bind(state: LibReferenceLoadingState) {
    setProgress(state.overallProgress)
  }
}
