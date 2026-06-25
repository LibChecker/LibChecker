package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import com.absinthe.rulesbundle.Rule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetSnapshotRuleUseCase {
  suspend operator fun invoke(item: SnapshotDetailItem): Rule? = withContext(Dispatchers.IO) {
    RulesRepository.getRule(item.name, item.itemType, true)
  }
}
