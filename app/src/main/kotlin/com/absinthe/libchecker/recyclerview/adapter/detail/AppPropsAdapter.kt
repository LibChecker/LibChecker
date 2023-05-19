package com.absinthe.libchecker.recyclerview.adapter.detail

import android.content.pm.PackageInfo
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.model.AppPropItem
import com.absinthe.libchecker.ui.fragment.detail.EXTRA_TEXT
import com.absinthe.libchecker.ui.fragment.detail.XmlBSDFragment
import com.absinthe.libchecker.utils.extensions.maybeResourceId
import com.absinthe.libchecker.utils.manifest.ResourceParser
import com.absinthe.libchecker.view.detail.AppPropItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import timber.log.Timber

class AppPropsAdapter(
  packageInfo: PackageInfo,
  private val fragMgr: FragmentManager
) : BaseQuickAdapter<AppPropItem, BaseViewHolder>(0) {

  private val appResources by lazy {
    SystemServices.packageManager.getResourcesForApplication(
      packageInfo.applicationInfo
    )
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(AppPropItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: AppPropItem) {
    (holder.itemView as AppPropItemView).apply {
      // TODO
      // setTipText("TODO")
      key.text = item.key
      value.text = parseValue(item)
      initLinkBtn(this, item)
    }
  }

  private val category = mapOf(
    0 to "game",
    1 to "audio",
    2 to "video",
    3 to "image",
    4 to "social",
    5 to "news",
    6 to "maps",
    7 to "productivity",
    8 to "accessibility"
  )

  private val installLocation = mapOf(
    0 to "auto",
    1 to "internalOnly",
    2 to "preferExternal"
  )

  private val configs = mapOf(
    0x0001 to "mcc",
    0x0002 to "mnc",
    0x0004 to "locale",
    0x0008 to "touchscreen",
    0x0010 to "keyboard",
    0x0020 to "keyboardHidden",
    0x0040 to "navigation",
    0x0080 to "orientation",
    0x0100 to "screenLayout",
    0x0200 to "uiMode",
    0x0400 to "screenSize",
    0x0800 to "smallestScreenSize",
    0x1000 to "density",
    0x2000 to "layoutDirection",
    0x4000 to "colorMode",
    0x8000 to "grammaticalGender",
    0x10000000 to "fontWeightAdjustment",
    0x40000000 to "fontScale"
  )

  private val screenOrientations = mapOf(
    -1 to "unspecified",
    0 to "landscape",
    1 to "portrait",
    2 to "user",
    3 to "behind",
    4 to "sensor",
    5 to "nosensor",
    6 to "sensorLandscape",
    7 to "sensorPortrait",
    8 to "reverseLandscape",
    9 to "reversePortrait",
    10 to "fullSensor",
    11 to "userLandscape",
    12 to "userPortrait",
    13 to "fullUser",
    14 to "locked"
  )

  private val windowSoftInputModes = mapOf(
    0x30 to "adjustNothing",
    0x20 to "adjustPan",
    0x10 to "adjustResize",
    5 to "stateAlwaysVisible",
    4 to "stateVisible",
    3 to "stateAlwaysHidden",
    2 to "stateHidden",
    1 to "stateUnchanged"
  )

  private val gwpAsanModes = mapOf(
    -1 to "default",
    0 to "never",
    1 to "always"
  )

  private val uiOptions = mapOf(
    0 to "none",
    1 to "splitActionBarWhenNarrow"
  )

  private val launchModes = mapOf(
    0 to "standard",
    1 to "singleTop",
    2 to "singleTask",
    3 to "singleInstance",
    4 to "singleInstance"
  )

  private val linkable = setOf("string", "array", "bool", "xml", "drawable", "mipmap", "color", "dimen")

  private fun parseValue(item: AppPropItem): String {
    return when {
      item.key == "appCategory" -> {
        category.getValue(item.value.toInt())
      }
      item.key == "installLocation" -> {
        installLocation.getValue(item.value.toInt())
      }
      item.key == "configChanges" -> {
        buildString {
          configs.forEach { (code, name) ->
            if (code and item.value.toInt() > 0) {
              append("|$name")
            }
          }
        }.substring(1)
      }
      item.key == "screenOrientation" -> {
        screenOrientations.getValue(item.value.toInt())
      }
      item.key == "windowSoftInputMode" -> {
        buildString {
          var value = item.value.toInt()
          windowSoftInputModes.forEach { (code, name) ->
            if (value - code >= 0) {
              append("|$name")
              value -= code
            }
          }
          if (this.isEmpty()) {
            append("|stateUnspecified|adjustUnspecified")
          }
        }.substring(1)
      }
      item.key == "gwpAsanMode" -> {
        gwpAsanModes.getValue(item.value.toInt())
      }
      item.key == "uiOptions" -> {
        uiOptions.getValue(item.value.toInt())
      }
      item.key == "launchMode" -> {
        launchModes.getValue(item.value.toInt())
      }
      item.value.maybeResourceId() -> {
        runCatching {
          appResources.getResourceName(item.value.toInt())
        }.getOrDefault(item.value)
      }
      else -> item.value
    }
  }

  private fun initLinkBtn(itemView: AppPropItemView, item: AppPropItem) {
    if (!item.value.maybeResourceId()) return

    val type = runCatching {
      appResources.getResourceTypeName(item.value.toInt())
    }.getOrDefault("null")
    itemView.linkToIcon.isVisible = linkable.contains(type)

    if (itemView.linkToIcon.isVisible) {
      itemView.linkToIcon.setOnClickListener {
        val transformed = itemView.linkToIcon.getTag(R.id.resource_transformed_id) as? Boolean ?: false
        if (transformed) {
          itemView.value.text = parseValue(item)
          itemView.linkToIcon.setImageResource(R.drawable.ic_outline_change_circle_24)
          itemView.linkToIcon.setTag(R.id.resource_transformed_id, false)
        } else {
          var clickedTag = false
          Timber.d("type: $type")
          runCatching {
            when (type) {
              "string" -> {
                itemView.value.text = appResources.getString(item.value.toInt())
                clickedTag = true
              }
              "array" -> {
                itemView.value.text =
                  appResources.getStringArray(item.value.toInt()).contentToString()
                clickedTag = true
              }
              "bool" -> {
                itemView.value.text = appResources.getBoolean(item.value.toInt()).toString()
                clickedTag = true
              }
              "xml" -> {
                appResources.getXml(item.value.toInt()).let {
                  val text = ResourceParser(it).setMarkColor(true).parse()
                  XmlBSDFragment().apply {
                    arguments = bundleOf(
                      EXTRA_TEXT to text
                    )
                    show(fragMgr, XmlBSDFragment::class.java.name)
                  }
                }
                clickedTag = false
              }
              "drawable", "mipmap" -> {
                itemView.linkToIcon.setImageDrawable(
                  appResources.getDrawable(
                    item.value.toInt(),
                    null
                  )
                )
                clickedTag = true
              }
              "color" -> {
                itemView.linkToIcon.setImageDrawable(
                  ColorDrawable(
                    appResources.getColor(
                      item.value.toInt(),
                      null
                    )
                  )
                )
                clickedTag = true
              }
              "dimen" -> {
                itemView.value.text = appResources.getDimension(item.value.toInt()).toString()
                clickedTag = true
              }
              else -> {
                clickedTag = false
              }
            }
          }
          itemView.linkToIcon.setTag(R.id.resource_transformed_id, clickedTag)
        }
      }
    }
  }
}
