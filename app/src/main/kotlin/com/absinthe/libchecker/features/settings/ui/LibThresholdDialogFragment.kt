package com.absinthe.libchecker.features.settings.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties

class LibThresholdDialogFragment : DialogFragment() {

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val view = LibReferenceThresholdView(requireContext())
    view.count.setText(GlobalValues.libReferenceThreshold.toString())

    return BaseAlertDialogBuilder(requireContext())
      .setView(view)
      .setTitle(R.string.lib_ref_threshold)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        val threshold = view.slider.value.toInt()
        GlobalValues.libReferenceThreshold = threshold
        GlobalValues.libReferenceThresholdLiveData.value = threshold
        Analytics.trackEvent(
          Constants.Event.SETTINGS,
          EventProperties().set("PREF_LIB_REF_THRESHOLD", threshold.toLong())
        )
      }
      .setNegativeButton(android.R.string.cancel, null)
      .create()
  }
}
