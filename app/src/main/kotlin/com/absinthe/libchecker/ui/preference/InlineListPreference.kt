package com.absinthe.libchecker.ui.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference

class InlineListPreference(
  context: Context,
  attrs: AttributeSet?
) : ListPreference(context, attrs) {

  override fun onClick() = Unit
}
