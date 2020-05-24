package com.absinthe.libchecker.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.utils.UiUtils
import com.blankj.utilcode.util.ToastUtils
import com.drakeet.about.*
import com.google.android.material.appbar.AppBarLayout

class AboutActivity : AbsAboutActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        initView()
    }

    override fun onCreateHeader(icon: ImageView, slogan: TextView, version: TextView) {
        icon.setImageResource(R.drawable.pic_splash)
        slogan.setText(R.string.app_name)
        version.text = String.format("Version: %s", BuildConfig.VERSION_NAME)
    }

    override fun onItemsCreated(items: MutableList<Any>) {
        items.apply {
            add(Category("What's this"))
            add(Card(getString(R.string.about_info)))

            add(Category("Developers"))
            add(Contributor(R.mipmap.pic_rabbit, "Absinthe", "Developer & Designer", "https://www.coolapk.com/u/482045"))

            add(Category("Open Source Licenses"))
            add(License("kotlin", "JetBrains", License.APACHE_2, "https://github.com/JetBrains/kotlin"))
            add(License("MultiType", "drakeet", License.APACHE_2, "https://github.com/drakeet/MultiType"))
            add(License("about-page", "drakeet", License.APACHE_2, "https://github.com/drakeet/about-page"))
            add(License("AndroidX", "Google", License.APACHE_2, "https://source.google.com"))
            add(License("Android Jetpack", "Google", License.APACHE_2, "https://source.google.com"))
            add(License("gson", "Google", License.APACHE_2, "https://github.com/google/gson"))
            add(License("material-components-android", "Google", License.APACHE_2, "https://github.com/material-components/material-components-android"))
            add(License("lottie-android", "Airbnb", License.APACHE_2, "https://github.com/airbnb/lottie-android"))
            add(License("MPAndroidChart", "PhilJay", License.APACHE_2, "https://github.com/PhilJay/MPAndroidChart"))
            add(License("Once", "jonfinerty", License.APACHE_2, "https://github.com/jonfinerty/Once"))
            add(License("BaseRecyclerViewAdapterHelper", "CymChad", License.MIT, "https://github.com/CymChad/BaseRecyclerViewAdapterHelper"))
            add(License("AndroidUtilCode", "Blankj", License.APACHE_2, "https://github.com/Blankj/AndroidUtilCode"))
            add(License("OkHttp", "Square", License.APACHE_2, "https://github.com/square/okhttp"))
            add(License("Retrofit", "Square", License.APACHE_2, "https://github.com/square/retrofit"))
            add(License("contour", "Square", License.APACHE_2, "https://github.com/cashapp/contour"))
            add(License("AndResGuard", "shwenzhang", License.APACHE_2, "https://github.com/shwenzhang/AndResGuard"))
            add(License("libraries", "RikkaApps", License.APACHE_2, "https://github.com/RikkaApps/libraries"))
        }
    }

    private fun initView() {
        UiUtils.setDarkMode(this)
        UiUtils.setSystemBarTransparent(this)

        val appbar = findViewById<AppBarLayout>(com.drakeet.about.R.id.header_layout)
        appbar.fitsSystemWindows = true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.about_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.toolbar_rate) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = URLManager.MARKET_PAGE.toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
                ToastUtils.showLong("Not exist any app market")
            }
        }
        return super.onOptionsItemSelected(menuItem)
    }
}