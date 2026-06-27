package com.absinthe.libchecker.domain.about.ui

import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.domain.about.model.DeveloperInfo
import com.absinthe.libchecker.domain.about.ui.view.DevelopersDialogView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class DevelopersDialogFragment : BaseBottomSheetViewDialogFragment<DevelopersDialogView>() {

  override fun initRootView(): DevelopersDialogView = DevelopersDialogView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.addPaddingTop(16.dp)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val items = listOf(
      DeveloperInfo(
        "Absinthe",
        "Developer & Designer",
        "https://github.com/zhaobozhen",
        "https://avatars.githubusercontent.com/u/25247117".toUri()
      ),
      DeveloperInfo(
        "Goooler",
        "Code Tidy & Optimize",
        "https://github.com/Goooler",
        "https://avatars.githubusercontent.com/u/10363352".toUri()
      ),
      DeveloperInfo(
        "qhy040404",
        "Developer",
        "https://github.com/qhy040404",
        "https://avatars.githubusercontent.com/u/45379733".toUri()
      ),
      DeveloperInfo(
        "Source Code",
        URLManager.GITHUB_REPO_PAGE,
        URLManager.GITHUB_REPO_PAGE,
        "https://avatars.githubusercontent.com/u/116417672".toUri()
      )
    )

    root.setItems(items)
  }
}
