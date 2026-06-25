package com.absinthe.libchecker.domain.app.detail.content

import android.content.Context
import com.absinthe.libchecker.constant.AbilityType
import com.absinthe.libchecker.features.statistics.bean.DISABLED
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import ohos.bundle.AbilityInfo
import ohos.bundle.IBundleManager

class GetAppDetailAbilityChipsUseCase(
  private val context: Context
) {

  operator fun invoke(packageName: String): Map<Int, List<LibStringItemChip>> {
    val abilities = ApplicationDelegate(context).iBundleManager?.getBundleInfo(
      packageName,
      IBundleManager.GET_BUNDLE_WITH_ABILITIES
    )?.abilityInfos ?: return emptyMap()

    return mapOf(
      AbilityType.PAGE to abilities.asSequence().toChips(AbilityInfo.AbilityType.PAGE),
      AbilityType.SERVICE to abilities.asSequence().toChips(AbilityInfo.AbilityType.SERVICE),
      AbilityType.WEB to abilities.asSequence().toChips(AbilityInfo.AbilityType.WEB),
      AbilityType.DATA to abilities.asSequence().toChips(AbilityInfo.AbilityType.DATA)
    )
  }

  private fun Sequence<AbilityInfo>.toChips(
    abilityType: AbilityInfo.AbilityType
  ): List<LibStringItemChip> {
    return filter { it.type == abilityType }
      .map { ability ->
        LibStringItemChip(
          LibStringItem(
            name = ability.className,
            source = DISABLED.takeIf { !ability.enabled }
          ),
          null
        )
      }
      .toList()
  }
}
