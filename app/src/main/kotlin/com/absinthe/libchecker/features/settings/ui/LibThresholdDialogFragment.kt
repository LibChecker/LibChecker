package com.absinthe.libchecker.features.settings.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.Telemetry
import kotlinx.coroutines.launch

class LibThresholdDialogFragment : DialogFragment() {

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val view = LibReferenceThresholdView(requireContext())
    view.count.setText(GlobalValues.libReferenceThreshold.toString())

    return BaseAlertDialogBuilder(requireContext())
      .setView(view)
      .setTitle(R.string.lib_ref_threshold)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        val threshold = view.slider.value.toInt()
        lifecycleScope.launch {
          GlobalValues.libReferenceThreshold = threshold
          GlobalValues.preferencesFlow.emit(Constants.PREF_LIB_REF_THRESHOLD to threshold)
        }
        Telemetry.recordEvent(
          Constants.Event.SETTINGS,
          mapOf("PREF_LIB_REF_THRESHOLD" to threshold.toLong())
        )
      }
      .setNegativeButton(android.R.string.cancel, null)
      .create()
  }
}
