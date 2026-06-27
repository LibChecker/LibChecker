package com.absinthe.libchecker.features.applist.detail.ui

import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.ui.adapter.AppInfoAdapter
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.applist.detail.ui.view.AppInfoBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * <pre>
 * author : Absinthe
 * time : 2020/10/25
 * </pre>
 */

class AppInfoBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<AppInfoBottomSheetView>() {

  private val viewModel: DetailViewModel by activityViewModel()
  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }
  private val aiAdapter = AppInfoAdapter()

  private val exportApkLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      shareController.onExportResult(result.resultCode, result.data?.data)
    }
  private val shareController: AppInfoPackageShareController by lazy {
    AppInfoPackageShareController(
      fragment = this,
      viewModel = viewModel,
      coroutineScope = lifecycleScope,
      exportDocument = { intent -> exportApkLauncher.launch(intent) },
      dismiss = ::dismiss
    )
  }
  private val primaryActionController: AppInfoPrimaryActionController by lazy {
    AppInfoPrimaryActionController(
      fragment = this,
      viewModel = viewModel,
      coroutineScope = lifecycleScope,
      dismiss = ::dismiss
    )
  }

  override fun initRootView(): AppInfoBottomSheetView = AppInfoBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    root.apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setPadding(24.dp, 16.dp, 24.dp, 0)
    }
    primaryActionController.bind(root, packageName)
    root.share.apply {
      setOnClickListener { view ->
        shareController.sharePackage(view, packageName)
      }
      setOnLongClickListener { view ->
        shareController.exportPackage(view, packageName)
      }
    }
    root.list.apply {
      adapter = aiAdapter
      layoutManager = GridLayoutManager(context, 4)
      setHasFixedSize(true)
    }

    aiAdapter.also { adapter ->
      adapter.setOnItemClickListener { _, _, position ->
        adapter.data[position].let {
          runCatching {
            startActivity(it.intent)
          }.onFailure {
            context?.let { ctx ->
              Toasty.showShort(ctx, R.string.toast_cant_open_app)
            }
          }
        }
        dismiss()
      }
    }
    packageName?.let { pkg ->
      lifecycleScope.launch {
        aiAdapter.setList(viewModel.getAppInfoActions(pkg))
      }
    }
  }
}
