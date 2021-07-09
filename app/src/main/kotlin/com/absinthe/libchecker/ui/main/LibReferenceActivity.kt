package com.absinthe.libchecker.ui.main

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.bean.DetailExtraBean
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.ActivityLibReferenceBinding
import com.absinthe.libchecker.extensions.isOrientationLandscape
import com.absinthe.libchecker.extensions.paddingTopCompat
import com.absinthe.libchecker.recyclerview.adapter.AppAdapter
import com.absinthe.libchecker.ui.detail.AppDetailActivity
import com.absinthe.libchecker.ui.detail.EXTRA_DETAIL_BEAN
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.viewmodel.LibReferenceViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import rikka.widget.borderview.BorderView

const val EXTRA_REF_NAME = "REF_NAME"
const val EXTRA_REF_TYPE = "REF_TYPE"

class LibReferenceActivity : BaseActivity() {

    private lateinit var binding: ActivityLibReferenceBinding
    private val adapter by lazy { AppAdapter(lifecycleScope) }
    private val viewModel by viewModels<LibReferenceViewModel>()
    private val refName by lazy { intent.extras?.getString(EXTRA_REF_NAME) }
    private val refType by lazy { intent.extras?.getInt(EXTRA_REF_TYPE) ?: NATIVE }

    override fun setViewBinding(): ViewGroup {
        binding = ActivityLibReferenceBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        refName?.let { name ->
            initView()
            viewModel.dbItems.observe(this, {
                viewModel.setData(name, refType)
            })

            lifecycleScope.launch(Dispatchers.IO) {
                LCAppUtils.getRuleWithRegex(name, refType)?.let {
                    withContext(Dispatchers.Main) {
                        binding.toolbar.title = it.label
                    }
                }
            }
        } ?: finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.release()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.root.apply {
            fitsSystemWindows = isOrientationLandscape
            paddingTopCompat = 0
        }
    }

    private fun initView() {
        binding.apply {
            root.apply {
                fitsSystemWindows = isOrientationLandscape
                paddingTopCompat = 0
            }

            setAppBar(appbar, toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            (root as ViewGroup).bringChildToFront(appbar)

            list.apply {
                adapter = this@LibReferenceActivity.adapter
                borderVisibilityChangedListener =
                    BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
                        appBar?.setRaised(!top)
                    }
                setHasFixedSize(true)
                FastScrollerBuilder(this).useMd2Style().build()
            }
            vfContainer.apply {
                setInAnimation(
                    this@LibReferenceActivity,
                    R.anim.anim_fade_in
                )
                setOutAnimation(
                    this@LibReferenceActivity,
                    R.anim.anim_fade_out
                )
                displayedChild = 0
                (root as ViewGroup).bringChildToFront(appbar)
            }
            lottie.apply {
                imageAssetsFolder = "/"

                val assetName = when(GlobalValues.season) {
                    SPRING -> "lib_reference_spring.json"
                    SUMMER -> "lib_reference_summer.json"
                    AUTUMN -> "lib_reference_autumn.json"
                    WINTER -> "lib_reference_winter.json"
                    else -> "lib_reference_summer.json"
                }

                setAnimation(assetName)
            }
        }

        viewModel.libRefList.observe(this, {
            adapter.setList(it)
            binding.vfContainer.displayedChild = 1
        })

        adapter.setOnItemClickListener { _, view, position ->
            if (AntiShakeUtils.isInvalidClick(view)) {
                return@setOnItemClickListener
            }

            val intent = Intent(this, AppDetailActivity::class.java).apply {
                putExtras(Bundle().apply {
                    val item = adapter.getItem(position)
                    putString(EXTRA_PACKAGE_NAME, item.packageName)
                    putString(EXTRA_REF_NAME, refName)
                    putInt(EXTRA_REF_TYPE, refType)
                    putParcelable(EXTRA_DETAIL_BEAN, DetailExtraBean(item.isSplitApk, item.isKotlinUsed, item.variant))
                })
            }
            startActivity(intent)
        }
    }
}
