package hidden;

import androidx.annotation.NonNull;

import java.io.FileNotFoundException;

import dalvik.system.DexFile;
import dev.rikka.tools.refine.RefineAs;

@RefineAs(DexFile.class)
public class DexFileHidden {
  public static @NonNull DexFileHidden.OptimizationInfo getDexFileOptimizationInfo(
    @NonNull String fileName, @NonNull String instructionSet) throws FileNotFoundException {
    throw new RuntimeException("Stub");
  }

  public static final class OptimizationInfo {

    public @NonNull String getStatus() {
      throw new RuntimeException("Stub");
    }

    public @NonNull String getReason() {
      throw new RuntimeException("Stub");
    }
  }
}
