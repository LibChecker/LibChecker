package com.absinthe.libchecker.features.settings.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import com.absinthe.libchecker.domain.settings.GetUpdatesAction
import com.absinthe.libchecker.features.settings.SettingsViewModel
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class GetUpdatesDialogFragment : BaseBottomSheetViewDialogFragment<GetUpdatesDialogView>() {

  private val viewModel: SettingsViewModel by viewModel(ownerProducer = { requireParentFragment() })

  override fun initRootView(): GetUpdatesDialogView = GetUpdatesDialogView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.addPaddingTop(16.dp)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    root.setItems(viewModel.buildGetUpdatesItems(), ::handleAction)
  }

  private fun handleAction(action: GetUpdatesAction) {
    when (action) {
      is GetUpdatesAction.OpenUri -> launchUri(action.uri)

      GetUpdatesAction.OpenInAppUpdate -> {
        InAppUpdateDialogFragment().show(
          childFragmentManager,
          InAppUpdateDialogFragment::class.java.simpleName
        )
      }
    }
  }

  private fun launchUri(uri: String) {
    runCatching {
      val intent = Intent(Intent.ACTION_VIEW)
        .setData(uri.toUri())
      context?.startActivity(intent)
    }.onFailure { inner ->
      Timber.e(inner)
      Toasty.showShort(requireActivity(), "No browser application")
    }
  }
}
