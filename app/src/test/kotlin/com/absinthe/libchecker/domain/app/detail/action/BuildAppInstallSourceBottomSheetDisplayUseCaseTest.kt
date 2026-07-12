package com.absinthe.libchecker.domain.app.detail.action

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceAction
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceItemContent
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceRequesterAccess
import com.absinthe.libchecker.domain.app.detail.related.RelatedAppDisplayData
import com.absinthe.libchecker.domain.app.model.AppInstallSource
import com.absinthe.libchecker.utils.extensions.DexFileOptimizationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class BuildAppInstallSourceBottomSheetDisplayUseCaseTest {

  @Test
  fun `builds related requester and unknown installer rows`() {
    val item = testItem("requester.package")
    val relatedApp = RelatedAppDisplayData(
      item = item,
      packageInfo = null,
      packageName = item.packageName,
      label = "Requester app",
      versionInfo = "1.0 (1)",
      abiInfo = "arm64-v8a",
      abiBadgeRes = null,
      isHarmony = false
    )
    val installedTime = AppInstalledTimeDisplayData("First", "Last")
    val dexoptInfo = DexFileOptimizationInfo(status = "speed", reason = "install")

    val display = buildDisplay(
      details = AppInstallSourceDetails(
        installSource = AppInstallSource(
          initiatingPackageName = null,
          originatingPackageName = item.packageName,
          installingPackageName = null
        ),
        installedTime = installedTime,
        dexoptInfo = dexoptInfo
      ),
      originatingApp = relatedApp,
      requesterAccess = AppInstallSourceRequesterAccess.Available
    )

    val requester = checkNotNull(display.originatingApp)
    assertEquals(AppInstallSourceItemContent.RelatedApp(relatedApp), requester.content)
    assertEquals(AppInstallSourceAction.OpenApp(item), requester.action)
    assertEquals(
      "Requester, Requester app, requester.package, 1.0 (1), arm64-v8a",
      requester.contentDescription
    )

    val installer = checkNotNull(display.installingApp)
    val installerMessage = installer.content as AppInstallSourceItemContent.Message
    assertEquals("Unknown", installerMessage.appName)
    assertEquals("Shell or removed app", installerMessage.packageName)
    assertEquals(true, installerMessage.showAbiInfo)
    assertNull(installer.action)
    assertSame(installedTime, display.installedTime)
    assertSame(dexoptInfo, display.dexoptInfo)
  }

  @Test
  fun `maps every unavailable requester state to its prompt action`() {
    val expectedActions = mapOf(
      AppInstallSourceRequesterAccess.ShizukuNotInstalled to
        AppInstallSourceAction.OpenShizukuReleasePage,
      AppInstallSourceRequesterAccess.ShizukuNotRunning to
        AppInstallSourceAction.LaunchShizuku,
      AppInstallSourceRequesterAccess.ShizukuLowVersion to
        AppInstallSourceAction.OpenShizukuReleasePage,
      AppInstallSourceRequesterAccess.ShizukuPermissionDenied to
        AppInstallSourceAction.RequestShizukuPermission
    )

    expectedActions.forEach { (access, expectedAction) ->
      val display = buildDisplay(
        details = AppInstallSourceDetails(
          installSource = AppInstallSource(
            initiatingPackageName = null,
            originatingPackageName = "requester.package",
            installingPackageName = "installer.package"
          ),
          installedTime = null,
          dexoptInfo = null
        ),
        requesterAccess = access
      )

      val requester = checkNotNull(display.originatingApp)
      val message = requester.content as AppInstallSourceItemContent.Message
      assertEquals("Requester $access", message.appName)
      assertEquals("Shizuku usage", message.packageName)
      assertEquals("Detail $access", message.versionInfo)
      assertEquals(false, message.showAbiInfo)
      assertEquals(expectedAction, requester.action)
    }
  }

  @Test
  fun `hides package rows when source data is unavailable`() {
    val display = buildDisplay(
      details = AppInstallSourceDetails(
        installSource = AppInstallSource(
          initiatingPackageName = null,
          originatingPackageName = "requester.package",
          installingPackageName = "installer.package"
        ),
        installedTime = null,
        dexoptInfo = null
      ),
      requesterAccess = AppInstallSourceRequesterAccess.Available
    )

    assertNull(display.originatingApp)
    assertNull(display.installingApp)
  }

  @Test
  fun `hides requester and installer when install source is unavailable`() {
    val display = buildDisplay(
      details = AppInstallSourceDetails(
        installSource = null,
        installedTime = null,
        dexoptInfo = null
      ),
      requesterAccess = AppInstallSourceRequesterAccess.ShizukuNotRunning
    )

    assertNull(display.originatingApp)
    assertNull(display.installingApp)
  }

  private fun buildDisplay(
    details: AppInstallSourceDetails,
    originatingApp: RelatedAppDisplayData? = null,
    installingApp: RelatedAppDisplayData? = null,
    requesterAccess: AppInstallSourceRequesterAccess
  ) = buildAppInstallSourceBottomSheetDisplay(
    request = BuildAppInstallSourceBottomSheetDisplayUseCase.Request(
      details = details,
      originatingApp = originatingApp,
      installingApp = installingApp,
      requesterAccess = requesterAccess
    ),
    strings = strings
  )

  private fun testItem(packageName: String): LCItem {
    return LCItem(
      packageName = packageName,
      label = "Requester app",
      versionName = "1.0",
      versionCode = 1,
      installedTime = 0,
      lastUpdatedTime = 0,
      isSystem = false,
      abi = 0,
      features = 0,
      targetApi = 35,
      variant = 0
    )
  }

  private companion object {
    val promptAccesses = listOf(
      AppInstallSourceRequesterAccess.ShizukuNotInstalled,
      AppInstallSourceRequesterAccess.ShizukuNotRunning,
      AppInstallSourceRequesterAccess.ShizukuLowVersion,
      AppInstallSourceRequesterAccess.ShizukuPermissionDenied
    )

    val strings = AppInstallSourceDisplayStrings(
      originatingTitle = "Requester",
      installingTitle = "Installer",
      unknownAppName = "Unknown",
      unknownPackageDescription = "Shell or removed app",
      shizukuUsage = "Shizuku usage",
      shizukuPrompts = promptAccesses.associateWith {
        ShizukuPromptStrings(
          appName = "Requester $it",
          detail = "Detail $it"
        )
      }
    )
  }
}
