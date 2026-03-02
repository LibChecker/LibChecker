package ohos.bundle

object AbilityInfo {
  enum class AbilityType {
    DATA,
    PAGE,
    SERVICE,
    UNKNOWN,
    WEB
  }

  var type = AbilityType.PAGE
  var bundleName = ""
  var className = ""
  var enabled = false
  var process = ""
}
