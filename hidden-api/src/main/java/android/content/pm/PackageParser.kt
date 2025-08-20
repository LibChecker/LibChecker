package android.content.pm

import android.content.IntentFilter
import java.io.File

class PackageParser {

  @Throws(PackageParserException::class)
  fun parsePackage(packageFile: File, flags: Int): Package {
    throw RuntimeException("Stub")
  }

  class PackageParserException : Exception()

  abstract class Component<II : IntentInfo> {
    val intents: ArrayList<II>? = null
    val className: String? = null
  }

  abstract class IntentInfo : IntentFilter()

  class Package {
    val activities: ArrayList<Activity> = ArrayList(0)
    val receivers: ArrayList<Activity> = ArrayList(0)
    val services: ArrayList<Service> = ArrayList(0)
  }

  class Activity : Component<ActivityIntentInfo>()

  class ActivityIntentInfo : IntentInfo()

  class Service : Component<ServiceIntentInfo>()

  class ServiceIntentInfo : IntentInfo()
}