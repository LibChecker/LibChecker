package com.absinthe.libchecker.domain.statistics.reference.usecase

import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.isComponentType
import com.absinthe.libchecker.database.RulesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BuildLibReferenceDetailDialogRequestUseCase {

  suspend operator fun invoke(
    name: String,
    @LibType type: Int
  ): LibReferenceDetailDialogRequest? = withContext(Dispatchers.IO) {
    if (!canOpenDetail(type)) {
      return@withContext null
    }

    LibReferenceDetailDialogRequest(
      name = name,
      type = type,
      regexName = RulesRepository.getRule(name, type, true)?.regexName
    )
  }

  private fun canOpenDetail(@LibType type: Int): Boolean {
    return type == NATIVE || isComponentType(type) || type == ACTION
  }
}

data class LibReferenceDetailDialogRequest(
  val name: String,
  @LibType val type: Int,
  val regexName: String?
)
