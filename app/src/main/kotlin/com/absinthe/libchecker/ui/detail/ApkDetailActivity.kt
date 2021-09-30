package com.absinthe.libchecker.ui.detail

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.text.style.StrikethroughSpan
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.databinding.ActivityAppDetailBinding
import com.absinthe.libchecker.ui.fragment.detail.DetailFragmentManager
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_LIB
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.ui.fragment.detail.impl.ComponentsAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.DexAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.MetaDataAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.NativeAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.PermissionAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.StaticAnalysisFragment
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.isOrientationPortrait
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.utils.manifest.ManifestReader
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.absinthe.libchecker.viewmodel.DetailViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.AppIconLoader
import timber.log.Timber
import java.io.File
import java.io.InputStream

class ApkDetailActivity : BaseActivity<ActivityAppDetailBinding>(), IDetailContainer {

  private var tempFile: File? = null

  private val viewModel: DetailViewModel by viewModels()

  override var detailFragmentManager: DetailFragmentManager = DetailFragmentManager()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewModel.isApk = true
    intent.data?.scheme?.let { scheme ->
      if (scheme == "content") {
        intent.data?.let {
          initView(it)
        }
      }
    }
  }

  override fun onDestroy() {
    tempFile?.delete()
    super.onDestroy()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
    }
    return super.onOptionsItemSelected(item)
  }

  private fun initView(uri: Uri) {
    setSupportActionBar(binding.toolbar)
    supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setDisplayShowHomeEnabled(true)
    }

    initData(uri)
  }

  @SuppressLint("SetTextI18n")
  private fun initData(uri: Uri) {
    tempFile = File(externalCacheDir, "temp.apk")

    val path = tempFile!!.path
    var inputStream: InputStream? = null
    var packageInfo: PackageInfo? = null

    try {
      inputStream = contentResolver.openInputStream(uri)

      FileUtils.writeFileFromIS(tempFile!!, inputStream)

      packageInfo = packageManager.getPackageArchiveInfo(path, 0)
      packageInfo?.let {
        it.applicationInfo.sourceDir = path
        it.applicationInfo.publicSourceDir = path

        supportActionBar?.apply {
          title = it.applicationInfo.loadLabel(packageManager)
        }
        binding.apply {
          try {
            val appIconLoader = AppIconLoader(
              resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size),
              false,
              this@ApkDetailActivity
            )
            detailsTitle.apply {
              iconView.load(appIconLoader.loadIcon(it.applicationInfo))
              appNameView.apply {
                text = it.applicationInfo.loadLabel(packageManager)
                setLongClickCopiedToClipboard(text)
              }
              packageNameView.apply {
                text = packageInfo.packageName
                setLongClickCopiedToClipboard(text)
              }
              versionInfoView.apply {
                text = PackageUtils.getVersionString(it)
                setLongClickCopiedToClipboard(text)
              }
            }

            val extraInfo = SpannableStringBuilder()
            val file = File(packageInfo.applicationInfo.sourceDir)
            val demands = ManifestReader.getManifestProperties(
              file,
              arrayOf(
                PackageUtils.use32bitAbiString,
                PackageUtils.multiArchString,
                PackageUtils.overlayString
              )
            )
            val overlay = demands[PackageUtils.overlayString] as? Boolean ?: false
            val abiSet = PackageUtils.getAbiSet(
              file,
              packageInfo.applicationInfo,
              isApk = true,
              overlay = overlay,
              ignoreArch = true
            )
            val abi = PackageUtils.getAbi(abiSet, demands)

            if (abiSet.isNotEmpty() && !abiSet.contains(Constants.OVERLAY) && !abiSet.contains(
                Constants.ERROR
              )
            ) {
              val spanStringBuilder = SpannableStringBuilder()
              var spanString: SpannableString
              var firstLoop = true
              var itemCount = 0
              abiSet.forEach { eachAbi ->
                itemCount++
                if (firstLoop) {
                  firstLoop = false
                }
                PackageUtils.getAbiString(
                  this@ApkDetailActivity,
                  eachAbi,
                  false
                ).let { str ->
                  spanString = SpannableString("  $str")
                }
                PackageUtils.getAbiBadgeResource(eachAbi).getDrawable(this@ApkDetailActivity)
                  ?.mutate()?.let { drawable ->
                    drawable.setBounds(
                      0,
                      0,
                      drawable.intrinsicWidth,
                      drawable.intrinsicHeight
                    )
                    if (eachAbi != abi % Constants.MULTI_ARCH) {
                      drawable.alpha = 128
                    } else {
                      drawable.alpha = 255
                    }
                    val span = CenterAlignImageSpan(drawable)
                    spanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
                  }
                if (eachAbi != abi % Constants.MULTI_ARCH) {
                  spanString.setSpan(
                    StrikethroughSpan(),
                    2,
                    spanString.length,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                  )
                }
                spanStringBuilder.append(spanString)
                if (itemCount < abiSet.size) {
                  spanStringBuilder.append(", ")
                }
                if (itemCount == 3 && abiSet.size > 3 && isOrientationPortrait) {
                  spanStringBuilder.appendLine()
                }
              }
              extraInfo.append(spanStringBuilder).appendLine()
            }

            val advanced = when (abi) {
              Constants.ERROR -> getString(R.string.cannot_read)
              Constants.OVERLAY -> Constants.OVERLAY_STRING
              else -> ""
            }
            extraInfo.apply {
              append(advanced)
              if (abi >= Constants.MULTI_ARCH) {
                if (advanced.isNotEmpty()) {
                  append(", ")
                }
                append(getString(R.string.multiArch))
                append(", ")
              }
              append(PackageUtils.getTargetApiString(it))
              append(", ").append(PackageUtils.getMinSdkVersion(packageInfo))
              packageInfo.sharedUserId?.let { sharedUid ->
                appendLine().append("sharedUserId = $sharedUid")
              }
            }
            detailsTitle.extraInfoView.text = extraInfo
          } catch (e: Exception) {
            Timber.e(e)
            finish()
          }

          ibSort.setOnClickListener {
            lifecycleScope.launch {
              detailFragmentManager.sortAll()
              viewModel.sortMode = if (viewModel.sortMode == MODE_SORT_BY_LIB) {
                MODE_SORT_BY_SIZE
              } else {
                MODE_SORT_BY_LIB
              }
              detailFragmentManager.changeSortMode(viewModel.sortMode)
            }
          }
        }

        viewModel.initComponentsData(path)
      } ?: run {
        Timber.e("PackageInfo is null")
        finish()
      }
    } catch (e: Exception) {
      showToast(R.string.toast_use_another_file_manager)
      finish()
    } finally {
      inputStream?.close()
    }

    val types = mutableListOf(
      NATIVE, SERVICE, ACTIVITY, RECEIVER, PROVIDER, PERMISSION, METADATA, DEX
    )
    val tabTitles = mutableListOf(
      getText(R.string.ref_category_native),
      getText(R.string.ref_category_service),
      getText(R.string.ref_category_activity),
      getText(R.string.ref_category_br),
      getText(R.string.ref_category_cp),
      getText(R.string.ref_category_perm),
      getText(R.string.ref_category_metadata),
      getText(R.string.ref_category_dex)
    )
    if (packageInfo != null && PackageUtils.getStaticLibs(packageInfo).isNotEmpty()) {
      types.add(1, STATIC)
      tabTitles.add(1, getText(R.string.ref_category_static))
    }

    binding.viewpager.adapter = object : FragmentStateAdapter(this) {
      override fun getItemCount(): Int {
        return types.size
      }

      override fun createFragment(position: Int): Fragment {
        return when (position) {
          types.indexOf(NATIVE) -> NativeAnalysisFragment.newInstance(path)
          types.indexOf(STATIC) -> StaticAnalysisFragment.newInstance(path)
          types.indexOf(PERMISSION) -> PermissionAnalysisFragment.newInstance(path)
          types.indexOf(METADATA) -> MetaDataAnalysisFragment.newInstance(path)
          types.indexOf(DEX) -> DexAnalysisFragment.newInstance(path)
          else -> ComponentsAnalysisFragment.newInstance(types[position])
        }
      }
    }
    binding.tabLayout.apply {
      tabTitles.forEach {
        addTab(newTab().apply { text = it })
      }
      addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
          val count = viewModel.itemsCountList[types[tab.position]]
          if (detailFragmentManager.currentItemsCount != count) {
            binding.tsComponentCount.setText(count.toString())
            detailFragmentManager.currentItemsCount = count
          }
          detailFragmentManager.selectedPosition = tab.position
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {}

        override fun onTabReselected(tab: TabLayout.Tab?) {}
      })
    }

    val mediator = TabLayoutMediator(binding.tabLayout, binding.viewpager) { tab, position ->
      tab.text = tabTitles[position]
    }
    mediator.attach()

    viewModel.itemsCountLiveData.observe(this) { locatedCount ->
      if (detailFragmentManager.currentItemsCount != locatedCount.count && types[binding.tabLayout.selectedTabPosition] == locatedCount.locate) {
        binding.tsComponentCount.setText(locatedCount.count.toString())
        detailFragmentManager.currentItemsCount = locatedCount.count
      }
    }
  }
}
