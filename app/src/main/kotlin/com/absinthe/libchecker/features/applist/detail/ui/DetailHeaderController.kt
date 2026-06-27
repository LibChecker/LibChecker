package com.absinthe.libchecker.features.applist.detail.ui

import android.content.pm.PackageInfo
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.domain.app.detail.model.DetailExtraBean
import com.absinthe.libchecker.domain.app.detail.ui.AppBarStateChangeListener
import com.absinthe.libchecker.domain.app.detail.ui.DetailHeaderExtraInfoBinder
import com.absinthe.libchecker.domain.app.detail.ui.DetailHeaderTitleBinder
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.utils.Toasty
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ohos.bundle.BundleInfo
import timber.log.Timber

class DetailHeaderController(
  private val activity: FragmentActivity,
  private val supportActionBar: () -> ActionBar?,
  private val collapsingToolbar: CollapsingToolbarLayout,
  private val headerLayout: AppBarLayout,
  private val headerTitleBinder: DetailHeaderTitleBinder,
  private val headerExtraInfoBinder: DetailHeaderExtraInfoBinder,
  private val viewModel: DetailViewModel,
  private val coroutineScope: CoroutineScope,
  private val isDisplayOptionEnabled: (Int) -> Boolean,
  private val harmonyBundleInfo: (String) -> BundleInfo?
) {

  fun bind(
    packageInfo: PackageInfo,
    extraBean: DetailExtraBean?,
    isHarmonyMode: Boolean,
    apkAnalyticsMode: Boolean
  ): DetailHeaderBindingResult? {
    val headerTitleData = viewModel.buildAppDetailHeaderTitleData(packageInfo, apkAnalyticsMode)
    val packageName = headerTitleData.packageName

    return try {
      supportActionBar()?.title = null
      collapsingToolbar.also {
        it.setOnApplyWindowInsetsListener(null)
        it.title = headerTitleData.title
      }
      headerLayout.addOnOffsetChangedListener(object : AppBarStateChangeListener() {
        override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {
          collapsingToolbar.isTitleEnabled = state == State.COLLAPSED
        }
      })
      headerTitleBinder.bind(headerTitleData, packageInfo.applicationInfo)
      bindExtraInfo(packageInfo, extraBean, isHarmonyMode, packageName)
      DetailHeaderBindingResult(packageName)
    } catch (e: Exception) {
      Timber.e(e)
      Toasty.showLong(activity, e.toString())
      activity.finish()
      null
    }
  }

  private fun bindExtraInfo(
    packageInfo: PackageInfo,
    extraBean: DetailExtraBean?,
    isHarmonyMode: Boolean,
    packageName: String
  ) {
    coroutineScope.launch(Dispatchers.IO) {
      val showAndroidVersion = isDisplayOptionEnabled(AdvancedOptions.SHOW_ANDROID_VERSION)
      val versionInfo = if (!isHarmonyMode) {
        val headerExtraInfo = viewModel.buildAppDetailHeaderExtraInfo(
          packageInfo = packageInfo,
          showAndroidVersion = showAndroidVersion
        )
        headerExtraInfoBinder.format(headerExtraInfo)
      } else {
        if (extraBean?.variant == Constants.VARIANT_HAP) {
          headerExtraInfoBinder.formatHarmony(harmonyBundleInfo(packageName))
        } else {
          ""
        }
      }

      withContext(Dispatchers.Main) {
        headerExtraInfoBinder.bind(versionInfo)
      }
    }
  }
}

data class DetailHeaderBindingResult(
  val packageName: String
)
