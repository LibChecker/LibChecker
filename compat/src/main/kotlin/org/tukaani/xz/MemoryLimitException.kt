package org.tukaani.xz

import java.io.IOException

class MemoryLimitException @JvmOverloads constructor(
  private val memoryNeeded: Int = -1,
  private val memoryLimit: Int = -1
) : IOException("$memoryNeeded KiB of memory would be needed; limit was $memoryLimit KiB") {

  fun getMemoryNeeded(): Int {
    return memoryNeeded
  }

  fun getMemoryLimit(): Int {
    return memoryLimit
  }
}
