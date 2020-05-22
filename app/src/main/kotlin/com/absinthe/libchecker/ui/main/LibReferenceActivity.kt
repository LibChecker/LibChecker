package com.absinthe.libchecker.ui.main

import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.AppItem
import com.absinthe.libchecker.constant.AppItemRepository
import com.absinthe.libchecker.databinding.ActivityLibReferenceBinding
import com.absinthe.libchecker.recyclerview.AppAdapter
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.view.dialogfragment.EXTRA_PKG_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val TYPE_NATIVE = 0
const val TYPE_SERVICE = 1

const val EXTRA_NAME = "NAME"
const val EXTRA_TYPE = "TYPE"

class LibReferenceActivity : BaseActivity() {

    private lateinit var binding: ActivityLibReferenceBinding
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
            setInAnimation(
                this@LibReferenceActivity,
                R.anim.anim_fade_in
            )
            setOutAnimation(
                this@LibReferenceActivity,
                R.anim.anim_fade_out
            )
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter.setOnItemClickListener { _, view, position ->
            val intent = Intent(this, AppDetailActivity::class.java).apply {
                putExtras(Bundle().apply {
                    putString(EXTRA_PKG_NAME, adapter.getItem(position).packageName)
                })
            }

            val options = ActivityOptions.makeSceneTransitionAnimation(
                this,
                view,
                "app_card_container"
            )
            startActivity(intent, options.toBundle())
        }

        val name = intent.extras?.getString(EXTRA_NAME)
        val type = intent.extras?.getInt(EXTRA_TYPE)

        if (name == null || type == null) {
            finish()
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                setData(name, type)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private suspend fun setData(name: String, type: Int) {

        withContext(Dispatchers.Main) {
            binding.vfContainer.displayedChild = 0
        }

        val list = mutableListOf<AppItem>()

        AppItemRepository.allItems.value?.let { items ->

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
                    val packageInfo =
                        packageManager.getPackageInfo(item.packageName, PackageManager.GET_SERVICES)
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
