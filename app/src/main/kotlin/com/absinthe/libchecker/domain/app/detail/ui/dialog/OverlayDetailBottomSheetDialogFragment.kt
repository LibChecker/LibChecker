package com.absinthe.libchecker.domain.app.detail.ui.dialog

import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.model.OverlayDetailAction
import com.absinthe.libchecker.domain.app.detail.model.OverlayDetailBottomSheetResult
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_LC_ITEM
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.view.OverlayDetailBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class OverlayDetailBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<OverlayDetailBottomSheetView>() {

  private val viewModel: DetailViewModel by activityViewModel()

  override fun initRootView(): OverlayDetailBottomSheetView {
    return OverlayDetailBottomSheetView(requireContext())
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    val lcItem = arguments?.let {
      BundleCompat.getParcelable(it, EXTRA_LC_ITEM, LCItem::class.java)
    } ?: return

    lifecycleScope.launch {
      when (val result = viewModel.getOverlayDetailBottomSheetResult(lcItem)) {
        OverlayDetailBottomSheetResult.NotFound -> {
          Toasty.showShort(requireContext(), R.string.toast_cant_open_app)
        }

        is OverlayDetailBottomSheetResult.Available -> {
          root.bind(result.display, ::handleAction)
        }
      }
    }
  }

  private fun handleAction(action: OverlayDetailAction) {
    when (action) {
      is OverlayDetailAction.OpenApp -> {
        activity?.launchDetailPage(action.item, forceDetail = action.forceDetail)
      }
    }
  }
}
