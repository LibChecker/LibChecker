package org.tukaani.xz

import java.io.InputStream

@Suppress("UNUSED_PARAMETER")
class SingleXZInputStream @JvmOverloads constructor(
  inputStream: InputStream,
  memoryLimit: Int = -1
) : InputStream() {

  override fun read(): Int {
    throw UnsupportedOperationException("XZ decompression is not bundled")
  }
}
