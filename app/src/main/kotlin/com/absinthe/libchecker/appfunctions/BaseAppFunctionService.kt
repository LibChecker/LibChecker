package com.absinthe.libchecker.appfunctions

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService as JetpackAppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.buildmetadata.ComposeResourcesDetector.usesComposeMultiplatformResources
import com.absinthe.libchecker.domain.app.feature.PackageFeatureDetector.getFeatures
import com.absinthe.libchecker.domain.app.list.AppListItemFactory
import com.absinthe.libchecker.domain.app.model.PackageInfoFormatter
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotDiffsUseCase
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailDiffTextStyle
import com.absinthe.libchecker.domain.snapshot.detail.usecase.SnapshotDetailSectionBuilder
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.statistics.reference.usecase.ComputeLibReferenceUseCase
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.isArchivedPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject

@RequiresApi(36)
@AppFunctionServiceEntryPoint(
  serviceName = "AppFunctionService",
  appFunctionXmlFileName = "libchecker_app_functions"
)
abstract class BaseAppFunctionService : JetpackAppFunctionService() {
  private val installedApps: InstalledAppRepository by inject()
  private val itemFactory: AppListItemFactory by inject()
  private val snapshots: SnapshotRepository by inject()
  private val compare: CompareSnapshotDiffsUseCase by inject()

  /**
   * Find installed applications by label, package name and optional detected feature.
   * Returns exact package identifiers for analyzeApp. Name matches can be ambiguous.
   * This reads the current inventory without changing the app's saved filters.
   * @param query Case-insensitive label or package substring; empty selects all.
   * @param feature Empty, kotlin, compose, compose_resources, agp, split_apks, xposed or pwa.
   * Detection means artifact presence, not proof of architecture or runtime use.
   * @param includeSystem Whether system applications are included; defaults to false.
   * @param offset Zero-based result offset, at least zero.
   * @param limit Page size from 1 to 100, default 20.
   * @return Matching apps ordered by package name, with paging and detection limitations.
   * @throws AppFunctionInvalidArgumentException For an unsupported feature or invalid paging.
   */
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  suspend fun findApps(
    query: String? = null,
    feature: String? = null,
    includeSystem: Boolean = false,
    offset: Int = 0,
    limit: Int? = null
  ): AppPage = withContext(Dispatchers.IO) {
    val limit = limit ?: 20
    validatePage(offset, limit)
    val query = query.orEmpty()
    validateQuery(query)
    val feature = feature.orEmpty()
    val flag = featureFlag(feature)
    val candidates = livePackages(includeSystem).filter {
      it.packageName.contains(query, true) || it.getAppName(packageManager).orEmpty().contains(query, true)
    }
    val warnings = mutableListOf<String>()
    val matches = candidates.filter { item ->
      currentCoroutineContext().ensureActive()
      if (flag == null) {
        true
      } else {
        val info = installedApps.getPackageInfo(item.packageName, PackageManager.GET_META_DATA)
        if (info == null || info.isArchivedPackage()) {
          warnings.add("${item.packageName}: feature not analyzed")
          false
        } else {
          if (feature == "compose_resources") {
            info.usesComposeMultiplatformResources()
          } else {
            val coroutineContext = currentCoroutineContext()
            info.getFeatures(coroutineContext::ensureActive) and flag != 0
          }
        }
      }
    }
    AppPage(
      apps = matches.drop(offset).take(limit).map {
        currentCoroutineContext().ensureActive()
        itemFactory.create(it, delayInitFeatures = true)
          .toRecord(if (feature.isEmpty()) emptyList() else listOf(feature))
      },
      total = matches.size,
      nextOffset = nextOffset(offset, limit, matches.size),
      observedAt = System.currentTimeMillis(),
      warnings = warnings
    )
  }

  /**
   * Analyze one installed application's selected technical category.
   * findApps supplies exact package names. Native results include each ABI and rule labels.
   * Empty detections are not proof of absence; permissions are declarations, not evidence of use.
   * No APK files, tokens, metadata values or app-private content are returned.
   * @param packageName Exact installed package name.
   * @param category One of features, native, permissions, services, activities, receivers or providers.
   * Defaults to native. Each call analyzes only the selected category.
   * @param offset Zero-based evidence offset, at least zero.
   * @param limit Number of observations, from 1 to 100, default 30.
   * @return App identity and a page of technical evidence.
   * @throws AppFunctionElementNotFoundException If the package is no longer installed.
   * @throws AppFunctionInvalidArgumentException For an unsupported category or invalid paging.
   */
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  suspend fun analyzeApp(
    packageName: String,
    category: String? = null,
    offset: Int = 0,
    limit: Int? = null
  ): AnalysisPage = withContext(Dispatchers.IO) {
    val limit = limit ?: 30
    val category = category ?: "native"
    validatePage(offset, limit)
    validateQuery(packageName, required = true)
    val flags = if (category == "features") PackageManager.GET_META_DATA else 0
    val info = installedApps.getPackageInfo(packageName, flags)
      ?: throw AppFunctionElementNotFoundException("Package not installed: $packageName")
    val item = itemFactory.create(info, delayInitFeatures = true)
    val evidence = analyzeEvidence(info, item, category, installedApps)
    AnalysisPage(
      app = item.toRecord(),
      evidence = evidence.drop(offset).take(limit),
      total = evidence.size,
      nextOffset = nextOffset(offset, limit, evidence.size),
      rulesVersion = get<com.absinthe.libchecker.domain.rules.CloudRulesRepository>().getLocalVersion(),
      observedAt = System.currentTimeMillis(),
      warnings = listOf("Static detection only. Unsupported, stripped or inaccessible artifacts may be absent from results.")
    )
  }

  /**
   * Find applications referring to a library artifact or recognized SDK label.
   * Reuses LibChecker's reference scanner and rule matching, with no minimum popularity threshold.
   * A label substring can match multiple SDKs; returned matches name the actual artifacts.
   * This scans one category and may be slower than findApps.
   * @param query Nonblank case-insensitive artifact name or SDK label substring, at most 200 characters.
   * @param category One of native, services, activities, receivers, providers or permissions.
   * Defaults to native; other categories require separate calls.
   * @param includeSystem Whether to include system apps, default false.
   * @param offset Zero-based app offset.
   * @param limit Page size from 1 to 100, default 20.
   * @return Matching applications with bounded artifact evidence.
   * @throws AppFunctionInvalidArgumentException For invalid query, category or paging.
   */
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  suspend fun findAppsUsingLibrary(
    query: String,
    category: String? = null,
    includeSystem: Boolean = false,
    offset: Int = 0,
    limit: Int? = null
  ): AppPage = withContext(Dispatchers.IO) {
    val limit = limit ?: 20
    validatePage(offset, limit)
    validateQuery(query, required = true)
    val option = referenceOption(category ?: "native")
    val items = livePackages(includeSystem)
    val scanner = get<ComputeLibReferenceUseCase>()
    val index = scanner.buildIndex(ComputeLibReferenceUseCase.ReferenceConfig(includeSystem, option)) {}
    currentCoroutineContext().ensureActive()
    checkNotNull(index) { "Reference scan did not complete" }
    val references = try {
      scanner.matchRules(index, ComputeLibReferenceUseCase.MatchConfig(1, false)) {}
    } finally {
      index.clear()
    }
    currentCoroutineContext().ensureActive()
    checkNotNull(references) { "Rule matching did not complete" }
    val evidenceByPackage = mutableMapOf<String, MutableSet<String>>()
    references.filter { it.libName.contains(query, true) || it.rule?.label?.contains(query, true) == true }
      .forEach { reference ->
        val evidence = "${reference.libName} (${reference.rule?.label.orEmpty()})"
        reference.referredList.forEach { name ->
          evidenceByPackage.getOrPut(name) { sortedSetOf() }.add(evidence)
        }
      }
    val matches = items.filter { it.packageName in evidenceByPackage }
    AppPage(
      apps = matches.drop(offset).take(limit).map {
        currentCoroutineContext().ensureActive()
        itemFactory.create(it, delayInitFeatures = true).toRecord(evidenceByPackage.getValue(it.packageName).take(20))
      },
      total = matches.size,
      nextOffset = nextOffset(offset, limit, matches.size),
      observedAt = System.currentTimeMillis(),
      warnings = listOf("Uses the existing reference scanner; inaccessible packages may be skipped. At most 20 matching artifacts per app.")
    )
  }

  /**
   * List existing saved snapshots, newest first, without creating a snapshot or changing selection.
   * Returned timestamps identify the comparison baselines for compareSnapshot.
   * An empty result means no saved snapshots are available; it does not describe current installed apps.
   * @param offset Zero-based snapshot offset.
   * @param limit Page size from 1 to 100, default 20.
   * @return Snapshot identifiers, capture times and application counts.
   * @throws AppFunctionInvalidArgumentException For invalid paging.
   */
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  suspend fun listSnapshots(offset: Int = 0, limit: Int? = null): SnapshotPage = withContext(Dispatchers.IO) {
    val limit = limit ?: 20
    validatePage(offset, limit)
    val nodes = snapshots.getTimeStamps().sortedByDescending { it.timestamp }
    val counts = snapshots.getSnapshotCountsByTimestamp()
    SnapshotPage(
      snapshots = nodes.drop(offset).take(limit).map { SnapshotRecord(it.timestamp, counts[it.timestamp] ?: 0) },
      total = nodes.size,
      nextOffset = nextOffset(offset, limit, nodes.size)
    )
  }

  /**
   * Compare a saved snapshot with installed applications or a newer saved snapshot.
   * listSnapshots supplies valid timestamps. Does not create, delete or select snapshots.
   * Reuses the app's comparison policy and derived cache; no history is inferred without a snapshot.
   * @param fromTimestamp Existing older snapshot identifier, in Unix milliseconds.
   * @param toTimestamp Existing newer snapshot identifier, or zero for currently installed apps.
   * @param packageName Exact package filter, or empty for an app-change overview.
   * A package filter also includes up to 100 technical changes; detailCount reports truncation.
   * @param offset Zero-based changed-app offset.
   * @param limit Page size from 1 to 100, default 20.
   * @return Version and technical change summaries ordered by package name.
   * @throws AppFunctionElementNotFoundException If a snapshot is missing or has no application data,
   * including when removed during comparison. Refresh listSnapshots before retrying.
   * @throws AppFunctionInvalidArgumentException For reversed timestamps or invalid paging.
   */
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  suspend fun compareSnapshot(
    fromTimestamp: Long,
    toTimestamp: Long = 0,
    packageName: String? = null,
    offset: Int = 0,
    limit: Int? = null
  ): ComparisonPage = withContext(Dispatchers.IO) {
    val limit = limit ?: 20
    val packageName = packageName.orEmpty()
    validatePage(offset, limit)
    validateQuery(packageName)
    if (fromTimestamp <= 0 || toTimestamp < 0 || (toTimestamp != 0L && toTimestamp < fromTimestamp)) {
      throw AppFunctionInvalidArgumentException("Use a positive baseline and a newer snapshot, or zero for current apps")
    }
    requireSnapshot(fromTimestamp)
    if (toTimestamp != 0L) requireSnapshot(toTimestamp)
    val diffs = compare(fromTimestamp, toTimestamp.takeIf { it != 0L }) {}
    currentCoroutineContext().ensureActive()
    // Snapshot deletion is independent of the comparison cache's mutex.
    requireSnapshot(fromTimestamp)
    if (toTimestamp != 0L) requireSnapshot(toTimestamp)
    if (diffs == null) {
      throw AppFunctionElementNotFoundException("Snapshot data unavailable. Refresh listSnapshots.")
    }
    val filtered = diffs.filter {
      (packageName.isEmpty() || it.packageName == packageName) &&
        (
          it.newInstalled || it.deleted || !it.isNothingChanged() ||
            it.versionCodeDiff.new != it.versionCodeDiff.old ||
            it.versionNameDiff.new != it.versionNameDiff.old ||
            it.packageSizeDiff.new != it.packageSizeDiff.old
          )
    }.sortedBy { it.packageName }
    ComparisonPage(
      fromTimestamp = fromTimestamp,
      toTimestamp = toTimestamp,
      changes = filtered.drop(offset).take(limit).map { it.toChange(packageName.isNotEmpty()) },
      total = filtered.size,
      nextOffset = nextOffset(offset, limit, filtered.size),
      observedAt = System.currentTimeMillis()
    )
  }

  private fun livePackages(includeSystem: Boolean): List<PackageInfo> = installedApps.getApplicationList(forceUpdate = true)
    .filter {
      val info = it.applicationInfo
      info != null && (includeSystem || info.flags and ApplicationInfo.FLAG_SYSTEM == 0)
    }.sortedBy { it.packageName }

  private fun LCItem.toRecord(matches: List<String> = emptyList()) = AppRecord(
    packageName,
    label,
    versionName,
    versionCode,
    isSystem,
    targetApi.toInt(),
    PackageInfoFormatter.getAbiString(this@BaseAppFunctionService, abi.toInt(), true),
    matches
  )

  private suspend fun requireSnapshot(timestamp: Long) {
    if (snapshots.getTimeStamp(timestamp) == null || (snapshots.getSnapshotCountsByTimestamp()[timestamp] ?: 0) == 0) {
      throw AppFunctionElementNotFoundException("Snapshot not found or empty: $timestamp. Refresh listSnapshots.")
    }
  }

  private suspend fun SnapshotDiffItem.toChange(includeDetails: Boolean): AppChange {
    val details = if (includeDetails) {
      get<SnapshotDetailSectionBuilder>()(this, SnapshotDetailDiffTextStyle(null, arrowColor = 0))
        .flatMap { section ->
          section.items.map {
            EvidenceRecord(section.title, it.item.name, it.reportText, it.ruleChip?.label.orEmpty())
          }
        }
    } else {
      emptyList()
    }
    return AppChange(
      packageName, labelDiff.new ?: labelDiff.old,
      if (newInstalled) {
        "added"
      } else if (deleted) {
        "removed"
      } else {
        "changed"
      },
      if (newInstalled) "" else versionNameDiff.old,
      if (deleted) "" else versionNameDiff.new ?: versionNameDiff.old,
      added, removed, changed, moved, details.take(100), details.size
    )
  }
}
