package com.absinthe.libchecker.features.settings.export

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.content.pm.ServiceInfo
import android.content.pm.Signature
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.core.graphics.drawable.toBitmap
import com.absinthe.libchecker.R
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.domain.app.BuildAppExportNativeLibrariesUseCase
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.features.applist.detail.bean.KotlinToolingMetadata
import com.absinthe.libchecker.utils.IntentFilterUtils
import com.absinthe.libchecker.utils.apk.ApkSignatureSchemeDetector
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersion
import com.absinthe.libchecker.utils.extensions.getPackageSize
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.extensions.md5
import com.absinthe.libchecker.utils.extensions.sha1
import com.absinthe.libchecker.utils.extensions.sha256
import com.absinthe.libchecker.utils.extensions.toHexString
import com.absinthe.libchecker.utils.fromJson
import com.squareup.moshi.JsonWriter
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.math.BigInteger
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.DSAPublicKey
import java.security.interfaces.RSAPublicKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.TimeZone
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import timber.log.Timber

object LcAppsExporter {

  private const val APPS_JSON_ENTRY = "apps.json"
  private const val ICONS_DIR = "icons"
  private const val ICON_SIZE = 128
  private const val MIME_TYPE_PNG = "image/png"

  private val safeZipNameRegex = Regex("[^A-Za-z0-9._-]")
  private val isoDateFormat = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat {
      return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
      }
    }
  }

  suspend fun export(
    context: Context,
    installedAppRepository: InstalledAppRepository,
    buildAppExportNativeLibraries: BuildAppExportNativeLibrariesUseCase,
    outputStream: OutputStream,
    progress: suspend (Int) -> Unit
  ): ExportResult = withContext(Dispatchers.IO) {
    val exportStartedAt = SystemClock.elapsedRealtime()
    val appContext = context.applicationContext
    val packages = installedAppRepository.getApplicationList()
    val profiler = ExportProfiler(packages.size)
    val totalSteps = (packages.size * 2).coerceAtLeast(1)
    var completedSteps = 0

    Timber.i("LcApps export started appCount=${packages.size}")
    suspend fun updateProgress(value: Int? = null) {
      progress(value ?: (completedSteps * 100 / totalSteps))
    }

    updateProgress(0)
    ZipOutputStream(BufferedOutputStream(outputStream, ZIP_BUFFER_SIZE)).use { zip ->
      zip.setLevel(Deflater.BEST_SPEED)
      val iconEntries = profiler.measure(ExportStage.ICONS) {
        writeIcons(appContext, zip, packages, profiler) {
          completedSteps++
          updateProgress()
        }
      }

      zip.putNextEntry(ZipEntry(APPS_JSON_ENTRY))
      val sink = zip.sink().buffer()
      val jsonWriter = JsonWriter.of(sink)
      jsonWriter.beginArray()

      val analyzedAt = nowIsoString()
      packages.chunked(REPORT_BATCH_SIZE).forEach { batch ->
        currentCoroutineContext().ensureActive()
        val reports = coroutineScope {
          batch.map { packageInfo ->
            async {
              buildReport(
                context = appContext,
                installedAppRepository = installedAppRepository,
                buildAppExportNativeLibraries = buildAppExportNativeLibraries,
                basePackageInfo = packageInfo,
                iconEntry = iconEntries[packageInfo.packageName],
                analyzedAt = analyzedAt,
                profiler = profiler
              )
            }
          }.awaitAll()
        }
        reports.forEach { report ->
          profiler.measure(ExportStage.WRITE_JSON) {
            writeReport(jsonWriter, report)
          }
          completedSteps++
          updateProgress()
        }
      }

      jsonWriter.endArray()
      jsonWriter.flush()
      sink.flush()
      zip.closeEntry()
    }
    updateProgress(100)
    Timber.i(
      "LcApps export finished appCount=${packages.size} total=${SystemClock.elapsedRealtime() - exportStartedAt}ms ${profiler.summary()}"
    )

    ExportResult(packages.size)
  }

  private suspend fun writeIcons(
    context: Context,
    zip: ZipOutputStream,
    packages: List<PackageInfo>,
    profiler: ExportProfiler,
    onIconProcessed: suspend () -> Unit
  ): Map<String, IconEntry> {
    val packageManager = context.packageManager
    val usedPaths = mutableSetOf<String>()
    val entries = linkedMapOf<String, IconEntry>()

    packages.withIndex().chunked(ICON_BATCH_SIZE).forEach { batch ->
      currentCoroutineContext().ensureActive()
      val encodedIcons = coroutineScope {
        batch.map { (index, packageInfo) ->
          async {
            encodeIcon(packageManager, index, packageInfo, profiler)
          }
        }.awaitAll()
      }

      encodedIcons.forEach { icon ->
        if (icon != null) {
          val path = buildIconPath(icon.packageName, icon.index, usedPaths)
          profiler.measure(ExportStage.ICON_WRITE) {
            zip.putNextEntry(ZipEntry(path))
            zip.write(icon.bytes)
            zip.closeEntry()
          }
          entries[icon.packageName] = IconEntry(path, icon.bytes.size.toLong())
        }
        onIconProcessed()
      }
    }

    return entries
  }

  private suspend fun encodeIcon(
    packageManager: PackageManager,
    index: Int,
    packageInfo: PackageInfo,
    profiler: ExportProfiler
  ): EncodedIcon? {
    currentCoroutineContext().ensureActive()
    val appInfo = packageInfo.applicationInfo ?: return null
    val startedAt = SystemClock.elapsedRealtime()
    return runCatching {
      val bytes = profiler.measure(ExportStage.ICON_ENCODE) {
        packageManager.getApplicationIcon(appInfo)
          .toBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
          .toPngBytes()
      }
      val duration = SystemClock.elapsedRealtime() - startedAt
      if (duration >= SLOW_ICON_THRESHOLD_MS) {
        Timber.i("LcApps slow icon package=${packageInfo.packageName} duration=${duration}ms")
      }
      EncodedIcon(
        index = index,
        packageName = packageInfo.packageName,
        bytes = bytes
      )
    }.onFailure {
      Timber.w(it, "Failed to export icon: ${packageInfo.packageName}")
    }.getOrNull()
  }

  private suspend fun buildReport(
    context: Context,
    installedAppRepository: InstalledAppRepository,
    buildAppExportNativeLibraries: BuildAppExportNativeLibrariesUseCase,
    basePackageInfo: PackageInfo,
    iconEntry: IconEntry?,
    analyzedAt: String,
    profiler: ExportProfiler
  ): ExportReport {
    val startedAt = SystemClock.elapsedRealtime()
    val packageProfile = PackageExportProfile(basePackageInfo.packageName)
    val packageInfo = profiler.measure(ExportStage.LOAD_PACKAGE_INFO, packageProfile) {
      loadExportPackageInfo(installedAppRepository, basePackageInfo)
    }
    val packageManager = context.packageManager
    val unknown = context.getString(R.string.unknown)
    val appInfo = packageInfo.applicationInfo ?: basePackageInfo.applicationInfo
    val appName = appInfo?.let {
      runCatching { it.loadLabel(packageManager).toString() }.getOrNull()
    }?.takeIf { it.isNotBlank() } ?: packageInfo.packageName
    val versionName = packageInfo.versionName?.takeIf { it.isNotBlank() } ?: unknown
    val sourceFileName = "${packageInfo.packageName}.apk"

    val nativeLibraries = profiler.measure(ExportStage.NATIVE_LIBS, packageProfile) {
      buildNativeLibraries(buildAppExportNativeLibraries, packageInfo)
    }
    val parsedActions = profiler.measure(ExportStage.INTENT_ACTIONS, packageProfile) {
      parseIntentActions(packageInfo)
    }
    val components = profiler.measure(ExportStage.COMPONENTS, packageProfile) {
      buildComponents(packageInfo, parsedActions)
    }
    val buildFeatures = profiler.measure(ExportStage.BUILD_FEATURES, packageProfile) {
      buildBuildFeatures(packageInfo)
    }
    val signatures = profiler.measure(ExportStage.SIGNATURES, packageProfile) {
      buildSignatures(packageInfo)
    }
    val analysisProfile = profiler.measure(ExportStage.ANALYSIS_PROFILE, packageProfile) {
      buildAnalysisProfile(components)
    }

    val durationMs = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0)
    if (durationMs >= SLOW_PACKAGE_THRESHOLD_MS) {
      Timber.i("LcApps slow package=${packageInfo.packageName} duration=${durationMs}ms ${packageProfile.summary()}")
    }

    return ExportReport(
      locale = Locale.getDefault().toLanguageTag(),
      terminalSystem = buildTerminalSystem(),
      analysisProfile = analysisProfile,
      durationMs = durationMs,
      fileName = sourceFileName,
      fileSizeBytes = runCatching { packageInfo.getPackageSize(includeSplits = true) }.getOrDefault(0L),
      analyzedAt = analyzedAt,
      apkInfo = ApkInfoExport(
        appName = appName,
        packageName = packageInfo.packageName,
        versionName = versionName,
        versionCode = packageInfo.getVersionCode().toString(),
        minSdk = appInfo?.minSdkVersion?.toString() ?: unknown,
        targetSdk = appInfo?.targetSdkVersion?.toString() ?: unknown,
        compileSdk = packageInfo.getCompileSdkVersion().takeIf { it > 0 }?.toString() ?: unknown,
        icon = iconEntry,
        permissions = packageInfo.requestedPermissions?.filterNotNull().orEmpty(),
        nativeLibraries = nativeLibraries,
        components = components,
        metaData = MetaDataExport(
          application = buildMetaData(packageInfo.applicationInfo?.metaData),
          components = emptyList()
        ),
        buildFeatures = buildFeatures,
        signatures = signatures,
        sdkSummary = SdkSummaryExport(
          native = emptyList(),
          components = emptyList()
        )
      )
    )
  }

  private fun loadExportPackageInfo(
    installedAppRepository: InstalledAppRepository,
    basePackageInfo: PackageInfo
  ): PackageInfo {
    return installedAppRepository.getPackageInfo(basePackageInfo.packageName, exportPackageFlags())
      ?: basePackageInfo
  }

  @Suppress("DEPRECATION")
  private fun exportPackageFlags(): Int {
    return PackageManager.GET_ACTIVITIES or
      PackageManager.GET_SERVICES or
      PackageManager.GET_RECEIVERS or
      PackageManager.GET_PROVIDERS or
      PackageManager.GET_PERMISSIONS or
      PackageManager.GET_META_DATA or
      PackageManager.GET_SIGNATURES or
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
      } else {
        0
      }
  }

  private fun buildNativeLibraries(
    buildAppExportNativeLibraries: BuildAppExportNativeLibrariesUseCase,
    packageInfo: PackageInfo
  ): List<NativeLibraryExport> {
    return buildAppExportNativeLibraries(packageInfo).map {
      NativeLibraryExport(
        abi = it.abi,
        name = it.name,
        path = it.path,
        size = it.size,
        sdk = null
      )
    }
  }

  private fun parseIntentActions(packageInfo: PackageInfo): Map<String, List<String>> {
    val sourceDir = packageInfo.applicationInfo?.sourceDir ?: return emptyMap()
    return runCatching {
      IntentFilterUtils.parseComponentsFromApk(sourceDir)
        .associate { component ->
          component.className to component.intentFilters
            .flatMap { it.actions }
            .distinct()
            .sorted()
        }
    }.onFailure {
      Timber.w(it, "Failed to parse intent actions: ${packageInfo.packageName}")
    }.getOrDefault(emptyMap())
  }

  private suspend fun buildComponents(
    packageInfo: PackageInfo,
    parsedActions: Map<String, List<String>>
  ): ComponentsExport {
    return ComponentsExport(
      activities = packageInfo.activities.orEmpty().map {
        buildComponent(packageInfo, it, "activity", "activities", parsedActions[it.name].orEmpty())
      },
      services = packageInfo.services.orEmpty().map {
        buildComponent(packageInfo, it, "service", "services", parsedActions[it.name].orEmpty())
      },
      receivers = packageInfo.receivers.orEmpty().map {
        buildComponent(packageInfo, it, "receiver", "receivers", parsedActions[it.name].orEmpty())
      },
      providers = packageInfo.providers.orEmpty().map {
        buildComponent(packageInfo, it, "provider", "providers", emptyList())
      }
    )
  }

  private fun buildComponent(
    packageInfo: PackageInfo,
    info: ComponentInfo,
    typeName: String,
    sectionName: String,
    actions: List<String>
  ): ComponentExport {
    val componentName = info.name.orEmpty()

    return ComponentExport(
      type = typeName,
      section = sectionName,
      name = componentName,
      shortName = shortenComponentName(packageInfo.packageName, componentName),
      exported = info.exported,
      enabled = info.enabled,
      permission = readComponentPermission(info),
      process = shortenProcessName(packageInfo.packageName, info.processName),
      authorities = (info as? ProviderInfo)?.authority,
      targetActivity = (info as? ActivityInfo)?.targetActivity,
      label = info.nonLocalizedLabel?.toString()?.takeIf { it.isNotBlank() },
      labelRef = null,
      actions = actions,
      metaData = buildMetaData(info.metaData),
      sdk = null
    )
  }

  private fun readComponentPermission(info: ComponentInfo): String? {
    return when (info) {
      is ActivityInfo -> info.permission
      is ServiceInfo -> info.permission
      is ProviderInfo -> info.readPermission ?: info.writePermission
      else -> null
    }?.takeIf { it.isNotBlank() }
  }

  @Suppress("DEPRECATION")
  private fun buildMetaData(bundle: Bundle?): List<MetaDataItemExport> {
    bundle ?: return emptyList()
    return bundle.keySet().map { key ->
      val value = bundle.get(key)
      val resourceId = (value as? Int)?.takeIf { it != 0 }
      MetaDataItemExport(
        name = key,
        value = value?.toString(),
        resourceId = resourceId,
        hasResourceReference = resourceId != null,
        resolvedFromResource = false
      )
    }.sortedBy { it.name }
  }

  private fun buildBuildFeatures(packageInfo: PackageInfo): BuildFeaturesExport {
    val buildMetadata = readBuildMetadata(packageInfo)
    return BuildFeaturesExport(
      kotlinDetected = buildMetadata.kotlinDetected,
      kotlinVersion = buildMetadata.kotlinVersion,
      gradleVersion = buildMetadata.gradleVersion,
      composeDetected = buildMetadata.composeDetected,
      composeVersion = buildMetadata.composeVersion,
      agpVersion = buildMetadata.agpVersion,
      appMetadataVersion = null
    )
  }

  private fun readBuildMetadata(packageInfo: PackageInfo): BuildMetadataExport {
    val sourceDir = packageInfo.applicationInfo?.sourceDir ?: return BuildMetadataExport()
    return runCatching {
      ZipFileCompat(File(sourceDir)).use { zip ->
        val kotlinInfo = readKotlinPluginInfo(zip)
        val composeVersion = readFirstPresentLine(zip, COMPOSE_VERSION_ENTRIES)
        val composeDetected = composeVersion != null || zip.entries().any { entry ->
          val fileName = entry.name.substringAfterLast('/')
          entry.isDirectory.not() &&
            (fileName.startsWith("androidx.compose.ui") || fileName.startsWith("androidx.compose.material")) &&
            fileName.endsWith(".version")
        }

        BuildMetadataExport(
          kotlinDetected = kotlinInfo.kotlinVersion != null ||
            zip.getEntry("kotlin/kotlin.kotlin_builtins") != null ||
            zip.getEntry("META-INF/services/kotlinx.coroutines.CoroutineExceptionHandler") != null ||
            zip.getEntry("META-INF/services/kotlinx.coroutines.internal.MainDispatcherFactory") != null,
          kotlinVersion = kotlinInfo.kotlinVersion,
          gradleVersion = kotlinInfo.gradleVersion,
          composeDetected = composeDetected,
          composeVersion = composeVersion,
          agpVersion = readAgpVersion(zip)
        )
      }
    }.onFailure {
      Timber.w(it, "Failed to read build metadata: ${packageInfo.packageName}")
    }.getOrDefault(BuildMetadataExport())
  }

  private fun ZipFileCompat.entries(): Sequence<ZipEntry> {
    return getZipEntries().asSequence()
  }

  private fun readKotlinPluginInfo(zip: ZipFileCompat): KotlinPluginExport {
    val entry = zip.getEntry("kotlin-tooling-metadata.json") ?: return KotlinPluginExport()
    return runCatching {
      val json = InputStreamReader(zip.getInputStream(entry), Charsets.UTF_8).use { it.readText() }
      val metadata = json.fromJson<KotlinToolingMetadata>() ?: return@runCatching KotlinPluginExport()
      val kotlinAndroidTarget =
        metadata.projectTargets?.find { target -> target.target == "org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget" }
      KotlinPluginExport(
        kotlinVersion = metadata.buildPluginVersion.takeIf {
          metadata.buildPlugin == "org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper" || kotlinAndroidTarget != null
        },
        gradleVersion = metadata.buildSystemVersion.takeIf {
          metadata.buildSystem == "Gradle" && metadata.buildSystemVersion.isNotEmpty()
        }
      )
    }.getOrDefault(KotlinPluginExport())
  }

  private fun readAgpVersion(zip: ZipFileCompat): String? {
    zip.getEntry("META-INF/com/android/build/gradle/app-metadata.properties")?.let { entry ->
      runCatching {
        val properties = Properties()
        zip.getInputStream(entry).use { properties.load(it) }
        properties.getProperty(AGP_KEYWORD)?.takeIf { it.isNotBlank() }
      }.getOrNull()?.let { return it }
    }

    zip.getEntry("META-INF/MANIFEST.MF")?.let { entry ->
      runCatching {
        InputStreamReader(zip.getInputStream(entry), Charsets.UTF_8).buffered().useLines { lines ->
          lines.firstOrNull { it.startsWith(AGP_KEYWORD_MANIFEST_PREFIX) }
            ?.removePrefix(AGP_KEYWORD_MANIFEST_PREFIX)
            ?.takeIf { it.isNotBlank() }
        }
      }.getOrNull()?.let { return it }
    }

    return readFirstPresentLine(zip, DATA_BINDING_VERSION_ENTRIES)
  }

  private fun readFirstPresentLine(zip: ZipFileCompat, entries: Array<String>): String? {
    entries.forEach { name ->
      zip.getEntry(name)?.let { entry ->
        runCatching {
          InputStreamReader(zip.getInputStream(entry), Charsets.UTF_8).buffered().use { it.readLine() }
            ?.takeIf { line -> line.isNotBlank() }
        }.getOrNull()?.let { return it }
      }
    }
    return null
  }

  private fun buildSignatures(packageInfo: PackageInfo): SignaturesExport {
    val schemes = packageInfo.readSignatureSchemes()
    val signatures = packageInfo.readSignatures()
    val certificates = signatures
      .distinctBy { it.toByteArray().sha256() }
      .mapNotNull { signature ->
        runCatching {
          signature.toCertificateExport(schemes)
        }.onFailure {
          Timber.w(it, "Failed to parse signature: ${packageInfo.packageName}")
        }.getOrNull()
      }

    return SignaturesExport(schemes, certificates)
  }

  private fun PackageInfo.readSignatureSchemes(): List<String> {
    val apk = File(applicationInfo?.sourceDir ?: return emptyList())
    return runCatching {
      ApkSignatureSchemeDetector.detect(apk)
    }.onFailure {
      Timber.w(it, "Failed to detect signature schemes: $packageName")
    }.getOrDefault(emptyList())
  }

  @Suppress("DEPRECATION")
  private fun PackageInfo.readSignatures(): List<Signature> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && signingInfo != null) {
      if (signingInfo!!.hasMultipleSigners()) {
        signingInfo!!.apkContentsSigners
      } else {
        signingInfo!!.signingCertificateHistory
      }
    } else {
      signatures
    }.orEmpty().toList()
  }

  private fun Signature.toCertificateExport(schemes: List<String>): CertificateExport {
    val bytes = toByteArray()
    val certificate = CertificateFactory.getInstance("X.509")
      .generateCertificate(ByteArrayInputStream(bytes)) as X509Certificate
    val publicKey = certificate.publicKey
    return CertificateExport(
      schemes = schemes,
      sourceEntries = emptyList(),
      derLength = bytes.size.toLong(),
      version = certificate.version,
      serialNumber = IntegerValueExport(
        decimal = certificate.serialNumber.toString(10),
        hex = "0x${certificate.serialNumber.toPositiveByteArray().toHexString("").lowercase(Locale.US)}"
      ),
      issuer = certificate.issuerX500Principal.name,
      subject = certificate.subjectX500Principal.name,
      validity = ValidityExport(
        notBefore = certificate.notBefore.toIsoString(),
        notAfter = certificate.notAfter.toIsoString()
      ),
      publicKey = PublicKeyExport(
        format = publicKey.format,
        algorithm = publicKey.algorithm,
        algorithmOid = null,
        exponent = (publicKey as? RSAPublicKey)?.publicExponent?.toIntegerValue(),
        modulusSizeBits = (publicKey as? RSAPublicKey)?.modulus?.bitLength(),
        modulusHex = (publicKey as? RSAPublicKey)?.modulus?.toPositiveByteArray()?.toHexString(":"),
        y = (publicKey as? DSAPublicKey)?.y?.toString(),
        type = publicKey.takeUnless { it is RSAPublicKey || it is DSAPublicKey }?.javaClass?.simpleName
      ),
      signatureAlgorithm = SignatureAlgorithmExport(
        name = certificate.sigAlgName,
        oid = certificate.sigAlgOID
      ),
      fingerprints = FingerprintsExport(
        md5 = bytes.md5(":"),
        sha1 = bytes.sha1(":"),
        sha256 = bytes.sha256(":")
      ),
      charString = bytes.toHexString("").lowercase(Locale.US)
    )
  }

  private fun BigInteger.toIntegerValue(): IntegerValueExport {
    return IntegerValueExport(
      decimal = toString(10),
      hex = "0x${toPositiveByteArray().toHexString("").lowercase(Locale.US)}"
    )
  }

  private fun BigInteger.toPositiveByteArray(): ByteArray {
    val bytes = toByteArray()
    return if (bytes.size > 1 && bytes[0] == 0.toByte()) {
      bytes.copyOfRange(1, bytes.size)
    } else {
      bytes
    }
  }

  private fun buildAnalysisProfile(components: ComponentsExport): AnalysisProfileExport {
    return AnalysisProfileExport(
      id = "libchecker-installed-app-exporter",
      capabilities = listOf(
        "package-manager",
        "manifest",
        "native-libraries",
        "apk-signatures",
        "installed-app-icons"
      ),
      ruleCount = 0,
      iconCount = 0,
      uniqueSdkCount = 0,
      sdkMarkerCount = 0,
      nativeSdkMarkerCount = 0,
      componentSdkMarkerCount = 0,
      runtime = RuntimeProfileExport(
        worker = false,
        decompressionStream = false,
        system = buildTerminalSystem()
      ),
      stats = StatsExport(
        components = components.all.size
      )
    )
  }

  private fun Bitmap.toPngBytes(): ByteArray {
    return ByteArrayOutputStream().use { buffer ->
      compress(Bitmap.CompressFormat.PNG, 100, buffer)
      buffer.toByteArray()
    }
  }

  private fun buildIconPath(
    packageName: String,
    index: Int,
    usedPaths: MutableSet<String>
  ): String {
    val sanitized = packageName.replace(safeZipNameRegex, "_").ifBlank { "app_$index" }
    var path = "$ICONS_DIR/$sanitized.png"
    var suffix = 1
    while (!usedPaths.add(path)) {
      path = "$ICONS_DIR/$sanitized-$suffix.png"
      suffix++
    }
    return path
  }

  private fun shortenComponentName(packageName: String, componentName: String): String? {
    return componentName
      .takeIf { it.isNotBlank() }
      ?.let {
        if (it.startsWith("$packageName.")) {
          it.substring(packageName.length + 1)
        } else {
          it
        }
      }
  }

  private fun shortenProcessName(packageName: String, processName: String?): String? {
    val value = processName?.takeIf { it.isNotBlank() && it != packageName } ?: return null
    return when {
      value.startsWith("$packageName:") -> value.removePrefix(packageName)
      value.startsWith("$packageName.") -> value.removePrefix(packageName)
      else -> value
    }
  }

  private fun buildTerminalSystem(): TerminalSystemExport {
    return TerminalSystemExport(
      name = "Android",
      version = Build.VERSION.RELEASE.orEmpty(),
      sdkInt = Build.VERSION.SDK_INT,
      manufacturer = Build.MANUFACTURER.orEmpty(),
      model = Build.MODEL.orEmpty()
    )
  }

  private fun Date.toIsoString(): String {
    return isoDateFormat.get()!!.format(this)
  }

  private fun nowIsoString(): String {
    return Date(System.currentTimeMillis()).toIsoString()
  }

  private fun writeReport(writer: JsonWriter, report: ExportReport) {
    writer.beginObject()
    writer.writeString("locale", report.locale)
    writer.name("terminalSystem")
    writer.writeTerminalSystem(report.terminalSystem)
    writer.name("analysisProfile")
    writer.writeAnalysisProfile(report.analysisProfile)
    writer.writeNumber("durationMs", report.durationMs)
    writer.writeString("fileName", report.fileName)
    writer.writeNumber("fileSizeBytes", report.fileSizeBytes)
    writer.writeString("analyzedAt", report.analyzedAt)
    writer.name("apkInfo")
    writer.writeApkInfo(report.apkInfo)
    writer.endObject()
  }

  private fun JsonWriter.writeApkInfo(info: ApkInfoExport) {
    beginObject()
    writeString("appName", info.appName)
    writeString("packageName", info.packageName)
    writeString("versionName", info.versionName)
    writeString("versionCode", info.versionCode)
    writeString("minSdk", info.minSdk)
    writeString("targetSdk", info.targetSdk)
    writeString("compileSdk", info.compileSdk)
    name("icon")
    writeIcon(info.icon)
    writeStringArray("permissions", info.permissions)
    name("nativeLibraries")
    beginArray()
    info.nativeLibraries.forEach { writeNativeLibrary(it) }
    endArray()
    name("components")
    writeComponents(info.components)
    name("metaData")
    writeMetaData(info.metaData)
    name("buildFeatures")
    writeBuildFeatures(info.buildFeatures)
    name("signatures")
    writeSignatures(info.signatures)
    name("sdkSummary")
    writeSdkSummary(info.sdkSummary)
    endObject()
  }

  private fun JsonWriter.writeIcon(icon: IconEntry?) {
    if (icon == null) {
      nullValue()
      return
    }
    beginObject()
    nullNameValue("resourceId")
    writeString("path", icon.path)
    writeString("mimeType", MIME_TYPE_PNG)
    writeNumber("size", icon.size)
    writeString("dataUri", "")
    endObject()
  }

  private fun JsonWriter.writeNativeLibrary(library: NativeLibraryExport) {
    beginObject()
    writeString("abi", library.abi)
    writeString("name", library.name)
    writeString("path", library.path)
    writeNumber("size", library.size)
    name("sdk")
    writeSdkRule(library.sdk)
    endObject()
  }

  private fun JsonWriter.writeComponents(components: ComponentsExport) {
    beginObject()
    writeComponentArray("activities", components.activities)
    writeComponentArray("services", components.services)
    writeComponentArray("receivers", components.receivers)
    writeComponentArray("providers", components.providers)
    endObject()
  }

  private fun JsonWriter.writeComponentArray(name: String, components: List<ComponentExport>) {
    name(name)
    beginArray()
    components.forEach { writeComponent(it) }
    endArray()
  }

  private fun JsonWriter.writeComponent(component: ComponentExport) {
    beginObject()
    writeString("type", component.type)
    writeString("section", component.section)
    writeString("name", component.name)
    writeString("shortName", component.shortName)
    writeBoolean("exported", component.exported)
    writeBoolean("enabled", component.enabled)
    writeString("permission", component.permission)
    writeString("process", component.process)
    writeString("authorities", component.authorities)
    writeString("targetActivity", component.targetActivity)
    writeString("label", component.label)
    writeNumber("labelRef", component.labelRef)
    writeStringArray("actions", component.actions)
    name("metaData")
    beginArray()
    component.metaData.forEach { writeMetaDataItem(it) }
    endArray()
    name("sdk")
    writeSdkRule(component.sdk)
    endObject()
  }

  private fun JsonWriter.writeMetaData(metaData: MetaDataExport) {
    beginObject()
    name("application")
    beginArray()
    metaData.application.forEach { writeMetaDataItem(it) }
    endArray()
    name("components")
    beginArray()
    metaData.components.forEach { writeMetaDataItem(it) }
    endArray()
    endObject()
  }

  private fun JsonWriter.writeMetaDataItem(item: MetaDataItemExport) {
    beginObject()
    writeString("name", item.name)
    writeString("value", item.value)
    writeNumber("resourceId", item.resourceId)
    writeBoolean("hasResourceReference", item.hasResourceReference)
    writeBoolean("resolvedFromResource", item.resolvedFromResource)
    endObject()
  }

  private fun JsonWriter.writeBuildFeatures(features: BuildFeaturesExport) {
    beginObject()
    writeBoolean("kotlinDetected", features.kotlinDetected)
    writeString("kotlinVersion", features.kotlinVersion)
    writeString("gradleVersion", features.gradleVersion)
    writeBoolean("composeDetected", features.composeDetected)
    writeString("composeVersion", features.composeVersion)
    writeString("agpVersion", features.agpVersion)
    writeString("appMetadataVersion", features.appMetadataVersion)
    endObject()
  }

  private fun JsonWriter.writeSignatures(signatures: SignaturesExport) {
    beginObject()
    writeStringArray("schemes", signatures.schemes)
    name("certificates")
    beginArray()
    signatures.certificates.forEach { writeCertificate(it) }
    endArray()
    endObject()
  }

  private fun JsonWriter.writeCertificate(certificate: CertificateExport) {
    beginObject()
    writeStringArray("schemes", certificate.schemes)
    name("serialNumber")
    writeIntegerValue(certificate.serialNumber)
    writeNumber("version", certificate.version)
    writeString("issuer", certificate.issuer)
    writeString("subject", certificate.subject)
    name("validity")
    beginObject()
    writeString("notBefore", certificate.validity.notBefore)
    writeString("notAfter", certificate.validity.notAfter)
    endObject()
    name("publicKey")
    writePublicKey(certificate.publicKey)
    name("signatureAlgorithm")
    beginObject()
    writeString("name", certificate.signatureAlgorithm.name)
    writeString("oid", certificate.signatureAlgorithm.oid)
    endObject()
    name("fingerprints")
    beginObject()
    writeString("md5", certificate.fingerprints.md5)
    writeString("sha1", certificate.fingerprints.sha1)
    writeString("sha256", certificate.fingerprints.sha256)
    endObject()
    writeString("charString", certificate.charString)
    writeStringArray("sourceEntries", certificate.sourceEntries)
    writeNumber("derLength", certificate.derLength)
    endObject()
  }

  private fun JsonWriter.writePublicKey(publicKey: PublicKeyExport) {
    beginObject()
    writeString("format", publicKey.format)
    writeString("algorithm", publicKey.algorithm)
    writeString("algorithmOid", publicKey.algorithmOid)
    name("exponent")
    writeIntegerValue(publicKey.exponent)
    writeNumber("modulusSizeBits", publicKey.modulusSizeBits)
    writeString("modulusHex", publicKey.modulusHex)
    writeString("y", publicKey.y)
    writeString("type", publicKey.type)
    endObject()
  }

  private fun JsonWriter.writeIntegerValue(value: IntegerValueExport?) {
    if (value == null) {
      nullValue()
      return
    }
    beginObject()
    writeString("decimal", value.decimal)
    writeString("hex", value.hex)
    endObject()
  }

  private fun JsonWriter.writeSdkSummary(summary: SdkSummaryExport) {
    beginObject()
    writeSdkSummaryArray("native", summary.native)
    writeSdkSummaryArray("components", summary.components)
    endObject()
  }

  private fun JsonWriter.writeSdkSummaryArray(name: String, items: List<SdkSummaryItemExport>) {
    name(name)
    beginArray()
    items.forEach { item ->
      beginObject()
      writeString("key", item.key)
      writeString("label", item.label)
      writeString("iconName", item.iconName)
      writeString("iconUrl", item.iconUrl)
      writeBoolean("singleColorIcon", item.singleColorIcon)
      writeString("ruleDetail", item.ruleDetail)
      writeNumber("count", item.count)
      writeString("detail", item.detail)
      writeStringArray("previewItems", item.previewItems)
      endObject()
    }
    endArray()
  }

  private fun JsonWriter.writeSdkRule(rule: SdkRuleExport?) {
    if (rule == null) {
      nullValue()
      return
    }
    beginObject()
    writeString("label", rule.label)
    writeString("iconName", rule.iconName)
    writeString("iconUrl", rule.iconUrl)
    writeBoolean("singleColorIcon", rule.singleColorIcon)
    writeString("matchSource", rule.matchSource)
    writeString("regexName", rule.regexName)
    writeString("ruleDetail", rule.ruleDetail)
    writeNumber("type", rule.type)
    endObject()
  }

  private fun JsonWriter.writeAnalysisProfile(profile: AnalysisProfileExport) {
    beginObject()
    writeString("id", profile.id)
    writeStringArray("capabilities", profile.capabilities)
    writeNumber("ruleCount", profile.ruleCount)
    writeNumber("iconCount", profile.iconCount)
    writeNumber("uniqueSdkCount", profile.uniqueSdkCount)
    writeNumber("sdkMarkerCount", profile.sdkMarkerCount)
    writeNumber("nativeSdkMarkerCount", profile.nativeSdkMarkerCount)
    writeNumber("componentSdkMarkerCount", profile.componentSdkMarkerCount)
    name("runtime")
    beginObject()
    writeBoolean("worker", profile.runtime.worker)
    writeBoolean("decompressionStream", profile.runtime.decompressionStream)
    name("system")
    writeTerminalSystem(profile.runtime.system)
    endObject()
    name("stats")
    beginObject()
    writeNumber("components", profile.stats.components)
    endObject()
    endObject()
  }

  private fun JsonWriter.writeTerminalSystem(system: TerminalSystemExport) {
    beginObject()
    writeString("name", system.name)
    writeString("version", system.version)
    writeNumber("sdkInt", system.sdkInt)
    writeString("manufacturer", system.manufacturer)
    writeString("model", system.model)
    endObject()
  }

  private fun JsonWriter.writeString(name: String, value: String?) {
    name(name)
    if (value == null) {
      nullValue()
    } else {
      value(value)
    }
  }

  private fun JsonWriter.writeStringArray(name: String, values: List<String>) {
    name(name)
    beginArray()
    values.forEach { value(it) }
    endArray()
  }

  private fun JsonWriter.writeNumber(name: String, value: Number?) {
    name(name)
    if (value == null) {
      nullValue()
    } else {
      value(value)
    }
  }

  private fun JsonWriter.writeBoolean(name: String, value: Boolean?) {
    name(name)
    if (value == null) {
      nullValue()
    } else {
      value(value)
    }
  }

  private fun JsonWriter.nullNameValue(name: String) {
    name(name)
    nullValue()
  }

  private enum class ExportStage {
    ICONS,
    ICON_ENCODE,
    ICON_WRITE,
    LOAD_PACKAGE_INFO,
    NATIVE_LIBS,
    INTENT_ACTIONS,
    COMPONENTS,
    BUILD_FEATURES,
    SIGNATURES,
    ANALYSIS_PROFILE,
    WRITE_JSON
  }

  private class ExportProfiler(
    private val appCount: Int
  ) {
    private val totals = LongArray(ExportStage.entries.size)

    suspend fun <T> measure(
      stage: ExportStage,
      packageProfile: PackageExportProfile? = null,
      block: suspend () -> T
    ): T {
      val startedAt = SystemClock.elapsedRealtime()
      return try {
        block()
      } finally {
        val duration = SystemClock.elapsedRealtime() - startedAt
        synchronized(totals) {
          totals[stage.ordinal] += duration
        }
        packageProfile?.add(stage, duration)
      }
    }

    fun summary(): String {
      val snapshot = synchronized(totals) {
        totals.copyOf()
      }
      return ExportStage.entries.joinToString(prefix = "stages=[", postfix = "]") { stage ->
        "${stage.name.lowercase(Locale.US)}=${snapshot[stage.ordinal]}ms"
      } + " avgPerApp=${if (appCount > 0) snapshot.sum() / appCount else 0}ms"
    }
  }

  private class PackageExportProfile(
    private val packageName: String
  ) {
    private val totals = LongArray(ExportStage.entries.size)

    fun add(stage: ExportStage, duration: Long) {
      totals[stage.ordinal] += duration
    }

    fun summary(): String {
      return ExportStage.entries
        .filter { totals[it.ordinal] > 0 }
        .joinToString(prefix = "stages=[", postfix = "]") { stage ->
          "${stage.name.lowercase(Locale.US)}=${totals[stage.ordinal]}ms"
        } + " package=$packageName"
    }
  }

  private const val SLOW_ICON_THRESHOLD_MS = 500L
  private const val SLOW_PACKAGE_THRESHOLD_MS = 1500L
  private const val ICON_BATCH_SIZE = 8
  private const val REPORT_BATCH_SIZE = 8
  private const val ZIP_BUFFER_SIZE = 64 * 1024
  private const val AGP_KEYWORD = "androidGradlePluginVersion"
  private const val AGP_KEYWORD_MANIFEST_PREFIX = "Created-By: Android Gradle "

  private val COMPOSE_VERSION_ENTRIES = arrayOf(
    "META-INF/androidx.compose.runtime_runtime.version",
    "META-INF/androidx.compose.ui_ui.version",
    "META-INF/androidx.compose.ui_ui-tooling-preview.version",
    "META-INF/androidx.compose.foundation_foundation.version",
    "META-INF/androidx.compose.animation_animation.version"
  )

  private val DATA_BINDING_VERSION_ENTRIES = arrayOf(
    "META-INF/androidx.databinding_viewbinding.version",
    "META-INF/androidx.databinding_databindingKtx.version",
    "META-INF/androidx.databinding_library.version"
  )

  data class ExportResult(val appCount: Int)

  private data class ExportReport(
    val locale: String,
    val terminalSystem: TerminalSystemExport,
    val analysisProfile: AnalysisProfileExport,
    val durationMs: Long,
    val fileName: String,
    val fileSizeBytes: Long,
    val analyzedAt: String,
    val apkInfo: ApkInfoExport
  )

  private data class ApkInfoExport(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: String,
    val minSdk: String,
    val targetSdk: String,
    val compileSdk: String,
    val icon: IconEntry?,
    val permissions: List<String>,
    val nativeLibraries: List<NativeLibraryExport>,
    val components: ComponentsExport,
    val metaData: MetaDataExport,
    val buildFeatures: BuildFeaturesExport,
    val signatures: SignaturesExport,
    val sdkSummary: SdkSummaryExport
  )

  private data class IconEntry(val path: String, val size: Long)

  private data class EncodedIcon(
    val index: Int,
    val packageName: String,
    val bytes: ByteArray
  )

  private data class NativeLibraryExport(
    val abi: String,
    val name: String,
    val path: String,
    val size: Long,
    val sdk: SdkRuleExport?
  )

  private data class ComponentsExport(
    val activities: List<ComponentExport>,
    val services: List<ComponentExport>,
    val receivers: List<ComponentExport>,
    val providers: List<ComponentExport>
  ) {
    val all: List<ComponentExport>
      get() = activities + services + receivers + providers
  }

  private data class ComponentExport(
    val type: String,
    val section: String,
    val name: String,
    val shortName: String?,
    val exported: Boolean?,
    val enabled: Boolean?,
    val permission: String?,
    val process: String?,
    val authorities: String?,
    val targetActivity: String?,
    val label: String?,
    val labelRef: Long?,
    val actions: List<String>,
    val metaData: List<MetaDataItemExport>,
    val sdk: SdkRuleExport?
  )

  private data class MetaDataExport(
    val application: List<MetaDataItemExport>,
    val components: List<MetaDataItemExport>
  )

  private data class MetaDataItemExport(
    val name: String,
    val value: String?,
    val resourceId: Int?,
    val hasResourceReference: Boolean,
    val resolvedFromResource: Boolean
  )

  private data class BuildFeaturesExport(
    val kotlinDetected: Boolean,
    val kotlinVersion: String?,
    val gradleVersion: String?,
    val composeDetected: Boolean,
    val composeVersion: String?,
    val agpVersion: String?,
    val appMetadataVersion: String?
  )

  private data class BuildMetadataExport(
    val kotlinDetected: Boolean = false,
    val kotlinVersion: String? = null,
    val gradleVersion: String? = null,
    val composeDetected: Boolean = false,
    val composeVersion: String? = null,
    val agpVersion: String? = null
  )

  private data class KotlinPluginExport(
    val kotlinVersion: String? = null,
    val gradleVersion: String? = null
  )

  private data class SignaturesExport(
    val schemes: List<String>,
    val certificates: List<CertificateExport>
  )

  private data class CertificateExport(
    val schemes: List<String>,
    val sourceEntries: List<String>,
    val derLength: Long,
    val version: Int,
    val serialNumber: IntegerValueExport,
    val issuer: String,
    val subject: String,
    val validity: ValidityExport,
    val publicKey: PublicKeyExport,
    val signatureAlgorithm: SignatureAlgorithmExport,
    val fingerprints: FingerprintsExport,
    val charString: String
  )

  private data class IntegerValueExport(
    val decimal: String,
    val hex: String
  )

  private data class ValidityExport(
    val notBefore: String,
    val notAfter: String
  )

  private data class PublicKeyExport(
    val format: String?,
    val algorithm: String?,
    val algorithmOid: String?,
    val exponent: IntegerValueExport?,
    val modulusSizeBits: Int?,
    val modulusHex: String?,
    val y: String?,
    val type: String?
  )

  private data class SignatureAlgorithmExport(
    val name: String?,
    val oid: String?
  )

  private data class FingerprintsExport(
    val md5: String,
    val sha1: String,
    val sha256: String
  )

  private data class SdkSummaryExport(
    val native: List<SdkSummaryItemExport>,
    val components: List<SdkSummaryItemExport>
  )

  private data class SdkSummaryItemExport(
    val key: String,
    val label: String,
    val iconName: String,
    val iconUrl: String,
    val singleColorIcon: Boolean,
    val ruleDetail: String?,
    val count: Int,
    val detail: String,
    val previewItems: List<String>
  )

  private data class SdkRuleExport(
    val label: String,
    val iconName: String,
    val iconUrl: String,
    val singleColorIcon: Boolean,
    val matchSource: String,
    val regexName: String?,
    val ruleDetail: String?,
    val type: Int
  )

  private data class AnalysisProfileExport(
    val id: String,
    val capabilities: List<String>,
    val ruleCount: Int,
    val iconCount: Int,
    val uniqueSdkCount: Int,
    val sdkMarkerCount: Int,
    val nativeSdkMarkerCount: Int,
    val componentSdkMarkerCount: Int,
    val runtime: RuntimeProfileExport,
    val stats: StatsExport
  )

  private data class RuntimeProfileExport(
    val worker: Boolean,
    val decompressionStream: Boolean,
    val system: TerminalSystemExport
  )

  private data class StatsExport(
    val components: Int
  )

  private data class TerminalSystemExport(
    val name: String,
    val version: String,
    val sdkInt: Int,
    val manufacturer: String,
    val model: String
  )
}
