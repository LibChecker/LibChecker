package android.text.format;

import android.content.Context;


import androidx.annotation.Nullable;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(Formatter.class)
public class FormatterHidden {
  public static int FLAG_SI_UNITS;

  public static int FLAG_IEC_UNITS;

  public static String formatFileSize(@Nullable Context context, long sizeBytes, int flags) {
    throw new RuntimeException("Stub");
  }
}
