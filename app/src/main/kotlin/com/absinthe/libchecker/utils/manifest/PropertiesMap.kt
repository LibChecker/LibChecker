package com.absinthe.libchecker.utils.manifest

object PropertiesMap {
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

  private val memtagModes = mapOf(
    -1 to "default",
    0 to "off",
    1 to "async",
    2 to "sync"
  )

  private val autoRevokePermissions = mapOf(
    0 to "allowed",
    1 to "discouraged",
    2 to "disallowed"
  )

  fun parseProperty(key: String, value: String): String {
    fun String.addOrigValue(): String {
      return "$this ($value)"
    }

    return when (key) {
      "appCategory" -> {
        category.getValue(value.toInt()).addOrigValue()
      }

      "installLocation" -> {
        installLocation.getValue(value.toInt()).addOrigValue()
      }

      "configChanges" -> {
        buildString {
          configs.forEach { (code, name) ->
            if (code and value.toInt() > 0) {
              append("|$name")
            }
          }
        }.substring(1).addOrigValue()
      }

      "screenOrientation" -> {
        screenOrientations.getValue(value.toInt()).addOrigValue()
      }

      "windowSoftInputMode" -> {
        buildString {
          var modes = value.toInt()
          windowSoftInputModes.forEach { (code, name) ->
            if (modes - code >= 0) {
              append("|$name")
              modes -= code
            }
          }
          if (this.isEmpty()) {
            append("|stateUnspecified|adjustUnspecified")
          }
        }.substring(1).addOrigValue()
      }

      "gwpAsanMode" -> {
        gwpAsanModes.getValue(value.toInt()).addOrigValue()
      }

      "uiOptions" -> {
        uiOptions.getValue(value.toInt()).addOrigValue()
      }

      "launchMode" -> {
        launchModes.getValue(value.toInt()).addOrigValue()
      }

      "memtagMode" -> {
        memtagModes.getValue(value.toInt()).addOrigValue()
      }

      "autoRevokePermissions" -> {
        autoRevokePermissions.getValue(value.toInt()).addOrigValue()
      }

      else -> value
    }
  }
}
