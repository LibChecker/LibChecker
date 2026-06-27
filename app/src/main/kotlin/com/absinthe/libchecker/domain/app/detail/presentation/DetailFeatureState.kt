package com.absinthe.libchecker.domain.app.detail.presentation

import com.absinthe.libchecker.domain.app.AppIconItem
import com.absinthe.libchecker.domain.app.VersionedFeature
import com.absinthe.libchecker.domain.app.detail.AppDetailAbi
import com.absinthe.libchecker.domain.app.detail.AppDetailFeatures
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class DetailFeatureState {
  private val _featuresFlow = MutableSharedFlow<VersionedFeature>()
  val featuresFlow = _featuresFlow.asSharedFlow()

  private val _abiBundleStateFlow = MutableStateFlow<AppDetailAbi?>(null)
  val abiBundleStateFlow = _abiBundleStateFlow.asStateFlow()

  private val _is64Bit = MutableStateFlow<Boolean?>(null)
  val is64Bit = _is64Bit.asStateFlow()

  var appIcons: List<AppIconItem> = emptyList()
    private set

  suspend fun emitFeature(feature: VersionedFeature) {
    _featuresFlow.emit(feature)
  }

  suspend fun emitFeatures(detailFeatures: AppDetailFeatures) {
    appIcons = detailFeatures.appIcons
    detailFeatures.features.forEach {
      _featuresFlow.emit(it)
    }
  }

  suspend fun emitAbiBundle(abi: AppDetailAbi) {
    _abiBundleStateFlow.emit(abi)
  }

  suspend fun set64Bit(is64Bit: Boolean) {
    _is64Bit.emit(is64Bit)
  }
}
