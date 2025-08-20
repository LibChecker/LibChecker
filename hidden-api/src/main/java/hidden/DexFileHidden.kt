package hidden

import android.os.Build
import androidx.annotation.RequiresApi
import dalvik.system.DexFile
import dev.rikka.tools.refine.RefineAs
import java.io.FileNotFoundException

@RefineAs(DexFile::class)
class DexFileHidden {

  companion object {
    @RequiresApi(Build.VERSION_CODES.P)
    @JvmStatic
    @Throws(FileNotFoundException::class)
    fun getDexFileOptimizationInfo(
      fileName: String,
      instructionSet: String
    ): OptimizationInfo {
      throw RuntimeException("Stub")
    }
  }

  @RequiresApi(Build.VERSION_CODES.P)
  class OptimizationInfo {

    fun getStatus(): String {
      throw RuntimeException("Stub")
    }

    fun getReason(): String {
      throw RuntimeException("Stub")
    }
  }
}