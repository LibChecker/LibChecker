package com.absinthe.libchecker.di

import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.data.app.AndroidAppListExportMetadata
import com.absinthe.libchecker.data.app.AndroidAppListItemFactory
import com.absinthe.libchecker.data.app.GlobalAppDetailSettingsRepository
import com.absinthe.libchecker.data.app.GlobalAppListSettingsRepository
import com.absinthe.libchecker.data.app.LocalAppListRepository
import com.absinthe.libchecker.data.app.LocalInstalledAppRepository
import com.absinthe.libchecker.data.app.RemoteLibraryDetailRepository
import com.absinthe.libchecker.data.app.WorkerFeatureInitializationRepository
import com.absinthe.libchecker.data.app.update.AndroidAppUpdateRepository
import com.absinthe.libchecker.data.rules.AndroidCloudRulesRepository
import com.absinthe.libchecker.data.rules.GlobalRuleSettingsRepository
import com.absinthe.libchecker.data.settings.GlobalAppearanceSettingsRepository
import com.absinthe.libchecker.data.settings.GlobalDeveloperSettingsRepository
import com.absinthe.libchecker.data.snapshot.AndroidSnapshotItemFactory
import com.absinthe.libchecker.data.snapshot.GlobalSnapshotSelectionRepository
import com.absinthe.libchecker.data.snapshot.GlobalSnapshotSettingsRepository
import com.absinthe.libchecker.data.snapshot.GlobalSnapshotTrackChangeRepository
import com.absinthe.libchecker.data.snapshot.LocalSnapshotDatabaseBackupExporter
import com.absinthe.libchecker.data.snapshot.LocalSnapshotDatabaseBackupRestorer
import com.absinthe.libchecker.data.snapshot.LocalSnapshotDatabaseFileRepository
import com.absinthe.libchecker.data.snapshot.LocalSnapshotRepository
import com.absinthe.libchecker.data.snapshot.OnceSnapshotCaptureStateRepository
import com.absinthe.libchecker.data.snapshot.ProtoSnapshotArchiveCodec
import com.absinthe.libchecker.data.statistics.CachedAndroidDistributionRepository
import com.absinthe.libchecker.data.statistics.GlobalChartSettingsRepository
import com.absinthe.libchecker.data.statistics.GlobalLibReferenceSettingsRepository
import com.absinthe.libchecker.database.LCDatabase
import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.backup.RoomBackup
import com.absinthe.libchecker.domain.app.AppDetailSettingsRepository
import com.absinthe.libchecker.domain.app.AppListExportMetadata
import com.absinthe.libchecker.domain.app.AppListItemFactory
import com.absinthe.libchecker.domain.app.AppListItemsEquivalenceUseCase
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.app.AppListSettingsRepository
import com.absinthe.libchecker.domain.app.BuildAppExportNativeLibrariesUseCase
import com.absinthe.libchecker.domain.app.BuildAppListItemViewStatesUseCase
import com.absinthe.libchecker.domain.app.BuildAppListUpdatePlanUseCase
import com.absinthe.libchecker.domain.app.BuildNativeLibraryItemDisplayDataUseCase
import com.absinthe.libchecker.domain.app.CheckRequiredPackageAvailabilityUseCase
import com.absinthe.libchecker.domain.app.ClearApkCacheUseCase
import com.absinthe.libchecker.domain.app.ExportAppListToUriUseCase
import com.absinthe.libchecker.domain.app.ExportAppListUseCase
import com.absinthe.libchecker.domain.app.ExportInstalledAppsToUriUseCase
import com.absinthe.libchecker.domain.app.FeatureInitializationRepository
import com.absinthe.libchecker.domain.app.FilterAppListItemsUseCase
import com.absinthe.libchecker.domain.app.GetApkPreviewInfoUseCase
import com.absinthe.libchecker.domain.app.GetAppBundleItemsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailPackageUseCase
import com.absinthe.libchecker.domain.app.GetAppListContentUseCase
import com.absinthe.libchecker.domain.app.GetAppListPackageStatesUseCase
import com.absinthe.libchecker.domain.app.GetArchivePackageInfoUseCase
import com.absinthe.libchecker.domain.app.GetInstalledAppComparisonPackageUseCase
import com.absinthe.libchecker.domain.app.GetLibraryDetailUseCase
import com.absinthe.libchecker.domain.app.GetRandomAppIconUseCase
import com.absinthe.libchecker.domain.app.GetRelatedAppListItemUseCase
import com.absinthe.libchecker.domain.app.InitializeAppListUseCase
import com.absinthe.libchecker.domain.app.InitializePendingAppFeaturesUseCase
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.app.LibraryDetailRepository
import com.absinthe.libchecker.domain.app.ObserveAppListLoadingUseCase
import com.absinthe.libchecker.domain.app.PrepareApkAnalysisPackageUseCase
import com.absinthe.libchecker.domain.app.ResolveAppResourceValueUseCase
import com.absinthe.libchecker.domain.app.SetApkAnalysisEnabledUseCase
import com.absinthe.libchecker.domain.app.SyncAppListChangesUseCase
import com.absinthe.libchecker.domain.app.detail.BuildAppDetailAbiLabelDataUseCase
import com.absinthe.libchecker.domain.app.detail.BuildAppDetailHeaderExtraInfoUseCase
import com.absinthe.libchecker.domain.app.detail.BuildAppDetailHeaderTitleDataUseCase
import com.absinthe.libchecker.domain.app.detail.BuildRelatedAppDisplayDataUseCase
import com.absinthe.libchecker.domain.app.detail.GetAppDetailAbiUseCase
import com.absinthe.libchecker.domain.app.detail.GetAppDetailFeaturesUseCase
import com.absinthe.libchecker.domain.app.detail.GetAppDetailPackageSizeUseCase
import com.absinthe.libchecker.domain.app.detail.GetRelatedAppDisplayDataUseCase
import com.absinthe.libchecker.domain.app.detail.ShouldShowStaticLibraryTabUseCase
import com.absinthe.libchecker.domain.app.detail.action.AllowFileUriExposureUseCase
import com.absinthe.libchecker.domain.app.detail.action.BuildDetailItemDialogRequestUseCase
import com.absinthe.libchecker.domain.app.detail.action.BuildDetailItemLongClickActionsUseCase
import com.absinthe.libchecker.domain.app.detail.action.BuildSignatureDetailItemsUseCase
import com.absinthe.libchecker.domain.app.detail.action.ExportAppPackageShareFileUseCase
import com.absinthe.libchecker.domain.app.detail.action.ExtractNativeLibraryUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetAlternativeLaunchItemsUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetAppInfoActionsUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetAppInfoPrimaryActionsUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetAppInstallSourceDetailsUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetAppLaunchActionUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetAppManifestPropertiesUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetElfDetailUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetLibraryDetailDialogDataUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetOverlayDetailUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetPermissionDetailUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetXposedModuleInfoUseCase
import com.absinthe.libchecker.domain.app.detail.action.PrepareAppPackageShareActionUseCase
import com.absinthe.libchecker.domain.app.detail.action.PrepareAppPackageShareFileUseCase
import com.absinthe.libchecker.domain.app.detail.content.BuildAppDetailContentInitPlanUseCase
import com.absinthe.libchecker.domain.app.detail.content.BuildAppDetailTabTypesUseCase
import com.absinthe.libchecker.domain.app.detail.content.BuildDetailProcessFilterDataUseCase
import com.absinthe.libchecker.domain.app.detail.content.FilterAppDetailItemsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailAbilityChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailComponentChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailComponentsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailDexChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailMetadataChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailNativeLibrariesUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailPermissionChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailSignatureChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailStaticLibraryChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.SortAppDetailItemsUseCase
import com.absinthe.libchecker.domain.app.detail.feature.BuildAppDetailFeatureItemUseCase
import com.absinthe.libchecker.domain.app.detail.navigation.BuildDetailReferenceNavigationUseCase
import com.absinthe.libchecker.domain.app.search.HandleAppListSearchCommandUseCase
import com.absinthe.libchecker.domain.app.update.AppUpdateRepository
import com.absinthe.libchecker.domain.app.update.BuildInAppUpdateDiffDataUseCase
import com.absinthe.libchecker.domain.rules.CloudRulesRepository
import com.absinthe.libchecker.domain.rules.RuleSettingsRepository
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.domain.settings.repository.AppearanceSettingsRepository
import com.absinthe.libchecker.domain.settings.repository.DeveloperSettingsRepository
import com.absinthe.libchecker.domain.settings.usecase.BuildGetUpdatesItemsUseCase
import com.absinthe.libchecker.domain.settings.usecase.BuildLocalePreferenceDataUseCase
import com.absinthe.libchecker.domain.settings.usecase.BuildLogShareIntentUseCase
import com.absinthe.libchecker.domain.settings.usecase.SelectDarkModeUseCase
import com.absinthe.libchecker.domain.settings.usecase.SelectLocaleUseCase
import com.absinthe.libchecker.domain.snapshot.BuildArchiveSnapshotItemUseCase
import com.absinthe.libchecker.domain.snapshot.BuildInstalledSnapshotItemUseCase
import com.absinthe.libchecker.domain.snapshot.BuildPackageComparisonSnapshotItemUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotAbiDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotCapturePlanUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotComparisonListsUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotComparisonPlanUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotListUpdatePlanUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotPairDiffUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotSystemPropDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.CaptureInstalledSnapshotUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotDiffsUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotItemWithInstalledAppUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotItemsUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotListsUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotWithInstalledAppsUseCase
import com.absinthe.libchecker.domain.snapshot.DeleteSnapshotTimeStampUseCase
import com.absinthe.libchecker.domain.snapshot.FormatSnapshotTimestampUseCase
import com.absinthe.libchecker.domain.snapshot.GetApexPackageNamesUseCase
import com.absinthe.libchecker.domain.snapshot.GetSnapshotDashboardCountUseCase
import com.absinthe.libchecker.domain.snapshot.GetSnapshotPackageIconSourcesUseCase
import com.absinthe.libchecker.domain.snapshot.GetSnapshotRuleUseCase
import com.absinthe.libchecker.domain.snapshot.GetSnapshotSystemPropDiffsUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotArchiveCodec
import com.absinthe.libchecker.domain.snapshot.SnapshotArchiveUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotCaptureStateRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotDatabaseFileRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotItemFactory
import com.absinthe.libchecker.domain.snapshot.SnapshotLibraryUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotSelectionRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotSelectionUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository
import com.absinthe.libchecker.domain.snapshot.UpdateSnapshotDiffItemsUseCase
import com.absinthe.libchecker.domain.snapshot.UpdateSnapshotTopAppsUseCase
import com.absinthe.libchecker.domain.snapshot.backup.presentation.SnapshotBackupViewModel
import com.absinthe.libchecker.domain.snapshot.backup.usecase.BackupSnapshotArchiveToUriUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.BuildSnapshotRestorePlanUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.CreateSnapshotDatabaseBackupUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.GetSnapshotBackupTargetUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.PrepareRoomBackupRestoreFileUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.RestoreSnapshotArchiveFromUriUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.RestoreSnapshotDatabaseBackupUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.presentation.SnapshotComparisonViewModel
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.PrepareSnapshotComparisonArchivesUseCase
import com.absinthe.libchecker.domain.snapshot.detail.usecase.BuildSnapshotDetailItemsUseCase
import com.absinthe.libchecker.domain.snapshot.detail.usecase.BuildSnapshotDetailSectionsUseCase
import com.absinthe.libchecker.domain.snapshot.display.BuildSnapshotUpdateTimeDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.timenode.BuildSnapshotTimeNodeItemsUseCase
import com.absinthe.libchecker.domain.snapshot.timenode.UpdateSnapshotAutoRemoveThresholdUseCase
import com.absinthe.libchecker.domain.snapshot.track.presentation.TrackViewModel
import com.absinthe.libchecker.domain.snapshot.track.repository.SnapshotTrackChangeRepository
import com.absinthe.libchecker.domain.snapshot.track.usecase.CompareTrackedSnapshotListsUseCase
import com.absinthe.libchecker.domain.snapshot.track.usecase.GetTrackListItemsUseCase
import com.absinthe.libchecker.domain.snapshot.track.usecase.SetPackageTrackedUseCase
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartViewModel
import com.absinthe.libchecker.domain.statistics.chart.repository.AndroidDistributionRepository
import com.absinthe.libchecker.domain.statistics.chart.repository.ChartSettingsRepository
import com.absinthe.libchecker.domain.statistics.chart.source.ChartDataProvider
import com.absinthe.libchecker.domain.statistics.chart.source.ChartDataSourceFactory
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildAbiChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildApiLevelChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildDetailedAbiChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildFeatureFlagChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildPageSize16KBChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.GetAndroidDistributionUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.ObserveChartFeatureInitializationPlansUseCase
import com.absinthe.libchecker.domain.statistics.reference.presentation.LibReferenceComputationController
import com.absinthe.libchecker.domain.statistics.reference.presentation.LibReferenceViewModel
import com.absinthe.libchecker.domain.statistics.reference.repository.LibReferenceSettingsRepository
import com.absinthe.libchecker.domain.statistics.reference.usecase.BuildLibReferenceDetailDialogRequestUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.ComputeLibReferenceUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.GetLibReferenceAppsUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.GetLibReferenceConfigUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.GetLibReferenceIconPackagesUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.UpdateLibReferenceThresholdUseCase
import com.absinthe.libchecker.features.applist.detail.DetailActionLoader
import com.absinthe.libchecker.features.applist.detail.DetailContentLoader
import com.absinthe.libchecker.features.applist.detail.DetailFeatureLoader
import com.absinthe.libchecker.features.applist.detail.DetailFilterController
import com.absinthe.libchecker.features.applist.detail.DetailPackageLoader
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.applist.detail.content.DetailChipContentLoader
import com.absinthe.libchecker.features.applist.detail.content.DetailComponentContentLoader
import com.absinthe.libchecker.features.applist.detail.content.DetailNativeLibContentLoader
import com.absinthe.libchecker.features.applist.detail.content.DetailPermissionContentLoader
import com.absinthe.libchecker.features.applist.detail.ui.ApkDetailActivity
import com.absinthe.libchecker.features.home.HomeViewModel
import com.absinthe.libchecker.features.snapshot.SnapshotViewModel
import com.jakewharton.processphoenix.ProcessPhoenix
import jonathanfinerty.once.Once
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val appModule = module {
  single<LCRepository> { Repositories.lcRepository }
  single<AppDetailSettingsRepository> { GlobalAppDetailSettingsRepository() }
  single<AppListSettingsRepository> { GlobalAppListSettingsRepository() }
  single<InstalledAppRepository> { LocalInstalledAppRepository }
  single<AppListRepository> { LocalAppListRepository }
  single<LibraryDetailRepository> { RemoteLibraryDetailRepository }
  single<CloudRulesRepository> { AndroidCloudRulesRepository(androidContext()) }
  single<RuleSettingsRepository> { GlobalRuleSettingsRepository() }
  single<AppearanceSettingsRepository> { GlobalAppearanceSettingsRepository() }
  single<DeveloperSettingsRepository> { GlobalDeveloperSettingsRepository() }
  single<AppUpdateRepository> { AndroidAppUpdateRepository(SystemServices.downloadManager) }
  single<AndroidDistributionRepository> { CachedAndroidDistributionRepository(androidContext()) }
  single<AppListItemFactory> { AndroidAppListItemFactory(androidContext()) }
  single<AppListExportMetadata> { AndroidAppListExportMetadata(androidContext()) }
  single<FeatureInitializationRepository> { WorkerFeatureInitializationRepository() }
  single<SnapshotItemFactory> { AndroidSnapshotItemFactory() }
  single<SnapshotCaptureStateRepository> { OnceSnapshotCaptureStateRepository() }
  single<SnapshotSelectionRepository> { GlobalSnapshotSelectionRepository() }
  single<SnapshotSettingsRepository> { GlobalSnapshotSettingsRepository() }
  single<SnapshotTrackChangeRepository> { GlobalSnapshotTrackChangeRepository() }
  single<SnapshotDatabaseFileRepository> { LocalSnapshotDatabaseFileRepository() }
  single<SnapshotRepository> { LocalSnapshotRepository(get(), get()) }
  single<SnapshotArchiveCodec> { ProtoSnapshotArchiveCodec() }
  single<ChartSettingsRepository> { GlobalChartSettingsRepository() }
  single<LibReferenceSettingsRepository> { GlobalLibReferenceSettingsRepository() }
  single { AllowFileUriExposureUseCase() }
  factory { InitializeAppListUseCase(get(), get(), get()) }
  factory { ObserveAppListLoadingUseCase(get()) }
  factory { SyncAppListChangesUseCase(get(), get(), get()) }
  factory { BuildAbiChartDataUseCase() }
  factory { BuildApiLevelChartDataUseCase(get()) }
  factory { BuildDetailedAbiChartDataUseCase(androidContext(), get()) }
  factory { BuildFeatureFlagChartDataUseCase() }
  factory { BuildPageSize16KBChartDataUseCase(get()) }
  factory { ComputeLibReferenceUseCase(get()) }
  factory { GetAndroidDistributionUseCase(get()) }
  factory { GetLibReferenceConfigUseCase(get()) }
  factory { GetLibReferenceIconPackagesUseCase(get()) }
  factory { GetLibReferenceAppsUseCase(get()) }
  factory { BuildLibReferenceDetailDialogRequestUseCase() }
  factory { ObserveChartFeatureInitializationPlansUseCase(get()) }
  factory { UpdateLibReferenceThresholdUseCase(get()) }
  factory { LibReferenceComputationController.Factory(get(), get(), get()) }
  factory { ExportAppListUseCase(get(), get()) }
  factory { ExportAppListToUriUseCase(androidContext().contentResolver, get()) }
  factory { BuildAppDetailContentInitPlanUseCase() }
  factory { BuildAppDetailTabTypesUseCase() }
  factory { BuildDetailProcessFilterDataUseCase() }
  factory { FilterAppDetailItemsUseCase() }
  factory { FilterAppListItemsUseCase(get()) }
  factory { GetAlternativeLaunchItemsUseCase(androidContext().packageManager, get()) }
  factory { GetAppBundleItemsUseCase() }
  factory { GetAppDetailAbiUseCase() }
  factory { GetAppDetailAbilityChipsUseCase(androidContext()) }
  factory { GetAppDetailComponentsUseCase(get()) }
  factory { GetAppDetailComponentChipsUseCase(get()) }
  factory { GetAppDetailDexChipsUseCase() }
  factory { GetAppDetailFeaturesUseCase(get(), get()) }
  factory {
    GetAppInfoActionsUseCase(BuildConfig.APPLICATION_ID, androidContext().packageManager, get(), get())
  }
  factory { GetAppInstallSourceDetailsUseCase(androidContext(), get()) }
  factory { GetAppLaunchActionUseCase() }
  factory { GetAppInfoPrimaryActionsUseCase(BuildConfig.APPLICATION_ID, get()) }
  factory { GetAppDetailMetadataChipsUseCase(androidContext().packageManager) }
  factory { GetAppDetailNativeLibrariesUseCase(get()) }
  factory { GetAppDetailStaticLibraryChipsUseCase() }
  factory { GetAppDetailPackageUseCase(get()) }
  factory { GetAppDetailPackageSizeUseCase(get()) }
  factory { ExtractNativeLibraryUseCase(androidContext(), BuildConfig.APPLICATION_ID) }
  factory { GetAppDetailPermissionChipsUseCase() }
  factory { GetAppDetailSignatureChipsUseCase(androidContext(), get()) }
  factory { PrepareAppPackageShareFileUseCase(androidContext(), BuildConfig.APPLICATION_ID, get()) }
  factory { PrepareAppPackageShareActionUseCase(get()) }
  factory { ExportAppPackageShareFileUseCase(androidContext().contentResolver) }
  factory { ResolveAppResourceValueUseCase(androidContext().packageManager) }
  factory { GetApkPreviewInfoUseCase() }
  factory { GetAppListContentUseCase(BuildConfig.APPLICATION_ID, get(), get(), get(), get()) }
  factory { HandleAppListSearchCommandUseCase(get()) }
  factory { GetAppListPackageStatesUseCase(get()) }
  factory { BuildAppDetailAbiLabelDataUseCase(androidContext()) }
  factory { BuildAppDetailFeatureItemUseCase() }
  factory { BuildAppDetailHeaderExtraInfoUseCase(androidContext(), get()) }
  factory { BuildAppDetailHeaderTitleDataUseCase(androidContext(), get()) }
  factory { BuildAppExportNativeLibrariesUseCase() }
  factory {
    ExportInstalledAppsToUriUseCase(androidContext(), androidContext().contentResolver, get(), get())
  }
  factory { AppListItemsEquivalenceUseCase() }
  factory { BuildAppListItemViewStatesUseCase(androidContext(), get()) }
  factory { BuildAppListUpdatePlanUseCase() }
  factory { BuildInAppUpdateDiffDataUseCase(BuildConfig.APPLICATION_ID, androidContext().packageManager, get()) }
  factory { BuildNativeLibraryItemDisplayDataUseCase(androidContext()) }
  factory { BuildRelatedAppDisplayDataUseCase(androidContext()) }
  factory { BuildDetailItemDialogRequestUseCase() }
  factory { BuildDetailItemLongClickActionsUseCase() }
  factory { BuildDetailReferenceNavigationUseCase() }
  factory { BuildSignatureDetailItemsUseCase() }
  factory { ClearApkCacheUseCase(androidContext()) }
  factory { CheckRequiredPackageAvailabilityUseCase(get()) }
  factory { GetAppManifestPropertiesUseCase(androidContext().packageManager) }
  factory { GetArchivePackageInfoUseCase() }
  factory { PrepareApkAnalysisPackageUseCase(androidContext().contentResolver, get()) }
  factory { GetElfDetailUseCase(get()) }
  factory { GetInstalledAppComparisonPackageUseCase(get()) }
  factory { GetLibraryDetailUseCase(get()) }
  factory { GetLibraryDetailDialogDataUseCase(get()) }
  factory { GetOverlayDetailUseCase(androidContext(), get()) }
  factory { GetPermissionDetailUseCase(androidContext().packageManager, get()) }
  factory { GetRandomAppIconUseCase(androidContext().packageManager, get()) }
  factory { GetRelatedAppListItemUseCase(get(), get()) }
  factory { GetRelatedAppDisplayDataUseCase(get(), get()) }
  factory { GetXposedModuleInfoUseCase(androidContext().packageManager, get()) }
  factory {
    SetApkAnalysisEnabledUseCase(
      BuildConfig.APPLICATION_ID,
      androidContext().packageManager,
      ApkDetailActivity::class.java.name
    )
  }
  factory { BuildGetUpdatesItemsUseCase(androidContext()) }
  factory { BuildLocalePreferenceDataUseCase(get()) }
  factory { BuildLogShareIntentUseCase(androidContext(), BuildConfig.APPLICATION_ID) }
  factory { SelectDarkModeUseCase(get()) }
  factory { SelectLocaleUseCase(get()) }
  factory { ShouldShowStaticLibraryTabUseCase(get()) }
  factory { InitializePendingAppFeaturesUseCase(get(), get()) }
  factory { SortAppDetailItemsUseCase() }
  factory {
    DetailActionLoader(
      getAlternativeLaunchItemsUseCase = get(),
      getAppBundleItemsUseCase = get(),
      getAppInfoActionsUseCase = get(),
      getAppInfoPrimaryActionsUseCase = get(),
      getAppInstallSourceDetailsUseCase = get(),
      getAppLaunchActionUseCase = get(),
      getAppManifestPropertiesUseCase = get(),
      getElfDetailUseCase = get(),
      getLibraryDetailDialogDataUseCase = get(),
      getOverlayDetailUseCase = get(),
      getPermissionDetailUseCase = get(),
      getRelatedAppDisplayDataUseCase = get(),
      getXposedModuleInfoUseCase = get(),
      buildDetailItemDialogRequestUseCase = get(),
      buildDetailItemLongClickActionsUseCase = get(),
      buildSignatureDetailItemsUseCase = get(),
      extractNativeLibraryUseCase = get(),
      prepareAppPackageShareActionUseCase = get(),
      exportAppPackageShareFileUseCase = get(),
      buildDetailReferenceNavigationUseCase = get()
    )
  }
  factory {
    DetailChipContentLoader(
      getAppDetailDexChipsUseCase = get(),
      getAppDetailMetadataChipsUseCase = get(),
      getAppDetailSignatureChipsUseCase = get(),
      getAppDetailStaticLibraryChipsUseCase = get()
    )
  }
  factory {
    DetailComponentContentLoader(
      getAppDetailComponentChipsUseCase = get()
    )
  }
  factory {
    DetailNativeLibContentLoader(
      getAppDetailNativeLibrariesUseCase = get()
    )
  }
  factory {
    DetailPermissionContentLoader(
      getAppDetailPermissionChipsUseCase = get()
    )
  }
  factory {
    DetailContentLoader(
      getAppDetailAbilityChipsUseCase = get(),
      detailChipContentLoader = get(),
      detailComponentContentLoader = get(),
      detailNativeLibContentLoader = get(),
      detailPermissionContentLoader = get(),
      appDetailSettingsRepository = get()
    )
  }
  factory {
    DetailFilterController(
      filterAppDetailItemsUseCase = get(),
      sortAppDetailItemsUseCase = get(),
      buildDetailProcessFilterDataUseCase = get(),
      appDetailSettingsRepository = get()
    )
  }
  factory {
    DetailFeatureLoader(
      getAppDetailAbiUseCase = get(),
      getAppDetailFeaturesUseCase = get(),
      buildAppDetailAbiLabelDataUseCase = get(),
      buildAppDetailHeaderExtraInfoUseCase = get(),
      buildAppDetailHeaderTitleDataUseCase = get(),
      buildAppDetailFeatureItemUseCase = get(),
      shouldShowStaticLibraryTabUseCase = get()
    )
  }
  factory {
    DetailPackageLoader(
      getAppDetailPackage = get(),
      getApkPreviewInfoUseCase = get(),
      prepareApkAnalysisPackageUseCase = get(),
      getInstalledAppComparisonPackageUseCase = get(),
      buildPackageComparisonSnapshotItemUseCase = get()
    )
  }
  factory { GetApexPackageNamesUseCase(get()) }
  factory { BuildArchiveSnapshotItemUseCase(androidContext()) }
  factory { PrepareSnapshotComparisonArchivesUseCase(get()) }
  factory { BuildInstalledSnapshotItemUseCase(get()) }
  factory { BuildPackageComparisonSnapshotItemUseCase(androidContext().packageManager) }
  factory { BuildSnapshotAbiDisplayDataUseCase(androidContext()) }
  factory { BuildSnapshotCapturePlanUseCase(get()) }
  factory { BuildSnapshotComparisonListsUseCase(get()) }
  factory { BuildSnapshotComparisonPlanUseCase(get()) }
  factory { BuildSnapshotRestorePlanUseCase() }
  factory { BuildSnapshotListUpdatePlanUseCase(get(), get(), get()) }
  factory { BuildSnapshotUpdateTimeDisplayDataUseCase() }
  factory { BuildSnapshotPairDiffUseCase() }
  factory { CaptureInstalledSnapshotUseCase(androidContext().packageManager, get(), get(), get(), get(), get()) }
  factory { CompareSnapshotItemsUseCase() }
  factory { CompareSnapshotListsUseCase(get()) }
  factory { CompareTrackedSnapshotListsUseCase(get(), get()) }
  factory { CompareSnapshotWithInstalledAppsUseCase(androidContext().packageManager, get(), get(), get(), get()) }
  factory { CompareSnapshotDiffsUseCase(get(), get(), get(), get()) }
  factory { CompareSnapshotItemWithInstalledAppUseCase(androidContext().packageManager, get(), get(), get(), get()) }
  factory { FormatSnapshotTimestampUseCase() }
  factory { GetSnapshotDashboardCountUseCase(get(), get()) }
  factory { GetSnapshotPackageIconSourcesUseCase(get()) }
  factory { GetSnapshotRuleUseCase() }
  factory { GetSnapshotBackupTargetUseCase(get()) }
  factory { GetSnapshotSystemPropDiffsUseCase(get()) }
  factory { BuildSnapshotSystemPropDisplayDataUseCase(androidContext(), get()) }
  factory { BuildSnapshotTimeNodeItemsUseCase() }
  factory { GetTrackListItemsUseCase(androidContext().packageManager, get(), get()) }
  factory { SetPackageTrackedUseCase(get(), get()) }
  factory { DeleteSnapshotTimeStampUseCase(get(), get()) }
  factory { UpdateSnapshotAutoRemoveThresholdUseCase(get(), get()) }
  factory { UpdateSnapshotDiffItemsUseCase() }
  factory { UpdateSnapshotTopAppsUseCase(get(), get()) }
  factory { BuildSnapshotDetailItemsUseCase(androidContext()) }
  factory { BuildSnapshotDetailSectionsUseCase(get(), get()) }
  factory { SnapshotArchiveUseCase(get(), get()) }
  factory { BackupSnapshotArchiveToUriUseCase(androidContext().contentResolver, get()) }
  factory { (roomBackup: RoomBackup) ->
    CreateSnapshotDatabaseBackupUseCase(
      databaseBackupExporter = LocalSnapshotDatabaseBackupExporter(
        roomBackup = roomBackup,
        database = { LCDatabase.getDatabase() }
      )
    )
  }
  factory { PrepareRoomBackupRestoreFileUseCase(androidContext().contentResolver) }
  factory { (roomBackup: RoomBackup) ->
    RestoreSnapshotDatabaseBackupUseCase(
      prepareRoomBackupRestoreFile = get(),
      databaseBackupRestorer = LocalSnapshotDatabaseBackupRestorer(
        roomBackup = roomBackup,
        database = { LCDatabase.getDatabase() }
      ),
      onSuccessfulRestore = {
        Once.clearDone(OnceTag.FIRST_LAUNCH)
        ProcessPhoenix.triggerRebirth(LibCheckerApp.app)
      }
    )
  }
  factory { RestoreSnapshotArchiveFromUriUseCase(androidContext().contentResolver, get()) }
  factory { SnapshotLibraryUseCase(get()) }
  factory { SnapshotSelectionUseCase(get()) }
  factory {
    ChartDataProvider(
      buildAppListItemViewStatesUseCase = get(),
      buildAbiChartDataUseCase = get(),
      buildApiLevelChartDataUseCase = get(),
      buildDetailedAbiChartDataUseCase = get(),
      buildFeatureFlagChartDataUseCase = get(),
      buildPageSize16KBChartDataUseCase = get(),
      getAndroidDistributionUseCase = get(),
      chartSettingsRepository = get()
    )
  }
  factory { ChartDataSourceFactory(get()) }

  viewModel {
    ChartViewModel(
      appListRepository = get(),
      chartDataProvider = get(),
      chartDataSourceFactory = get(),
      chartSettingsRepository = get(),
      observeChartFeatureInitializationPlans = get()
    )
  }
  viewModel {
    DetailViewModel(
      detailActionLoader = get(),
      detailContentLoader = get(),
      detailFilterController = get(),
      detailFeatureLoader = get(),
      detailPackageLoader = get()
    )
  }
  viewModel {
    HomeViewModel(
      installedAppRepository = get(),
      appListRepository = get(),
      initializeAppListUseCase = get(),
      syncAppListChangesUseCase = get(),
      exportAppListToUriUseCase = get(),
      getAppListContentUseCase = get(),
      buildAppListUpdatePlanUseCase = get(),
      handleAppListSearchCommandUseCase = get(),
      appListSettingsRepository = get(),
      clearApkCacheUseCase = get(),
      appListItemsEquivalenceUseCase = get(),
      observeAppListLoadingUseCase = get()
    )
  }
  viewModel {
    LibReferenceViewModel(
      appListRepository = get(),
      buildAppListItemViewStatesUseCase = get(),
      getLibReferenceAppsUseCase = get(),
      buildLibReferenceDetailDialogRequestUseCase = get(),
      libReferenceSettingsRepository = get(),
      libReferenceComputationControllerFactory = get()
    )
  }
  viewModel {
    SettingsViewModel(
      appUpdateRepository = get(),
      appListSettingsRepository = get(),
      cloudRulesRepository = get(),
      ruleSettingsRepository = get(),
      snapshotSettingsRepository = get(),
      buildGetUpdatesItemsUseCase = get(),
      buildLocalePreferenceDataUseCase = get(),
      buildLogShareIntentUseCase = get(),
      exportInstalledAppsToUriUseCase = get(),
      selectDarkModeUseCase = get(),
      selectLocaleUseCase = get(),
      setApkAnalysisEnabledUseCase = get(),
      libReferenceSettingsRepository = get(),
      updateLibReferenceThresholdUseCase = get()
    )
  }
  viewModel {
    SnapshotViewModel(
      repository = get(),
      appListRepository = get(),
      compareSnapshotDiffs = get(),
      compareSnapshotItemWithInstalledApp = get(),
      getSnapshotDashboardCount = get(),
      buildSnapshotDetailItems = get(),
      buildSnapshotDetailSections = get(),
      snapshotLibrary = get(),
      buildSnapshotCapturePlanUseCase = get(),
      getSnapshotPackageIconSourcesUseCase = get(),
      buildSnapshotListUpdatePlanUseCase = get(),
      buildSnapshotSystemPropDisplayDataUseCase = get(),
      buildSnapshotTimeNodeItemsUseCase = get(),
      deleteSnapshotTimeStampUseCase = get(),
      formatSnapshotTimestampUseCase = get(),
      snapshotSelectionUseCase = get(),
      snapshotSettingsRepository = get(),
      updateSnapshotAutoRemoveThresholdUseCase = get(),
      updateSnapshotDiffItemsUseCase = get(),
      snapshotTrackChangeRepository = get()
    )
  }
  viewModel {
    SnapshotBackupViewModel(
      backupSnapshotArchiveToUriUseCase = get(),
      restoreSnapshotArchiveFromUriUseCase = get(),
      getSnapshotBackupTargetUseCase = get(),
      buildSnapshotRestorePlanUseCase = get(),
      createSnapshotDatabaseBackupUseCaseFactory = { roomBackup ->
        get {
          parametersOf(roomBackup)
        }
      },
      restoreSnapshotDatabaseBackupUseCaseFactory = { roomBackup ->
        get {
          parametersOf(roomBackup)
        }
      },
      formatSnapshotTimestampUseCase = get(),
      snapshotSelectionUseCase = get()
    )
  }
  viewModel {
    SnapshotComparisonViewModel(
      compareSnapshotDiffs = get(),
      getSnapshotDashboardCount = get(),
      snapshotLibrary = get(),
      buildSnapshotPairDiffUseCase = get(),
      buildSnapshotComparisonPlanUseCase = get(),
      formatSnapshotTimestampUseCase = get(),
      prepareSnapshotComparisonArchivesUseCase = get()
    )
  }
  viewModel { TrackViewModel(get(), get()) }
}
