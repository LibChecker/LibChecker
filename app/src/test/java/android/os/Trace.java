package android.os;

/** No-op platform tracing for JVM tests that exercise reference batch loading. */
public final class Trace {
  private Trace() {}

  public static void beginSection(String sectionName) {}

  public static void endSection() {}

  public static void beginAsyncSection(String sectionName, int cookie) {}

  public static void endAsyncSection(String sectionName, int cookie) {}
}
