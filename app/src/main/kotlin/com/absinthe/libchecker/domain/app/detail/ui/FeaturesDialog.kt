package com.absinthe.libchecker.domain.app.detail.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout.LayoutParams
import android.widget.ScrollView
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.app.detail.feature.FeatureDialogEntryPlacement
import com.absinthe.libchecker.domain.app.detail.feature.FeatureDialogSpec
import com.absinthe.libchecker.domain.app.detail.feature.FeatureDialogTitleLabel
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_INFO
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PROPS
import com.absinthe.libchecker.domain.app.detail.ui.dialog.AppBundleBottomSheetDialogFragment
import com.absinthe.libchecker.domain.app.detail.ui.dialog.AppInstallSourceBSDFragment
import com.absinthe.libchecker.domain.app.detail.ui.dialog.AppPropBottomSheetDialogFragment
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.copyToClipboard
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.paddingStartCompat
import com.absinthe.libchecker.utils.extensions.paddingTopCompat
import com.absinthe.libchecker.utils.toJson
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import timber.log.Timber

object FeaturesDialog {

  fun showSplitApksDialog(activity: FragmentActivity, packageInfo: PackageInfo) {
    AppBundleBottomSheetDialogFragment().apply {
      arguments = Bundle().apply {
        putParcelable(EXTRA_PACKAGE_INFO, packageInfo)
      }
      show(activity.supportFragmentManager, AppBundleBottomSheetDialogFragment::class.java.name)
    }
  }

  fun show(context: Context, spec: FeatureDialogSpec) {
    val icon = spec.iconTint?.let {
      UiUtils.changeDrawableColor(context, spec.iconRes, it)
    } ?: requireNotNull(context.getDrawable(spec.iconRes))
    val dialog = BaseAlertDialogBuilder(context)
      .setIcon(icon)
      .setTitle(spec.titleRes)
      .setPositiveButton(android.R.string.ok, null)

    spec.messageRes?.let { messageRes ->
      dialog.setMessage(HtmlCompat.fromHtml(context.getString(messageRes), HtmlCompat.FROM_HTML_MODE_COMPACT))
    }
    spec.titleEntries?.let { entries ->
      when (spec.entryPlacement) {
        FeatureDialogEntryPlacement.TITLE -> {
          val title = entries.joinToString(", ") { entry ->
            "${entry.label.resolve(context)} <b>${entry.value.orEmpty()}</b>"
          }
          dialog.setTitle(HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_MODE_COMPACT))
        }

        FeatureDialogEntryPlacement.MESSAGE -> {
          val message = entries.joinToString("<br>") { entry ->
            val version = entry.value?.let { " <b>$it</b>" }.orEmpty()
            "• ${entry.label.resolve(context)}$version"
          }
          dialog.setMessage(HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_COMPACT))
        }
      }
    }

    spec.sourceUrl?.let { link ->
      dialog.setNeutralButton(R.string.lib_detail_app_props_tip) { _, _ ->
        openSourceLink(context, link)
      }
    }

    dialog.show()
    Telemetry.recordEvent(
      Constants.Event.FEATURE_DIALOG,
      mapOf(Telemetry.Param.CONTENT to context.getString(spec.titleRes))
    )
  }

  private fun FeatureDialogTitleLabel.resolve(context: Context): String {
    return when (this) {
      is FeatureDialogTitleLabel.Resource -> context.getString(res)
      is FeatureDialogTitleLabel.Text -> value
    }
  }

  fun showAppPropDialog(activity: FragmentActivity, packageInfo: PackageInfo?) {
    val pi = packageInfo ?: return

    AppPropBottomSheetDialogFragment().apply {
      arguments = Bundle().apply {
        putParcelable(EXTRA_PACKAGE_INFO, pi)
      }
      show(activity.supportFragmentManager, AppPropBottomSheetDialogFragment::class.java.name)
    }
  }

  fun showAppPropDialog(activity: FragmentActivity, props: Map<String, String>) {
    AppPropBottomSheetDialogFragment().apply {
      arguments = Bundle().apply {
        putString(EXTRA_PROPS, props.toJson())
      }
      show(activity.supportFragmentManager, AppPropBottomSheetDialogFragment::class.java.name)
    }
  }

  @RequiresApi(Build.VERSION_CODES.R)
  fun showAppInstallSourceDialog(activity: FragmentActivity, packageName: String) {
    AppInstallSourceBSDFragment().apply {
      arguments = Bundle().apply {
        putString(EXTRA_PACKAGE_NAME, packageName)
      }
      show(activity.supportFragmentManager, AppInstallSourceBSDFragment::class.java.name)
    }
  }

  fun showAppIconsDialog(context: Context, drawables: List<Drawable>, isFirstMonochrome: Boolean) {
    val flexLayout = FlexboxLayout(context).apply {
      layoutParams = LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT
      )
      paddingStartCompat = 24.dp
      paddingTopCompat = 16.dp
      flexWrap = FlexWrap.WRAP
      justifyContent = JustifyContent.FLEX_START
      flexDirection = FlexDirection.ROW
    }

    @OptIn(ExperimentalBadgeUtils::class)
    fun createBadge(anchor: View) {
      val badge = BadgeDrawable.create(context).apply {
        text = context.getString(R.string.dialog_themed)
        badgeGravity = BadgeDrawable.TOP_START
      }
      BadgeUtils.attachBadgeDrawable(badge, anchor)
    }

    val titleRes = R.string.dialog_themed_and_alternative_app_icons
    drawables.forEach { drawable ->
      flexLayout.addView(
        AppCompatImageView(context).apply {
          layoutParams = ViewGroup.MarginLayoutParams(48.dp, 48.dp).apply {
            setMargins(0, 8.dp, 16.dp, 8.dp)
          }
          scaleType = ImageView.ScaleType.CENTER_CROP
          setImageDrawable(drawable)
          contentDescription = context.getString(titleRes)
          setOnLongClickListener {
            copyToClipboard()
            true
          }
        }
      )
    }
    if (isFirstMonochrome) {
      flexLayout.getChildAt(0)?.let {
        it.post { createBadge(it) }
      }
    }
    val scrollView = ScrollView(context).apply {
      layoutParams = LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT
      )
      addView(flexLayout)
    }

    val dialog = BaseAlertDialogBuilder(context)
      .setIcon(drawables[0])
      .setTitle(titleRes)
      .setView(scrollView)
      .setPositiveButton(android.R.string.ok, null)

    dialog.show()
    Telemetry.recordEvent(
      Constants.Event.FEATURE_DIALOG,
      mapOf(Telemetry.Param.CONTENT to context.getString(titleRes))
    )
  }

  private fun openSourceLink(context: Context, link: String) {
    runCatching {
      CustomTabsIntent.Builder().build().apply {
        launchUrl(context, link.toUri())
      }
    }.onFailure {
      Timber.e(it)
      runCatching {
        val intent = Intent(Intent.ACTION_VIEW)
          .setData(link.toUri())
        context.startActivity(intent)
      }.onFailure { inner ->
        Timber.e(inner)
        Toasty.showShort(context, "No browser application")
      }
    }
  }
}
