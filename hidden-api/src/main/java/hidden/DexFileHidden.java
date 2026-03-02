package hidden;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.FileNotFoundException;

import dalvik.system.DexFile;
import dev.rikka.tools.refine.RefineAs;

@RefineAs(DexFile.class)
public class DexFileHidden {

  @RequiresApi(Build.VERSION_CODES.P)
  public static @NonNull DexFileHidden.OptimizationInfo getDexFileOptimizationInfo(
    @NonNull String fileName, @NonNull String instructionSet) throws FileNotFoundException {
    throw new RuntimeException("Stub");
  }

  @RequiresApi(Build.VERSION_CODES.P)
  public static final class OptimizationInfo {

    public @NonNull String getStatus() {
      throw new RuntimeException("Stub");
    }

    public @NonNull String getReason() {
      throw new RuntimeException("Stub");
    }
  }
}
