package com.absinthe.libchecker.utils

import java.text.Bidi

object IcuUtils {
  private fun isRtlChar(ch: Char): Boolean {
    val block = Character.UnicodeBlock.of(ch)
    return block == Character.UnicodeBlock.ARABIC ||
      block == Character.UnicodeBlock.HEBREW ||
      block == Character.UnicodeBlock.SYRIAC ||
      block == Character.UnicodeBlock.ARABIC_SUPPLEMENT ||
      block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A ||
      block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B ||
      block == Character.UnicodeBlock.NKO
  }

  fun isRtl(text: String): Boolean {
    val bidi = Bidi(text, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT)

    if (bidi.isRightToLeft) {
      return true
    }

    text.forEach { char ->
      if (isRtlChar(char)) {
        return true
      }
    }

    return false
  }
}
