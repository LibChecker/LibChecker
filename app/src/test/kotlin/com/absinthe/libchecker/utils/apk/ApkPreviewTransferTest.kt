package com.absinthe.libchecker.utils.apk

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ApkPreviewTransferTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `range and full responses produce identical manifests and remove cache files`() = runBlocking {
    val manifest = "manifest fixture".toByteArray()
    val archive = archive(manifest)
    for (mode in listOf("ranges", "ignored", "chunked")) {
      val gets = AtomicInteger()
      withServer({ exchange ->
        exchange.responseHeaders.add("Accept-Ranges", "bytes")
        if (exchange.requestMethod == "HEAD") {
          if (mode != "chunked") exchange.responseHeaders.add("Content-Length", archive.size.toString())
          exchange.sendResponseHeaders(200, -1)
        } else {
          gets.incrementAndGet()
          val range = exchange.requestHeaders.getFirst("Range")
          if (mode == "ranges" && range != null) {
            val bounds = range.removePrefix("bytes=").split('-')
            val start = bounds[0].toInt()
            val end = bounds[1].toIntOrNull() ?: archive.lastIndex
            exchange.responseHeaders.add("Content-Range", "bytes $start-$end/${archive.size}")
            exchange.sendResponseHeaders(206, (end - start + 1).toLong())
            exchange.responseBody.write(archive, start, end - start + 1)
          } else {
            exchange.sendResponseHeaders(200, if (mode == "chunked") 0 else archive.size.toLong())
            exchange.responseBody.write(archive)
          }
        }
      }) { preview ->
        assertArrayEquals(mode, manifest, preview.readManifestBytes())
        assertEquals(mode, if (mode == "ranges") 4 else 1, gets.get())
        assertTrue(temporaryFolder.root.listFiles()!!.isEmpty())
      }
    }
  }

  @Test
  fun `full APK larger than range budget streams through cache`() = runBlocking {
    val fixture = File.createTempFile("large-preview-fixture-", ".apk")
    val manifest = "manifest fixture".toByteArray()
    try {
      val block = ByteArray(8192)
      val blocks = 5 * 1024
      val crc = CRC32().apply { repeat(blocks) { update(block) } }.value
      ZipOutputStream(fixture.outputStream()).use { zip ->
        zip.putNextEntry(
          ZipEntry("assets/large.dat").apply {
            method = ZipEntry.STORED
            size = block.size.toLong() * blocks
            compressedSize = size
            this.crc = crc
          }
        )
        repeat(blocks) { zip.write(block) }
        zip.closeEntry()
        zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
        zip.write(manifest)
        zip.closeEntry()
      }
      withServer({ exchange ->
        if (exchange.requestMethod == "HEAD") {
          exchange.sendResponseHeaders(200, -1)
        } else {
          exchange.sendResponseHeaders(200, 0)
          fixture.inputStream().use { it.copyTo(exchange.responseBody) }
        }
      }) { preview ->
        assertArrayEquals(manifest, preview.readManifestBytes())
        assertTrue(temporaryFolder.root.listFiles()!!.isEmpty())
      }
    } finally {
      fixture.delete()
    }
  }

  @Test
  fun `inflated manifest budget is enforced even when directory lies about size`() = runBlocking {
    val archive = archive(ByteArray(17 * 1024 * 1024))
    val central = archive.indexOfSequence(byteArrayOf(0x50, 0x4b, 0x01, 0x02))
    ByteBuffer.wrap(archive).order(ByteOrder.LITTLE_ENDIAN).putInt(central + 24, 1)
    withArchive(archive) { preview ->
      val failure = runCatching { preview.readManifestBytes() }.exceptionOrNull()
      assertTrue(failure.toString(), failure is IllegalArgumentException)
      assertTrue(failure!!.message.orEmpty().contains("budget"))
      assertTrue(temporaryFolder.root.listFiles()!!.isEmpty())
    }
  }

  @Test
  fun `oversized directory is rejected before allocation and cache is removed`() = runBlocking {
    val archive = archive(byteArrayOf(1))
    ByteBuffer.wrap(archive).order(ByteOrder.LITTLE_ENDIAN).putInt(archive.size - 22 + 12, 64 * 1024 * 1024)
    withArchive(archive) { preview ->
      val failure = runCatching { preview.readManifestBytes() }.exceptionOrNull()
      assertTrue(failure.toString(), failure is IllegalArgumentException)
      assertTrue(failure!!.message.orEmpty().contains("budget"))
      assertTrue(temporaryFolder.root.listFiles()!!.isEmpty())
    }
  }

  @Test
  fun `cancellation closes a blocked network read and removes partial archive`() = runBlocking {
    val started = CountDownLatch(1)
    val disconnected = CountDownLatch(1)
    val sendMore = CountDownLatch(1)
    withServer({ exchange ->
      if (exchange.requestMethod == "HEAD") {
        exchange.sendResponseHeaders(200, -1)
      } else {
        exchange.sendResponseHeaders(200, 0)
        exchange.responseBody.write(ByteArray(8192))
        exchange.responseBody.flush()
        started.countDown()
        sendMore.await(5, TimeUnit.SECONDS)
        try {
          repeat(1024) {
            exchange.responseBody.write(ByteArray(8192))
            exchange.responseBody.flush()
          }
        } catch (_: IOException) {
          disconnected.countDown()
        }
      }
    }) { preview ->
      val job = launch(Dispatchers.IO) { preview.readManifestBytes() }
      assertTrue(withContext(Dispatchers.IO) { started.await(5, TimeUnit.SECONDS) })
      withTimeout(5_000) { job.cancelAndJoin() }
      assertTrue(job.isCancelled)
      assertTrue(temporaryFolder.root.listFiles()!!.isEmpty())
      sendMore.countDown()
      assertTrue(withContext(Dispatchers.IO) { disconnected.await(5, TimeUnit.SECONDS) })
    }
  }

  private fun archive(manifest: ByteArray): ByteArray {
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
      zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
      zip.write(manifest)
      zip.closeEntry()
    }
    return output.toByteArray()
  }

  private suspend fun withArchive(archive: ByteArray, action: suspend (ApkPreview) -> Unit) {
    withServer({ exchange ->
      if (exchange.requestMethod == "HEAD") {
        exchange.sendResponseHeaders(200, -1)
      } else {
        exchange.sendResponseHeaders(200, archive.size.toLong())
        exchange.responseBody.write(archive)
      }
    }, action)
  }

  private suspend fun withServer(handler: (HttpExchange) -> Unit, action: suspend (ApkPreview) -> Unit) {
    val executor = Executors.newCachedThreadPool()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.executor = executor
    server.createContext("/app.apk") { exchange -> exchange.use { handler(it) } }
    server.start()
    val client = OkHttpClient()
    try {
      action(ApkPreview("http://127.0.0.1:${server.address.port}/app.apk", client, temporaryFolder.root))
    } finally {
      server.stop(0)
      executor.shutdownNow()
      client.connectionPool.evictAll()
      client.dispatcher.executorService.shutdownNow()
    }
  }
}
