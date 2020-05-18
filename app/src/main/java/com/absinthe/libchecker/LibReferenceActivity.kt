package com.absinthe.libchecker

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.bean.AppItem
import com.absinthe.libchecker.databinding.ActivityLibReferenceBinding
import com.absinthe.libchecker.recyclerview.AppAdapter
import com.absinthe.libchecker.ui.main.MainActivity
import com.absinthe.libchecker.utils.ActivityStackManager
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.viewmodel.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val TYPE_NATIVE = 0
const val TYPE_SERVICE = 1

const val EXTRA_NAME = "NAME"
const val EXTRA_TYPE = "TYPE"

class LibReferenceActivity : BaseActivity() {

    private lateinit var binding: ActivityLibReferenceBinding

    private val viewModel by lazy {
        ViewModelProvider(ActivityStackManager.getActivity(MainActivity::class.java)!!).get(
            AppViewModel::class.java
        )
    }
    private val adapter = AppAdapter()

    override fun setViewBinding() {
        binding = ActivityLibReferenceBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setRoot() {
        root = binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.rvList.adapter = this.adapter
        binding.vfContainer.apply {
            setInAnimation(this@LibReferenceActivity, R.anim.anim_fade_in)
            setOutAnimation(this@LibReferenceActivity, R.anim.anim_fade_out)
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        intent.extras?.let {
            it.getString(EXTRA_NAME)?.let { name ->
                    it.getInt(EXTRA_TYPE).let { type ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        val list = mutableListOf<AppItem>()

                        viewModel.appItems.value?.let { items ->

                            if (type == TYPE_NATIVE) {
                                for (item in items) {
                                    val packageInfo = PackageUtils.getPackageInfo(item.packageName)

                                    val natives = PackageUtils.getAbiByNativeDir(
                                        packageInfo.applicationInfo.sourceDir,
                                        packageInfo.applicationInfo.nativeLibraryDir
                                    )

                                    for (native in natives) {
                                        if (native.name == name) {
                                            list.add(item)
                                            break
                                        }
                                    }
                                }
                            } else if (type == TYPE_SERVICE) {
                                for (item in items) {
                                    val packageInfo = packageManager.getPackageInfo(item.packageName, PackageManager.GET_SERVICES)
                                    packageInfo.services?.let { services ->
                                        for (service in services) {
                                            if (service.name == name) {
                                                list.add(item)
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        withContext(Dispatchers.Main) {
                            adapter.setNewInstance(list)
                            binding.vfContainer.displayedChild = 1
                        }
                    }

                }
            } ?: finish()
        } ?: finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}
