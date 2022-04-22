package com.absinthe.libchecker.ui.fragment.settings

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.absinthe.libchecker.R
import com.absinthe.libchecker.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.view.settings.LibReferenceThresholdView
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties

class LibThresholdDialogFragment : DialogFragment() {

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

    val view = LibReferenceThresholdView(requireContext())
    view.count.text = GlobalValues.libReferenceThreshold.toString()

    return BaseAlertDialogBuilder(requireContext())
      .setView(view)
      .setTitle(R.string.lib_ref_threshold)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        val threshold = view.slider.value.toInt()
        GlobalValues.libReferenceThresholdLiveData.value = threshold
        GlobalValues.libReferenceThreshold = threshold
        Analytics.trackEvent(
          Constants.Event.SETTINGS,
          EventProperties().set("PREF_LIB_REF_THRESHOLD", threshold.toLong())
        )
      }
      .setNegativeButton(android.R.string.cancel, null)
      .create()
  }
}
