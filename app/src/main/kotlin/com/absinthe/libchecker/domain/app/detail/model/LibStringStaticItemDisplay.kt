package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.rulesbundle.Rule

data class LibStringStaticItemDisplay(
  override val name: LibStringItemNameDisplay,
  val detail: Detail?,
  val rule: Rule?,
  override val contentDescription: String
) : LibStringItemDisplay {

  data class Detail(
    val path: String,
    val versionCode: Int,
    val certificateDigest: String
  ) {
    val plainText: String = buildString {
      append("[Path] ").append(path).appendLine()
      append("[Version Code] ").append(versionCode).appendLine()
      append("[Cert] ").append(certificateDigest)
    }
  }

  companion object {
    fun create(
      item: LibStringItemChip,
      itemDisplayOptions: Int
    ): LibStringStaticItemDisplay {
      val name = LibStringItemNameDisplay.create(
        item = item.item,
        type = STATIC,
        itemDisplayOptions = itemDisplayOptions
      )
      val detail = item.item.source
        ?.fromJson<StaticLibItem>()
        ?.let {
          Detail(
            path = it.path,
            versionCode = it.version,
            certificateDigest = it.certDigest
          )
        }
      val rule = item.rule.takeIf {
        itemDisplayOptions.isOptionEnabled(AdvancedOptions.SHOW_MARKED_LIB)
      }

      return LibStringStaticItemDisplay(
        name = name,
        detail = detail,
        rule = rule,
        contentDescription = buildLibStringItemDescription(
          name.text,
          detail?.plainText,
          rule?.label
        )
      )
    }
  }
}

private fun Int.isOptionEnabled(option: Int): Boolean {
  return (this and option) > 0
}
