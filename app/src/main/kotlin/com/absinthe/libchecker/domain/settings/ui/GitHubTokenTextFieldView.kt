package com.absinthe.libchecker.domain.settings.ui

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.OsUtils
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class GitHubTokenTextFieldView(context: Context) : LinearLayout(context) {

  private val textInputLayout = TextInputLayout(
    context,
    null,
    com.google.android.material.R.attr.textInputOutlinedStyle
  ).apply {
    layoutParams = LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    hint = context.getString(R.string.settings_github_token_dialog_hint)
    isSaveEnabled = false
    if (OsUtils.atLeastO()) {
      importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    }
  }

  private val input = TextInputEditText(textInputLayout.context).apply {
    layoutParams = LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setSingleLine()
    inputType = InputType.TYPE_CLASS_TEXT or
      InputType.TYPE_TEXT_VARIATION_PASSWORD or
      InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
    imeOptions = EditorInfo.IME_ACTION_DONE
    isSaveEnabled = false
    if (OsUtils.atLeastO()) {
      importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
    }
  }

  var token: String
    get() = input.text?.toString()?.trim().orEmpty()
    set(value) {
      input.setText(value)
      input.setSelection(input.text?.length ?: 0)
    }

  init {
    orientation = VERTICAL
    textInputLayout.addView(input)
    addView(textInputLayout)
  }
}
