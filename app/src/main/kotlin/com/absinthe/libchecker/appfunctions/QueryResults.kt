package com.absinthe.libchecker.appfunctions

import androidx.appfunctions.AppFunctionSerializable

/** An installed application identified by its package name. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppRecord(
  /** Exact package name for subsequent analysis. */
  val packageName: String,
  /** User-visible application name. */
  val label: String,
  /** Installed version name. */
  val versionName: String,
  /** Installed version code. */
  val versionCode: Long,
  /** Whether this is a system application. */
  val system: Boolean,
  /** Android target API level. */
  val targetSdk: Int,
  /** Detected ABI description, not proof of runtime execution. */
  val abi: String,
  /** Matching library artifacts or technical features, when requested. */
  val matches: List<String>
)

/** A page of matching applications, ordered by package name. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppPage(
  /** Applications on this page. */
  val apps: List<AppRecord>,
  /** Total matching applications for this query. */
  val total: Int,
  /** Next offset, or -1 when there are no more matches. */
  val nextOffset: Int,
  /** Query completion time in Unix milliseconds. */
  val observedAt: Long,
  /** Detection limitations; empty does not imply a security assessment. */
  val warnings: List<String>
)

/** One technical observation about an installed application. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class EvidenceRecord(
  /** Category such as native, permissions, or features. */
  val category: String,
  /** Artifact, permission, component or feature name. */
  val name: String,
  /** Associated version, ABI or other observed value; empty if unavailable. */
  val value: String,
  /** Rule label when recognized; empty if unrecognized. */
  val ruleLabel: String
)

/** A bounded page of application analysis. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AnalysisPage(
  /** Identity and installed version of the analyzed application. */
  val app: AppRecord,
  /** Selected category's observations. */
  val evidence: List<EvidenceRecord>,
  /** Total observations in the selected category. */
  val total: Int,
  /** Next offset, or -1 when complete. */
  val nextOffset: Int,
  /** Local rule data version used for matching. */
  val rulesVersion: Int,
  /** Query completion time in Unix milliseconds. */
  val observedAt: Long,
  /** Detection limitations, including incomplete evidence. */
  val warnings: List<String>
)

/** One saved snapshot. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SnapshotRecord(
  /** Snapshot identifier and capture time in Unix milliseconds. */
  val timestamp: Long,
  /** Number of application records in the snapshot. */
  val appCount: Int
)

/** Saved snapshot inventory, newest first. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SnapshotPage(
  /** Saved snapshots on this page. */
  val snapshots: List<SnapshotRecord>,
  /** Total saved snapshots. */
  val total: Int,
  /** Next offset, or -1 when complete. */
  val nextOffset: Int
)

/** One application change between snapshot states. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppChange(
  /** Application package name. */
  val packageName: String,
  /** Application label from the newer state, or older state if removed. */
  val label: String,
  /** added, removed or changed. */
  val change: String,
  /** Older version name; empty for newly installed apps. */
  val oldVersion: String,
  /** Newer version name; empty for removed apps. */
  val newVersion: String,
  /** Number of added technical items according to the existing snapshot comparison. */
  val added: Int,
  /** Number of removed technical items. */
  val removed: Int,
  /** Number of changed technical items. */
  val changed: Int,
  /** Number of moved technical items. */
  val moved: Int,
  /** Bounded details when a package filter is supplied. */
  val details: List<EvidenceRecord>,
  /** Total details before the 100-item response limit. */
  val detailCount: Int
)

/** A page of snapshot differences. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ComparisonPage(
  /** Older snapshot identifier. */
  val fromTimestamp: Long,
  /** Newer snapshot identifier, or zero for currently installed apps. */
  val toTimestamp: Long,
  /** Application changes ordered by package name. */
  val changes: List<AppChange>,
  /** Total matching changed applications. */
  val total: Int,
  /** Next offset, or -1 when complete. */
  val nextOffset: Int,
  /** Query completion time in Unix milliseconds. */
  val observedAt: Long
)
