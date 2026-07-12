package com.absinthe.libchecker.domain.app.detail.presentation

import com.absinthe.libchecker.domain.app.detail.abi.AppDetailAbi
import com.absinthe.libchecker.domain.app.detail.model.AppIconItem
import com.absinthe.libchecker.domain.app.model.VersionedFeature
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

  private val _isLoading = MutableStateFlow(false)
  val isLoading = _isLoading.asStateFlow()

  var appIcons: List<AppIconItem> = emptyList()
    private set

  suspend fun emitFeature(feature: VersionedFeature) {
    _featuresFlow.emit(feature)
  }

  suspend fun setAppIcons(appIcons: List<AppIconItem>) {
    this.appIcons = appIcons
  }

  suspend fun emitAbiBundle(abi: AppDetailAbi) {
    _abiBundleStateFlow.emit(abi)
  }

  suspend fun set64Bit(is64Bit: Boolean) {
    _is64Bit.emit(is64Bit)
  }

  suspend fun setLoading(isLoading: Boolean) {
    _isLoading.emit(isLoading)
  }

  fun reset() {
    appIcons = emptyList()
    _abiBundleStateFlow.value = null
    _is64Bit.value = null
    _isLoading.value = false
  }
}
