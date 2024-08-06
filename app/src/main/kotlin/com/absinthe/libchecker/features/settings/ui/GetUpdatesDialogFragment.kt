package com.absinthe.libchecker.features.settings.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.features.settings.bean.GetUpdatesItem
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.extensions.getBoolean
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class GetUpdatesDialogFragment : BaseBottomSheetViewDialogFragment<GetUpdatesDialogView>() {

  override fun initRootView(): GetUpdatesDialogView = GetUpdatesDialogView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.addPaddingTop(16.dp)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    var items = listOf(
      GetUpdatesItem(
        "GitHub",
        R.drawable.ic_github
      ) {
        context?.startActivity(
          Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(URLManager.GITHUB_REPO_PAGE)
          }
        )
      },
      GetUpdatesItem(
        "Google Play",
        com.absinthe.lc.rulesbundle.R.drawable.ic_lib_google
      ) {
        context?.startActivity(
          Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(URLManager.PLAY_STORE_DETAIL_PAGE)
          }
        )
      },
      GetUpdatesItem(
        "Telegram",
        com.absinthe.lc.rulesbundle.R.drawable.ic_lib_telegram
      ) {
        context?.startActivity(
          Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(URLManager.TELEGRAM_RELEASES)
          }
        )
      },
      GetUpdatesItem(
        "F-Droid",
        R.drawable.ic_fdroid
      ) {
        context?.startActivity(
          Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(URLManager.FDROID_PAGE)
          }
        )
      }
    )
    if (getBoolean(R.bool.is_foss)) {
      items = items + GetUpdatesItem(
        getString(R.string.settings_get_updates_in_app),
        R.drawable.ic_logo
      ) {
        InAppUpdateDialogFragment().show(
          childFragmentManager,
          InAppUpdateDialogFragment::class.java.simpleName
        )
      }
    }
    root.setItems(items)
  }
}
