package com.absinthe.libchecker.compat

import android.os.Build
import com.absinthe.libchecker.api.ApiManager
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeNoException
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Device smoke check for Encrypted Client Hello.
 *
 * Cloudflare reports the observed handshake in /cdn-cgi/trace: sni=encrypted when the request used
 * ECH, sni=plaintext otherwise. cloudflare-ech.com publishes an ECH config list in its HTTPS DNS
 * record, so it can tell whether [DnsCompat.echCapableDns] plus the domainEncryption network
 * security config actually take effect. The test skips itself when the endpoint is unreachable so a
 * restricted network does not look like an ECH regression.
 */
class EchHandshakeInstrumentedTest {

  @Test
  fun echCapableDnsNegotiatesEch() {
    assumeTrue(Build.VERSION.SDK_INT >= 37)
    val dns = DnsCompat.echCapableDns
    assertNotNull("DnsCompat.echCapableDns must be available on API 37+", dns)

    assertEquals("sni=encrypted", sniOf(OkHttpClient.Builder().dns(dns!!).build()))
  }

  @Test
  fun appApiClientNegotiatesEch() {
    assumeTrue(Build.VERSION.SDK_INT >= 37)
    assertEquals("sni=encrypted", sniOf(ApiManager.okHttpClient))
  }

  @Test
  fun defaultResolverStaysPlaintext() {
    assumeTrue(Build.VERSION.SDK_INT >= 37)
    assertEquals("sni=plaintext", sniOf(OkHttpClient()))
  }

  private fun sniOf(client: OkHttpClient): String? {
    val request = Request.Builder().url(TRACE_URL).build()
    val body = try {
      client.newCall(request).execute().use { it.body.string() }
    } catch (e: IOException) {
      assumeNoException("$TRACE_URL is unreachable, skipping the ECH check", e)
      return null
    }
    return body.lineSequence().firstOrNull { it.startsWith("sni=") }
  }

  private companion object {
    const val TRACE_URL = "https://cloudflare-ech.com/cdn-cgi/trace"
  }
}
