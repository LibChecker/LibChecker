package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.content.DialogInterface
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.app.detail.action.DetailItemResolver
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailBottomSheetState
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailHeaderDisplay
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.view.LibDetailBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
  private val dialogTitle by lazy {
    if (type == ACTION) "< $libName >" else libName
  }

  override fun initRootView(): LibDetailBottomSheetView {
    return LibDetailBottomSheetView(requireContext()) { locale ->
      GlobalValues.preferredRuleLanguage = locale
    }
  }

  override fun init() {
    maxPeekHeightPercentage = 0.8f
    root.bind(LibraryDetailBottomSheetState.Loading(dialogTitle))
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun onStart() {
    super.onStart()
    if (!isValidLib) {
      root.bind(
        LibraryDetailBottomSheetState.NotFound(
          title = dialogTitle,
          header = fallbackHeader
        )
      )
      return
    }
    lifecycleScope.launch {
      runCatching {
        coroutineScope {
          val headerDeferred = async {
            viewModel.getLibraryDetailDialogHeader(
              libName = libName,
              type = type,
              isValidLib = isValidLib
            )
          }
          val contentDeferred = async {
            viewModel.getLibraryDetailDialogData(
              libName = libName,
              type = type,
              regexName = regexName,
              isValidLib = isValidLib,
              preferredLocale = GlobalValues.preferredRuleLanguage
            )
          }
          val header = headerDeferred.await()
          root.bind(LibraryDetailBottomSheetState.Loading(dialogTitle, header))
          header to contentDeferred.await()
        }
      }.onSuccess { (header, result) ->
        when (result) {
          is DetailItemResolver.Result.Found -> {
            root.bind(
              LibraryDetailBottomSheetState.Content(
                title = dialogTitle,
                header = header,
                content = result.content
              )
            )
          }

          DetailItemResolver.Result.NotFound -> {
            showNotFoundAfterStickyEvent(header)
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

  private fun showNotFoundAfterStickyEvent(header: LibraryDetailHeaderDisplay) {
    if (isStickyEventReceived) {
      root.bind(
        LibraryDetailBottomSheetState.NotFound(
          title = dialogTitle,
          header = header
        )
      )
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
    private val fallbackHeader = LibraryDetailHeaderDisplay(
      iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_sdk_placeholder,
      isSimpleColorIcon = false
    )

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
