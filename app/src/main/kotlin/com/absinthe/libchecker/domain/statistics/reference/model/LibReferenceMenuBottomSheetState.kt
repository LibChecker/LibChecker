package com.absinthe.libchecker.domain.statistics.reference.model

import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.ui.app.MenuOptionItem

data class LibReferenceMenuBottomSheetState(
  val options: List<MenuOptionItem>
)

sealed interface LibReferenceMenuAction {
  data class OptionChanged(
    val item: MenuOptionItem,
    val isChecked: Boolean
  ) : LibReferenceMenuAction
}

fun buildLibReferenceMenuBottomSheetState(
  currentOptions: Int
): LibReferenceMenuBottomSheetState {
  return LibReferenceMenuBottomSheetState(
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
