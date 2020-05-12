package com.absinthe.libchecker.ui.about

import android.widget.ImageView
import android.widget.TextView
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.drakeet.about.*

class AboutActivity : AbsAboutActivity() {

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
            add(License("lottie-android", "Airbnb", License.APACHE_2, "https://github.com/airbnb/lottie-android"))
            add(License("MPAndroidChart", "PhilJay", License.APACHE_2, "https://github.com/PhilJay/MPAndroidChart"))
            add(License("material-components-android", "Google", License.APACHE_2, "https://github.com/material-components/material-components-android"))
            add(License("Once", "jonfinerty", License.APACHE_2, "https://github.com/jonfinerty/Once"))
            add(License("BaseRecyclerViewAdapterHelper", "CymChad", License.MIT, "https://github.com/CymChad/BaseRecyclerViewAdapterHelper"))
            add(License("AndroidUtilCode", "Blankj", License.APACHE_2, "https://github.com/Blankj/AndroidUtilCode"))
            add(License("CacheFunctionUtil", "heruoxin", License.MIT, "https://github.com/heruoxin/CacheFunctionUtil"))
        }
    }
}