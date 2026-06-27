package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.content.Context
import android.content.DialogInterface
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.action.XposedModuleInfo
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.ui.FeaturesDialog
import com.absinthe.libchecker.domain.app.detail.ui.adapter.node.XposedDetailItem
import com.absinthe.libchecker.domain.app.detail.ui.view.XposedInfoBottomSheetView
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class XposedInfoDialogFragment : BaseBottomSheetViewDialogFragment<XposedInfoBottomSheetView>() {

  private val viewModel: DetailViewModel by activityViewModel()
  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME).orEmpty() }

  override fun initRootView(): XposedInfoBottomSheetView = XposedInfoBottomSheetView(requireContext())

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    lifecycleScope.launch {
      val info = viewModel.getXposedModuleInfo(packageName)
      val context = context ?: return@launch
      if (info == null) {
        dismiss()
        FeaturesDialog.showXPosedDialog(context)
        return@launch
      }

      root.apply {
        setting.setText(info.appName)
        setting.setOnClickListener {
          openXposedSettings(info)
        }
        contentAdapter.setList(info.toDetailItems(context))
      }
    }
  }

  private fun openXposedSettings(info: XposedModuleInfo) {
    val activity = activity ?: return
    val settingsIntent = info.settingsIntent ?: run {
      Toasty.showShort(activity, R.string.toast_cant_open_app)
      return
    }
    runCatching {
      activity.startActivity(settingsIntent)
    }.onFailure {
      Toasty.showShort(activity, it.message.toString())
    }
  }

  private fun XposedModuleInfo.toDetailItems(context: Context): List<XposedDetailItem> {
    val titleSmall = context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceTitleSmall)
    val bodyMedium = context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBodyMedium)
    val list = mutableListOf<XposedDetailItem>()

    minVersion?.takeIf(String::isNotBlank)?.let {
      list.add(
        XposedDetailItem(
          iconRes = R.drawable.ic_app_prop,
          tip = context.getString(R.string.lib_detail_xposed_min_version),
          text = it,
          textStyleRes = titleSmall
        )
      )
    }

    targetVersion?.takeIf(String::isNotBlank)?.let {
      list.add(
        XposedDetailItem(
          iconRes = R.drawable.ic_app_prop,
          tip = context.getString(R.string.lib_detail_xposed_target_version),
          text = it,
          textStyleRes = titleSmall
        )
      )
    }

    if (staticScope) {
      list.add(
        XposedDetailItem(
          iconRes = R.drawable.ic_app_prop,
          tip = context.getString(R.string.lib_detail_xposed_static_scope),
          text = "True",
          textStyleRes = titleSmall
        )
      )
    }

    defaultScope?.takeIf(String::isNotBlank)?.let {
      list.add(
        XposedDetailItem(
          iconRes = R.drawable.ic_app_prop,
          tip = context.getString(R.string.lib_detail_xposed_default_scope),
          text = it,
          textStyleRes = titleSmall
        )
      )
    }

    javaInitClasses?.takeIf(String::isNotBlank)?.let {
      list.add(
        XposedDetailItem(
          iconRes = R.drawable.ic_app_prop,
          tip = context.getString(R.string.lib_detail_xposed_init_class) + " (Java)",
          text = it,
          textStyleRes = titleSmall
        )
      )
    }

    nativeInitLibraries?.takeIf(String::isNotBlank)?.let {
      list.add(
        XposedDetailItem(
          iconRes = R.drawable.ic_app_prop,
          tip = context.getString(R.string.lib_detail_xposed_init_class) + " (Native)",
          text = it,
          textStyleRes = titleSmall
        )
      )
    }

    legacyInitClass?.takeIf(String::isNotBlank)?.let {
      list.add(
        XposedDetailItem(
          iconRes = R.drawable.ic_app_prop,
          tip = context.getString(R.string.lib_detail_xposed_init_class) + " (Legacy)",
          text = it,
          textStyleRes = titleSmall
        )
      )
    }

    description?.takeIf(String::isNotBlank)?.let {
      list.add(
        XposedDetailItem(
          iconRes = R.drawable.ic_content,
          tip = context.getString(R.string.lib_detail_description_tip),
          text = it,
          textStyleRes = bodyMedium
        )
      )
    }

    return list
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

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
    fun newInstance(packageName: String): XposedInfoDialogFragment {
      return XposedInfoDialogFragment().putArguments(EXTRA_PACKAGE_NAME to packageName)
    }

    var isShowing = false
  }
}
