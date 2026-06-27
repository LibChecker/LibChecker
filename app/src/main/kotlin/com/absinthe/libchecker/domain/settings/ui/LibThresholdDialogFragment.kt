package com.absinthe.libchecker.domain.settings.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.putArguments

class LibThresholdDialogFragment : DialogFragment() {
  private var onThresholdSelected: (Int) -> Unit = {}

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val view = LibReferenceThresholdView(
      requireContext(),
      arguments?.getInt(EXTRA_THRESHOLD) ?: DEFAULT_THRESHOLD
    )

    return BaseAlertDialogBuilder(requireContext())
      .setView(view)
      .setTitle(R.string.lib_ref_threshold)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        val threshold = view.slider.value.toInt()
        onThresholdSelected(threshold)
        Telemetry.recordEvent(
          Constants.Event.SETTINGS,
          mapOf(Telemetry.Param.CONTENT to Constants.PREF_LIB_REF_THRESHOLD, Telemetry.Param.VALUE to threshold.toLong())
        )
      }
      .setNegativeButton(android.R.string.cancel, null)
      .create()
  }

  fun setOnThresholdSelectedListener(action: (Int) -> Unit) {
    onThresholdSelected = action
  }

  companion object {
    private const val EXTRA_THRESHOLD = "EXTRA_THRESHOLD"
    private const val DEFAULT_THRESHOLD = 2

    fun newInstance(threshold: Int): LibThresholdDialogFragment {
      return LibThresholdDialogFragment().putArguments(EXTRA_THRESHOLD to threshold)
    }
  }
}
