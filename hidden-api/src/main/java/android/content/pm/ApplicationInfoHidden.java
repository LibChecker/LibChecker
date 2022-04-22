package android.content.pm;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(ApplicationInfo.class)
public class ApplicationInfoHidden {

  public static final int PRIVATE_FLAG_HIDDEN = 1<<0;

  public String primaryCpuAbi;
  public int privateFlags;
}
