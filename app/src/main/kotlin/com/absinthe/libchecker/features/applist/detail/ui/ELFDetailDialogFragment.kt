package com.absinthe.libchecker.features.applist.detail.ui

import android.content.DialogInterface
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.features.applist.detail.ui.view.ELFInfoBottomSheetView
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.elf.ElfParser
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipFile

const val EXTRA_ELF_PATH = "EXTRA_ELF_PATH"
const val EXTRA_RULE_ICON = "EXTRA_RULE_ICON"

class ELFDetailDialogFragment : BaseBottomSheetViewDialogFragment<ELFInfoBottomSheetView>() {

  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME).orEmpty() }
  private val elfPath by lazy { arguments?.getString(EXTRA_ELF_PATH).orEmpty() }
  private val ruleIcon by lazy {
    arguments?.getInt(EXTRA_RULE_ICON) ?: com.absinthe.lc.rulesbundle.R.drawable.ic_sdk_placeholder
  }

  override fun initRootView(): ELFInfoBottomSheetView = ELFInfoBottomSheetView(requireContext())

  override fun init() {
    root.apply {
      maxPeekHeightPercentage = 0.67f
      title.text = elfPath.split("/").last()
      lifecycleScope.launch {
        icon.load(ruleIcon) {
          crossfade(true)
        }
        setContent(getString(R.string.loading), getString(R.string.loading))
      }
    }
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun onStart() {
    super.onStart()
    lifecycleScope.launch(Dispatchers.IO) {
      val info = getInfo() ?: return@launch
      withContext(Dispatchers.Main) {
        root.apply {
          setContent(
            info.deps.joinToString(", "),
            info.entryPoints.joinToString(System.lineSeparator()) { it -> "◉$it" }
          )
        }
      }
    }
  }

  override fun show(manager: FragmentManager, tag: String?) {
    if (!isShowing) {
      isShowing = true
      super.show(manager, tag)
    }
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    isShowing = false
  }

  private fun getInfo(): Info? {
    val pi = runCatching { PackageUtils.getPackageInfo(packageName) }.getOrNull() ?: return null
    val nativePath = pi.applicationInfo?.nativeLibraryDir
    if (nativePath != null) {
      File(nativePath).listFiles()
        ?.find { it.path.endsWith(elfPath) }
        ?.let {
          val parser = ElfParser(it)
          return Info(
            deps = parser.parseNeededDependencies(),
            entryPoints = parser.parseEntryPoints()
          ).also {
            parser.close()
          }
        }
    }
    val sourceDir = pi.applicationInfo?.sourceDir ?: return null
    val apkFile = File(sourceDir)
    var info = getInfoFromApk(apkFile)
    if (info != null) return info

    PackageUtils.getSplitsSourceDir(pi)?.forEach { split ->
      info = getInfoFromApk(File(split))
      if (info != null) return info
    }
    return null
  }

  private fun getInfoFromApk(apk: File): Info? {
    ZipFile.Builder().setFile(apk).get().use { zipFile ->
      zipFile.entries
        .asSequence()
        .find { it.name == elfPath }
        ?.let {
          val parser = ElfParser(zipFile.getInputStream(it))
          return Info(
            deps = parser.parseNeededDependencies(),
            entryPoints = parser.parseEntryPoints()
          ).also {
            parser.close()
          }
        }
    }
    return null
  }

  companion object {
    fun newInstance(packageName: String, elfPath: String, ruleIcon: Int): ELFDetailDialogFragment {
      return ELFDetailDialogFragment().putArguments(
        EXTRA_PACKAGE_NAME to packageName,
        EXTRA_ELF_PATH to elfPath,
        EXTRA_RULE_ICON to ruleIcon
      )
    }

    var isShowing = false
  }

  data class Info(
    val deps: List<String>,
    val entryPoints: List<String>
  )
}
