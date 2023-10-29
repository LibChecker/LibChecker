package android.content.pm;

import android.os.Build;

public class PackageParser {
  public static final int SDK_VERSION = Build.VERSION.SDK_INT;

  public static class PackageParserException extends Exception { }
}
