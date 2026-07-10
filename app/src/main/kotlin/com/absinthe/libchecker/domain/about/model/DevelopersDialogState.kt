package com.absinthe.libchecker.domain.about.model

sealed interface DevelopersDialogState {
  data object Loading : DevelopersDialogState

  data class Content(
    val items: List<DeveloperInfo>
  ) : DevelopersDialogState
}

sealed interface DevelopersDialogAction {
  data class OpenProfile(
    val url: String
  ) : DevelopersDialogAction
}

fun DeveloperInfo.toDevelopersDialogAction(): DevelopersDialogAction {
  return DevelopersDialogAction.OpenProfile(github)
}
