package com.absinthe.libchecker.domain.app.detail.ui.controller

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.domain.app.detail.header.DetailHeaderExtraInfoState
import com.absinthe.libchecker.domain.app.detail.header.DetailHeaderRenderState
import com.absinthe.libchecker.domain.app.detail.model.DetailExtraBean
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.AppBarStateChangeListener
import com.absinthe.libchecker.domain.app.detail.ui.DetailHeaderBinder
import com.absinthe.libchecker.utils.Toasty
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ohos.bundle.BundleInfo
import timber.log.Timber

class DetailHeaderController(
  private val activity: FragmentActivity,
  private val supportActionBar: () -> ActionBar?,
  private val collapsingToolbar: CollapsingToolbarLayout,
  private val headerLayout: AppBarLayout,
  private val headerBinder: DetailHeaderBinder,
  private val viewModel: DetailViewModel,
  private val coroutineScope: CoroutineScope,
  private val isDisplayOptionEnabled: (Int) -> Boolean,
  private val harmonyBundleInfo: (String) -> BundleInfo?
) {
  private var state: DetailHeaderRenderState? = null
  private var applicationInfo: ApplicationInfo? = null
  private var extraInfoJob: Job? = null
  private val offsetChangedListener = object : AppBarStateChangeListener() {
    override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {
      collapsingToolbar.isTitleEnabled = state == State.COLLAPSED
    }
  }

  init {
    headerLayout.addOnOffsetChangedListener(offsetChangedListener)
  }

  fun bind(
    packageInfo: PackageInfo,
    extraBean: DetailExtraBean?,
    isHarmonyMode: Boolean,
    apkAnalyticsMode: Boolean,
    renderId: Int
  ): DetailHeaderRenderState? {
    extraInfoJob?.cancel()
    return try {
      val renderState = DetailHeaderRenderState(
        renderId = renderId,
        title = viewModel.buildAppDetailHeaderTitleData(packageInfo, apkAnalyticsMode)
      )
      state = renderState
      applicationInfo = packageInfo.applicationInfo
      supportActionBar()?.title = null
      collapsingToolbar.also {
        it.setOnApplyWindowInsetsListener(null)
        it.title = renderState.title.title
      }
      headerBinder.bind(renderState, applicationInfo)
      bindExtraInfo(packageInfo, extraBean, isHarmonyMode, renderState)
      renderState
    } catch (e: Exception) {
      handleBindingError(e)
      null
    }
  }

  fun release() {
    extraInfoJob?.cancel()
    extraInfoJob = null
    state = null
    applicationInfo = null
    headerLayout.removeOnOffsetChangedListener(offsetChangedListener)
  }

  private fun bindExtraInfo(
    packageInfo: PackageInfo,
    extraBean: DetailExtraBean?,
    isHarmonyMode: Boolean,
    initialState: DetailHeaderRenderState
  ) {
    extraInfoJob = coroutineScope.launch {
      try {
        val extraInfo = loadExtraInfo(
          packageInfo = packageInfo,
          extraBean = extraBean,
          isHarmonyMode = isHarmonyMode,
          packageName = initialState.packageName
        )
        val currentState = state ?: return@launch
        val updatedState = currentState.withExtraInfo(initialState.renderId, extraInfo)
        if (updatedState === currentState) {
          return@launch
        }
        state = updatedState
        headerBinder.bind(updatedState, applicationInfo)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleBindingError(e)
      }
    }
  }

  private suspend fun loadExtraInfo(
    packageInfo: PackageInfo,
    extraBean: DetailExtraBean?,
    isHarmonyMode: Boolean,
    packageName: String
  ): DetailHeaderExtraInfoState {
    if (!isHarmonyMode) {
      val showAndroidVersion = isDisplayOptionEnabled(AdvancedOptions.SHOW_ANDROID_VERSION)
      return DetailHeaderExtraInfoState.Android(
        viewModel.buildAppDetailHeaderExtraInfo(
          packageInfo = packageInfo,
          showAndroidVersion = showAndroidVersion
        )
      )
    }

    if (extraBean?.variant != Constants.VARIANT_HAP) {
      return DetailHeaderExtraInfoState.Empty
    }
    val bundleInfo = withContext(Dispatchers.IO) {
      harmonyBundleInfo(packageName)
    } ?: return DetailHeaderExtraInfoState.Empty
    return DetailHeaderExtraInfoState.Harmony(
      targetVersion = bundleInfo.targetVersion.toString(),
      minSdkVersion = bundleInfo.minSdkVersion.toString(),
      jointUserId = bundleInfo.jointUserId?.takeIf(String::isNotEmpty)
    )
  }

  private fun handleBindingError(error: Exception) {
    Timber.e(error)
    Toasty.showLong(activity, error.toString())
    activity.finish()
  }
}
