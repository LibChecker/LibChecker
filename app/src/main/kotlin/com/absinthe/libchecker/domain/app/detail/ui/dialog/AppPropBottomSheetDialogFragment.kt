package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.domain.app.detail.model.AppPropItem
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_INFO
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PROPS
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_TEXT
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.resource.AppResourcePreview
import com.absinthe.libchecker.domain.app.detail.resource.ResolveAppResourceValueUseCase
import com.absinthe.libchecker.domain.app.detail.resource.ResolveAppResourceValueUseCase.AppResourceValue
import com.absinthe.libchecker.domain.app.detail.ui.view.AppPropsBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class AppPropBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<AppPropsBottomSheetView>() {

  private val viewModel: DetailViewModel by activityViewModel()
  private val resolveAppResourceValue: ResolveAppResourceValueUseCase by inject()
  private val packageInfo by lazy {
    BundleCompat.getParcelable(
      requireArguments(),
      EXTRA_PACKAGE_INFO,
      PackageInfo::class.java
    )
  }

  private val props by lazy {
    requireArguments().getString(EXTRA_PROPS)?.fromJson<Map<String, String>>()
  }

  override fun initRootView(): AppPropsBottomSheetView = AppPropsBottomSheetView(
    requireContext(),
    ::onResourceClick
  )

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    lifecycleScope.launch {
      root.bind(viewModel.getAppManifestProperties(packageInfo, props))
    }
  }

  private fun onResourceClick(item: AppPropItem) {
    if (item.isTransformed) {
      root.updateItem(item.restore())
      return
    }
    val resource = item.resource ?: return

    viewLifecycleOwner.lifecycleScope.launch {
      when (
        val resourceValue = withContext(Dispatchers.IO) {
          resolveAppResourceValue(
            ResolveAppResourceValueUseCase.Request(
              applicationInfo = packageInfo?.applicationInfo,
              resourceId = resource.id,
              resourceType = resource.type
            )
          )
        }
      ) {
        is AppResourceValue.Text -> root.updateItem(
          item.copy(preview = AppResourcePreview.Text(resourceValue.value))
        )

        is AppResourceValue.Xml -> showXml(resourceValue.value)

        is AppResourceValue.DrawablePreview -> root.updateItem(
          item.copy(preview = AppResourcePreview.DrawableValue(resourceValue.drawable))
        )

        is AppResourceValue.ColorPreview -> root.updateItem(
          item.copy(preview = AppResourcePreview.ColorValue(resourceValue.color))
        )

        null -> Unit
      }
    }
  }

  private fun showXml(xml: CharSequence) {
    val fragmentManager = parentFragmentManager
    XmlBSDFragment().apply {
      arguments = Bundle().apply {
        putCharSequence(EXTRA_TEXT, xml)
      }
    }.show(fragmentManager, XmlBSDFragment::class.java.name)
  }
}
