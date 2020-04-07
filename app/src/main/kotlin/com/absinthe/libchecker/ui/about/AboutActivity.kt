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
        items.add(Category("What's this"))
        items.add(Card(getString(R.string.about_info)))

        items.add(Category("Developers"))
        items.add(Contributor(R.mipmap.pic_rabbit, "Absinthe", "Developer & Designer", "https://www.coolapk.com/u/482045"))

        items.add(Category("Open Source Licenses"))
        items.add(License("kotlin", "JetBrains", License.APACHE_2, "https://github.com/JetBrains/kotlin"))
        items.add(License("MultiType", "drakeet", License.APACHE_2, "https://github.com/drakeet/MultiType"))
        items.add(License("about-page", "drakeet", License.APACHE_2, "https://github.com/drakeet/about-page"))
        items.add(License("AndroidX", "Google", License.APACHE_2, "https://source.google.com"))
        items.add(License("Android Jetpack", "Google", License.APACHE_2, "https://source.google.com"))
        items.add(License("lottie-android", "Airbnb", License.APACHE_2, "https://github.com/airbnb/lottie-android"))
        items.add(License("MPAndroidChart", "PhilJay", License.APACHE_2, "https://github.com/PhilJay/MPAndroidChart"))
    }
}