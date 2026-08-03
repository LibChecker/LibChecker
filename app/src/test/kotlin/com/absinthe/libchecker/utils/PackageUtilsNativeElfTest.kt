package com.absinthe.libchecker.utils

import com.absinthe.libchecker.annotation.ET_NOT_ELF
import com.absinthe.libchecker.annotation.ET_NOT_SET
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class PackageUtilsNativeElfTest {

  @Test
  fun skipsElfParsingWhenDisabled() {
    val elfInfo = PackageUtils.parseNativeDirElfInfo(File("does-not-exist.so"), parseElf = false)

    assertEquals(ET_NOT_SET, elfInfo.elfType)
    assertEquals(-1, elfInfo.pageSize)
  }

  @Test
  fun reportsInvalidElfWhenParsingIsEnabled() {
    val elfInfo = PackageUtils.parseNativeDirElfInfo(File("does-not-exist.so"), parseElf = true)

    assertEquals(ET_NOT_ELF, elfInfo.elfType)
    assertEquals(-1, elfInfo.pageSize)
  }
}
