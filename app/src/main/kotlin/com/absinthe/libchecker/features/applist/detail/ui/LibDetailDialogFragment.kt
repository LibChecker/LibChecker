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
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.applist.detail.ui.view.LibDetailBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        val rule = if (isValidLib) {
          RulesRepository.getRule(libName, type, true)
        } else {
          null
        }
        val iconRes = rule?.iconRes ?: com.absinthe.lc.rulesbundle.R.drawable.ic_sdk_placeholder
        setLoadingIcon(iconRes, rule?.isSimpleColorIcon == true)
        icon.load(iconRes) {
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
    lifecycleScope.launch(Dispatchers.IO) {
      runCatching {
        val detail = if (regexName.isNullOrEmpty()) {
          viewModel.requestLibDetail(libName, type)
        } else {
          viewModel.requestLibDetail(regexName!!, type, true)
        }
        if (detail != null) {
          root.apply {
            withContext(Dispatchers.Main) {
              setLibDetailBean(detail)
              showContent()
            }

            val sourceLink = detail.data[0].data.source_link
            if (sourceLink.startsWith(URLManager.GITHUB_HOST) && GlobalValues.isGitHubReachable) {
              val splits = sourceLink.removePrefix(URLManager.GITHUB_HOST).split("/")
              if (splits.size < 2) {
                return@launch
              }
              val date = viewModel.getRepoUpdatedTime(splits[0], splits[1]) ?: return@launch
              withContext(Dispatchers.Main) {
                root.setUpdateTIme(date)
              }
            }
          }
        } else {
          if (isStickyEventReceived) {
            withContext(Dispatchers.Main) {
              root.showNotFound()
            }
          } else {
            isStickyEventReceived = true
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
