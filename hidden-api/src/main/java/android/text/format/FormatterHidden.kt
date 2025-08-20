package android.text.format

import android.content.Context
import dev.rikka.tools.refine.RefineAs

@RefineAs(Formatter::class)
class FormatterHidden {
  companion object {
    @JvmField
    var FLAG_SI_UNITS: Int = 0

    @JvmField
    var FLAG_IEC_UNITS: Int = 0

    @JvmStatic
    fun formatFileSize(context: Context?, sizeBytes: Long, flags: Int): String {
      throw RuntimeException("Stub")
    }
  }
}