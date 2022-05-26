package com.absinthe.libchecker.privacy

import android.os.Process
import android.text.method.LinkMovementMethod
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import com.absinthe.libchecker.R
import com.absinthe.libchecker.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.extensions.dp
import com.google.android.material.textview.MaterialTextView

object Privacy {
  fun showPrivacyDialog(context: ContextThemeWrapper) {
    if (GlobalValues.agreedPrivacy) {
      return
    }
    BaseAlertDialogBuilder(context)
      .setTitle(R.string.privacy_title)
      .setView(
        MaterialTextView(context).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
          )
          setPadding(24.dp, 8.dp, 24.dp, 0)
          text = HtmlCompat.fromHtml(
            context.getString(R.string.privacy_content),
            HtmlCompat.FROM_HTML_MODE_LEGACY
          )
          movementMethod = LinkMovementMethod.getInstance()
        }
      )
      .setPositiveButton(R.string.agree) { dialog, _ ->
        GlobalValues.agreedPrivacy = true
        dialog.dismiss()
      }
      .setNegativeButton(R.string.deny) { _, _ -> Process.killProcess(Process.myPid()) }
      .setCancelable(false)
      .show()
  }
}
