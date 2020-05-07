package com.absinthe.libchecker.ui.applist

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.absinthe.libchecker.databinding.ActivityAppDetailBinding
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.view.EXTRA_PKG_NAME
import com.blankj.utilcode.util.AppUtils
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialContainerTransformSharedElementCallback

class AppDetailActivity : AppCompatActivity() {

    private val binding by lazy { ActivityAppDetailBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        initTransition()

        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        intent.extras?.let {
            val bundle = it
            bundle.getString(EXTRA_PKG_NAME)?.let { packageName ->
                supportActionBar?.apply {
                    title = AppUtils.getAppName(packageName)
                }
                binding.ivAppIcon.setImageDrawable(AppUtils.getAppIcon(packageName))
                binding.tvAppName.text = AppUtils.getAppName(packageName)
                binding.tvPackageName.text = packageName
                binding.tvVersion.text = PackageUtils.getVersionString(packageName)
                binding.tvTargetApi.text = PackageUtils.getTargetApiString(packageName)
            } ?: finish()

        } ?: finish()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initTransition() {
        window.apply {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            sharedElementEnterTransition = MaterialContainerTransform().apply {
                addTarget(android.R.id.content)
                duration = 300L
            }
            sharedElementReturnTransition = MaterialContainerTransform().apply {
                addTarget(android.R.id.content)
                duration = 300L
            }
        }
        findViewById<View>(android.R.id.content).transitionName = "app_card_container"
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
    }
}
