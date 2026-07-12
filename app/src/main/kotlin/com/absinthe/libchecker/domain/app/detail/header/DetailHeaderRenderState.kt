package com.absinthe.libchecker.domain.app.detail.header

data class DetailHeaderRenderState(
  val renderId: Int,
  val title: AppDetailHeaderTitleData,
  val extraInfo: DetailHeaderExtraInfoState = DetailHeaderExtraInfoState.Loading
) {
  val packageName: String
    get() = title.packageName

  fun withExtraInfo(
    renderId: Int,
    extraInfo: DetailHeaderExtraInfoState
  ): DetailHeaderRenderState {
    if (this.renderId != renderId) {
      return this
    }
    return copy(extraInfo = extraInfo)
  }
}

sealed interface DetailHeaderExtraInfoState {
  data object Loading : DetailHeaderExtraInfoState

  data object Empty : DetailHeaderExtraInfoState

  data class Android(
    val value: AppDetailHeaderExtraInfo
  ) : DetailHeaderExtraInfoState

  data class Harmony(
    val targetVersion: String,
    val minSdkVersion: String,
    val jointUserId: String?
  ) : DetailHeaderExtraInfoState
}
