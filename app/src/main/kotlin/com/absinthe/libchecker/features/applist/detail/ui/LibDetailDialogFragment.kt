package com.absinthe.libchecker.features.applist.detail.ui

import android.content.DialogInterface
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.app.detail.action.GetLibraryDetailDialogDataUseCase
import com.absinthe.libchecker.domain.app.detail.ui.view.LibDetailBottomSheetView
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber

const val EXTRA_LIB_NAME = "EXTRA_LIB_NAME"
const val EXTRA_LIB_TYPE = "EXTRA_LIB_TYPE"
const val EXTRA_REGEX_NAME = "EXTRA_REGEX_NAME"
const val EXTRA_IS_VALID_LIB = "EXTRA_IS_VALID_LIB"

class LibDetailDialogFragment : BaseBottomSheetViewDialogFragment<LibDetailBottomSheetView>() {

  private val libName by lazy { arguments?.getString(EXTRA_LIB_NAME).orEmpty() }
  private val type by lazy { arguments?.getInt(EXTRA_LIB_TYPE) ?: NATIVE }
  private val regexName by lazy { arguments?.getString(EXTRA_REGEX_NAME) }
  private val isValidLib by lazy { arguments?.getBoolean(EXTRA_IS_VALID_LIB) != false }
  private val viewModel: DetailViewModel by activityViewModel()
  private var isStickyEventReceived = false

  override fun initRootView(): LibDetailBottomSheetView = LibDetailBottomSheetView(requireContext())

  override fun init() {
    root.apply {
      maxPeekHeightPercentage = 0.8f
      title.text = if (type == ACTION) "< $libName >" else libName
      lifecycleScope.launch {
        val header = viewModel.getLibraryDetailDialogHeader(
          libName = libName,
          type = type,
          isValidLib = isValidLib
        )
        setLoadingIcon(header.iconRes, header.isSimpleColorIcon)
        icon.load(header.iconRes) {
          crossfade(true)
          placeholder(R.drawable.ic_logo)
        }
      }
    }
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun onStart() {
    super.onStart()
    if (!isValidLib) {
      root.showNotFound()
      return
    }
    lifecycleScope.launch {
      runCatching {
        when (
          val result = viewModel.getLibraryDetailDialogData(
            libName = libName,
            type = type,
            regexName = regexName,
            isValidLib = isValidLib
          )
        ) {
          is GetLibraryDetailDialogDataUseCase.Result.Found -> {
            root.apply {
              setLibDetailBean(result.detail)
              showContent()
              result.repoUpdatedTime?.let(::setUpdateTIme)
            }
          }

          GetLibraryDetailDialogDataUseCase.Result.NotFound -> {
            showNotFoundAfterStickyEvent()
          }
        }
      }.onFailure {
        Timber.e(it)
        context?.showToast(it.message.toString())
      }.also {
        Telemetry.recordEvent(
          Constants.Event.LIB_DETAIL_DIALOG,
          mapOf(
            Telemetry.Param.CONTENT to libName,
            Telemetry.Param.CONTENT_TYPE to type,
            Telemetry.Param.SUCCESS to (it.isSuccess && isValidLib)
          )
        )
      }
    }
  }

  private fun showNotFoundAfterStickyEvent() {
    if (isStickyEventReceived) {
      root.showNotFound()
    } else {
      isStickyEventReceived = true
    }
  }

  override fun show(manager: FragmentManager, tag: String?) {
    if (!isShowing) {
      isShowing = true
      super.show(manager, tag)
    }
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    isShowing = false
  }

  companion object {
    fun newInstance(
      libName: String,
      @LibType type: Int,
      regexName: String? = null,
      isValidLib: Boolean = true
    ): LibDetailDialogFragment {
      return LibDetailDialogFragment().putArguments(
        EXTRA_LIB_NAME to libName,
        EXTRA_LIB_TYPE to type,
        EXTRA_REGEX_NAME to regexName,
        EXTRA_IS_VALID_LIB to isValidLib
      )
    }

    var isShowing = false
  }
}
