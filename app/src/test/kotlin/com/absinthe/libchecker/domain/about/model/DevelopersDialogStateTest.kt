package com.absinthe.libchecker.domain.about.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DevelopersDialogStateTest {

  @Test
  fun `content state carries developer rows and profile action`() {
    val developer = DeveloperInfo(
      name = "Absinthe",
      desc = "Maintainer",
      github = "https://github.com/zhaobozhen",
      avatarUrl = "https://avatars.githubusercontent.com/u/25247117"
    )

    val state = DevelopersDialogState.Content(listOf(developer))

    assertEquals(listOf(developer), state.items)
    assertEquals(
      DevelopersDialogAction.OpenProfile(developer.github),
      developer.toDevelopersDialogAction()
    )
  }

  @Test
  fun `loading state is a stable render state`() {
    assertSame(DevelopersDialogState.Loading, DevelopersDialogState.Loading)
  }
}
