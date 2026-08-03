package com.absinthe.libchecker.database

import org.junit.Assert.assertEquals
import org.junit.Test

class RulesRepositoryNativeValidationTest {

  @Test
  fun acceptsNativeLibsThatDoNotRequireValidationWithoutResolvingPackage() {
    val result = RulesRepository.checkNativeLibValidations(
      nativeLibs = listOf("libexample.so", "libanother.so"),
      sourceProvider = { error("Source should not be resolved") }
    )

    assertEquals(
      mapOf("libexample.so" to true, "libanother.so" to true),
      result
    )
  }

  @Test
  fun preservesValidationFailureWhenPackageCannotBeResolved() {
    val result = RulesRepository.checkNativeLibValidations(
      nativeLibs = listOf("libexample.so", "libjiagu.so"),
      sourceProvider = { null }
    )

    assertEquals(
      mapOf("libexample.so" to true, "libjiagu.so" to false),
      result
    )
  }

  @Test
  fun acceptsCompanionValidatedNativeLibWithoutResolvingSource() {
    val result = RulesRepository.checkNativeLibValidations(
      nativeLibs = listOf("libapp.so", "libflutter.so"),
      otherNativeLibNames = listOf("libapp.so", "libflutter.so"),
      sourceProvider = { error("Source should not be resolved") }
    )

    assertEquals(
      mapOf("libapp.so" to true, "libflutter.so" to true),
      result
    )
  }

  @Test
  fun preservesCompanionValidationWhenAnotherNativeLibNeedsMissingSource() {
    val result = RulesRepository.checkNativeLibValidations(
      nativeLibs = listOf("libapp.so", "libflutter.so", "libjiagu.so"),
      otherNativeLibNames = listOf("libapp.so", "libflutter.so", "libjiagu.so"),
      sourceProvider = { null }
    )

    assertEquals(
      mapOf("libapp.so" to true, "libflutter.so" to true, "libjiagu.so" to false),
      result
    )
  }

  @Test
  fun rejectsUnityMarkerWithoutCompanionWithoutResolvingSource() {
    val result = RulesRepository.checkNativeLibValidations(
      nativeLibs = listOf("libmain.so"),
      sourceProvider = { error("Source should not be resolved") }
    )

    assertEquals(mapOf("libmain.so" to false), result)
  }
}
