package com.absinthe.libchecker.features.settings.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.statistics.LibReferenceSettingsRepository
import com.absinthe.libchecker.domain.statistics.UpdateLibReferenceThresholdUseCase
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.Telemetry
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class LibThresholdDialogFragment : DialogFragment() {
  private val libReferenceSettingsRepository: LibReferenceSettingsRepository by inject()
  private val updateLibReferenceThresholdUseCase: UpdateLibReferenceThresholdUseCase by inject()

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val view = LibReferenceThresholdView(
      requireContext(),
      libReferenceSettingsRepository.threshold
    )

    return BaseAlertDialogBuilder(requireContext())
      .setView(view)
      .setTitle(R.string.lib_ref_threshold)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        val threshold = view.slider.value.toInt()
        lifecycleScope.launch {
          updateLibReferenceThresholdUseCase(threshold)
        }
        Telemetry.recordEvent(
          Constants.Event.SETTINGS,
          mapOf(Telemetry.Param.CONTENT to Constants.PREF_LIB_REF_THRESHOLD, Telemetry.Param.VALUE to threshold.toLong())
        )
      }
      .setNegativeButton(android.R.string.cancel, null)
      .create()
  }
}
