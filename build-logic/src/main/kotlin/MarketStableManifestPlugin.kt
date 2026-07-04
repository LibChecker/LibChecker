import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.w3c.dom.Element

private const val ANDROID_MANIFEST_NAMESPACE = "http://schemas.android.com/apk/res/android"
private const val SELF_UPDATE_PERMISSION = "android.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION"
private const val SELF_UPDATE_RECEIVER = ".data.app.update.AppUpdateInstallResultReceiver"

class MarketStableManifestPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    project.plugins.withId("com.android.application") {
      val isDevVersion = project.providers.exec {
        commandLine("git", "tag", "-l", baseVersionName)
      }.standardOutput.asText.get().trim().isEmpty()

      project.tasks.configureEach {
        if (name.startsWith("processMarket") && name.endsWith("MainManifest")) {
          inputs.property("selfUpdateManifestMode", if (isDevVersion) "keep" else "remove")
          if (!isDevVersion) {
            val variantName = name
              .removePrefix("process")
              .removeSuffix("MainManifest")
              .replaceFirstChar { it.lowercaseChar() }
            val manifestFile = project.layout.buildDirectory
              .file("intermediates/merged_manifest/$variantName/$name/AndroidManifest.xml")
            doLast {
              manifestFile.get().asFile.removeSelfUpdateEntries()
            }
          }
        }
      }
    }
  }
}

private fun File.removeSelfUpdateEntries() {
  if (!isFile) {
    return
  }

  val document = DocumentBuilderFactory.newInstance().apply {
    isNamespaceAware = true
  }.newDocumentBuilder().parse(this)

  val permissions = document.getElementsByTagName("uses-permission")
  for (index in permissions.length - 1 downTo 0) {
    val element = permissions.item(index) as? Element ?: continue
    if (element.getAttributeNS(ANDROID_MANIFEST_NAMESPACE, "name") == SELF_UPDATE_PERMISSION) {
      element.parentNode.removeChild(element)
    }
  }

  val receivers = document.getElementsByTagName("receiver")
  for (index in receivers.length - 1 downTo 0) {
    val element = receivers.item(index) as? Element ?: continue
    val receiverName = element.getAttributeNS(ANDROID_MANIFEST_NAMESPACE, "name")
    if (receiverName.endsWith(SELF_UPDATE_RECEIVER)) {
      element.parentNode.removeChild(element)
    }
  }

  TransformerFactory.newInstance().newTransformer().apply {
    setOutputProperty(OutputKeys.INDENT, "yes")
    setOutputProperty(OutputKeys.ENCODING, "utf-8")
  }.transform(DOMSource(document), StreamResult(this))
}
