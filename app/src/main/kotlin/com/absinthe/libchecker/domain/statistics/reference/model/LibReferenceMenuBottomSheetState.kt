package com.absinthe.libchecker.domain.statistics.reference.model

import android.content.pm.PackageInfo
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.ACTION_IN_RULES
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.SHARED_UID
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.ui.app.MenuOptionItem
import com.absinthe.rulesbundle.Rule

data class LibReferenceMenuBottomSheetState(
  val demoItems: List<LibReference>,
  val colorfulRuleIcon: Boolean,
  val options: List<MenuOptionItem>
)

sealed interface LibReferenceMenuAction {
  data class OptionChanged(
    val item: MenuOptionItem,
    val isChecked: Boolean
  ) : LibReferenceMenuAction
}

fun buildLibReferenceMenuBottomSheetState(
  currentOptions: Int,
  colorfulRuleIcon: Boolean = true,
  demoIconPackages: List<PackageInfo> = emptyList()
): LibReferenceMenuBottomSheetState {
  return LibReferenceMenuBottomSheetState(
    demoItems = buildLibReferenceDemoItems(currentOptions, demoIconPackages),
    colorfulRuleIcon = colorfulRuleIcon,
    options = listOf(
      MenuOptionItem(R.string.ref_category_native, LibReferenceOptions.NATIVE_LIBS, currentOptions),
      MenuOptionItem(R.string.ref_category_service, LibReferenceOptions.SERVICES, currentOptions),
      MenuOptionItem(R.string.ref_category_activity, LibReferenceOptions.ACTIVITIES, currentOptions),
      MenuOptionItem(R.string.ref_category_br, LibReferenceOptions.RECEIVERS, currentOptions),
      MenuOptionItem(R.string.ref_category_cp, LibReferenceOptions.PROVIDERS, currentOptions),
      MenuOptionItem(R.string.ref_category_action, LibReferenceOptions.ACTION, currentOptions),
      MenuOptionItem(R.string.ref_category_perm, LibReferenceOptions.PERMISSIONS, currentOptions),
      MenuOptionItem(R.string.ref_category_metadata, LibReferenceOptions.METADATA, currentOptions),
      MenuOptionItem(R.string.ref_category_package, LibReferenceOptions.PACKAGES, currentOptions),
      MenuOptionItem(R.string.ref_category_shared_uid, LibReferenceOptions.SHARED_UID, currentOptions),
      MenuOptionItem(
        R.string.ref_category_only_not_marked,
        LibReferenceOptions.ONLY_NOT_MARKED,
        currentOptions
      )
    )
  )
}

private fun buildLibReferenceDemoItems(
  currentOptions: Int,
  demoIconPackages: List<PackageInfo>
): List<LibReference> = buildList {
  if (currentOptions and LibReferenceOptions.NATIVE_LIBS > 0) {
    add(
      markedDemoReference(
        libName = "libflutter.so",
        label = "Flutter",
        type = NATIVE,
        iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_flutter,
        count = 13
      )
    )
  }
  if (currentOptions and LibReferenceOptions.SERVICES > 0) {
    add(
      markedDemoReference(
        libName = "com.google.firebase.messaging.FirebaseMessagingService",
        label = "FCM",
        type = SERVICE,
        iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_firebase,
        count = 11
      )
    )
  }
  if (currentOptions and LibReferenceOptions.ACTIVITIES > 0) {
    add(
      markedDemoReference(
        libName = "com.google.android.gms.ads.AdActivity",
        label = "Google AdMob",
        type = ACTIVITY,
        iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_google,
        count = 7
      )
    )
  }
  if (currentOptions and LibReferenceOptions.RECEIVERS > 0) {
    add(
      markedDemoReference(
        libName = "com.google.firebase.iid.FirebaseInstanceIdReceiver",
        label = "Firebase",
        type = RECEIVER,
        iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_firebase,
        count = 9
      )
    )
  }
  if (currentOptions and LibReferenceOptions.PROVIDERS > 0) {
    add(
      markedDemoReference(
        libName = "rikka.shizuku.ShizukuProvider",
        label = "Shizuku",
        type = PROVIDER,
        iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_shizuku,
        count = 5
      )
    )
  }
  if (currentOptions and LibReferenceOptions.ACTION > 0) {
    add(
      markedDemoReference(
        libName = "com.xiaomi.mipush.RECEIVE_MESSAGE",
        label = "MiPush",
        type = ACTION,
        ruleType = ACTION_IN_RULES,
        iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_xiaomi,
        count = 8
      )
    )
  }
  if (currentOptions and LibReferenceOptions.PERMISSIONS > 0) {
    add(
      unmarkedDemoReference(
        libName = "android.permission.CAMERA",
        type = PERMISSION,
        count = 17
      )
    )
  }
  if (currentOptions and LibReferenceOptions.METADATA > 0) {
    add(
      unmarkedDemoReference(
        libName = "com.google.android.gms.version",
        type = METADATA,
        count = 15
      )
    )
  }
  if (currentOptions and LibReferenceOptions.PACKAGES > 0) {
    add(
      unmarkedDemoReference(
        libName = "com.google",
        type = PACKAGE,
        count = 12,
        iconPackages = demoIconPackages
      )
    )
  }
  if (currentOptions and LibReferenceOptions.SHARED_UID > 0) {
    add(
      unmarkedDemoReference(
        libName = "android.uid.system",
        type = SHARED_UID,
        count = 6,
        iconPackages = demoIconPackages
      )
    )
  }
}

private fun markedDemoReference(
  libName: String,
  label: String,
  type: Int,
  iconRes: Int,
  count: Int,
  ruleType: Int = type
): LibReference {
  return LibReference(
    libName = libName,
    rule = Rule(libName, ruleType, label, iconRes, null, null, false),
    referredList = demoReferredPackages(count),
    type = type
  )
}

private fun unmarkedDemoReference(
  libName: String,
  type: Int,
  count: Int,
  iconPackages: List<PackageInfo> = emptyList()
): LibReference {
  return LibReference(
    libName = libName,
    rule = null,
    referredList = demoReferredPackages(count),
    type = type,
    iconPackages = iconPackages
  )
}

private fun demoReferredPackages(count: Int): Set<String> {
  return List(count) { index -> "demo.package.$index" }.toSet()
}
