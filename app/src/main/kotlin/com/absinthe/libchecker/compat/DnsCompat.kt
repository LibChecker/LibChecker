package com.absinthe.libchecker.compat

import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import okhttp3.Dns
import okhttp3.android.AndroidDns
import timber.log.Timber

/**
 * Resolver selection for app HTTP clients.
 *
 * OkHttp can only apply Encrypted Client Hello (ECH) when the resolver reports the `HTTPS` (type
 * 65) service metadata that carries the server's ECH config list. [Dns.SYSTEM] never issues that
 * query, so [AndroidDns] has to be installed explicitly. [AndroidDns] then gates itself at runtime:
 * it only asks for service metadata on API 37+ and only when the app's network security config
 * opts the hostname into domain encryption.
 */
object DnsCompat {

  /**
   * The resolver to install on [okhttp3.OkHttpClient.Builder.dns], or null to keep OkHttp's
   * default. [AndroidDns] needs `android.net.DnsResolver`, which is API 29+.
   */
  val echCapableDns: Dns? by unsafeLazy {
    if (!OsUtils.atLeastQ()) {
      null
    } else {
      runCatching { AndroidDns() }
        .onFailure { Timber.w(it, "Failed to create AndroidDns, falling back to the default resolver") }
        .getOrNull()
    }
  }
}
