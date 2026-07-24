package com.absinthe.libchecker.domain.app.buildmetadata

import com.absinthe.libchecker.compat.ZipFileCompat
import java.io.InputStreamReader

internal val COMPOSE_VERSION_ENTRIES = arrayOf(
  "META-INF/androidx.compose.runtime_runtime.version",
  "META-INF/androidx.compose.ui_ui.version",
  "META-INF/androidx.compose.ui_ui-tooling-preview.version",
  "META-INF/androidx.compose.foundation_foundation.version",
  "META-INF/androidx.compose.animation_animation.version"
)

internal val DATA_BINDING_VERSION_ENTRIES = arrayOf(
  "META-INF/androidx.databinding_viewbinding.version",
  "META-INF/androidx.databinding_databindingKtx.version",
  "META-INF/androidx.databinding_library.version"
)

internal fun ZipFileCompat.readFirstPresentLine(entries: Array<String>): String? {
  entries.forEach { name ->
    getEntry(name)?.let { entry ->
      runCatching {
        InputStreamReader(getInputStream(entry), Charsets.UTF_8).buffered().use { it.readLine() }
          ?.takeIf { line -> line.isNotBlank() }
      }.getOrNull()?.let { return it }
    }
  }
  return null
}
