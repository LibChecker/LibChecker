package com.absinthe.libchecker.domain.home.model

data class HomeToolbarTitleState(
  val title: CharSequence,
  val isLoading: Boolean = false
) {
  fun withLoading(isLoading: Boolean): HomeToolbarTitleState {
    return if (this.isLoading == isLoading) this else copy(isLoading = isLoading)
  }
}
