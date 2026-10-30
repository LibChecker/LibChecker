package com.absinthe.libchecker.domain.app.detail.ui

import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.style.ImageSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout.LayoutParams
import android.widget.ScrollView
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.text.HtmlCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
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
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.copyToClipboard
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.openUrlInBrowser
import com.absinthe.libchecker.utils.extensions.paddingStartCompat
import com.absinthe.libchecker.utils.extensions.paddingTopCompat
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.utils.toJson
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils

object FeaturesDialog {

  fun showSplitApksDialog(activity: FragmentActivity, packageInfo: PackageInfo) {
    AppBundleBottomSheetDialogFragment()
      .putArguments(EXTRA_PACKAGE_INFO to packageInfo)
      .show(activity.supportFragmentManager, AppBundleBottomSheetDialogFragment::class.java.name)
  }

  fun show(context: Context, spec: FeatureDialogSpec) {
    val icon = spec.iconTint?.let {
      UiUtils.changeDrawableColor(context, spec.iconRes, it)
    } ?: requireNotNull(AppCompatResources.getDrawable(context, spec.iconRes))
    val dialog = BaseAlertDialogBuilder(context)
      .setIcon(icon)
      .setTitle(spec.titleRes)
      .setPositiveButton(android.R.string.ok, null)

    val message = buildSpannedString {
      spec.messageRes?.let { messageRes ->
        append(HtmlCompat.fromHtml(context.getString(messageRes), HtmlCompat.FROM_HTML_MODE_COMPACT))
      }
      spec.items?.forEach { item ->
        if (isNotEmpty()) append("\n\n")
        spec.itemIconRes?.let { res ->
          context.getDrawable(res)?.let { itemIcon ->
            itemIcon.setBounds(0, 0, 16.dp, 16.dp)
            inSpans(ImageSpan(itemIcon, ImageSpan.ALIGN_BASELINE)) { append("\uFFFC") }
            append(" ")
          }
        }
        bold { append(item.label.resolve(context)) }
        append("\n")
        append(item.value ?: context.getString(R.string.lib_permission_description_not_provided))
      }
    }
    if (message.isNotEmpty()) dialog.setMessage(message)
    spec.titleEntries?.let { entries ->
      val title = entries.joinToString(", ") { entry ->
        "${entry.label.resolve(context)} <b>${entry.value.orEmpty()}</b>"
      }
      dialog.setTitle(HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_MODE_COMPACT))
    }

    spec.sourceUrl?.let { link ->
      dialog.setNeutralButton(R.string.lib_detail_app_props_tip) { _, _ ->
        context.openUrlInBrowser(link)
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

    AppPropBottomSheetDialogFragment()
      .putArguments(EXTRA_PACKAGE_INFO to pi)
      .show(activity.supportFragmentManager, AppPropBottomSheetDialogFragment::class.java.name)
  }

  fun showAppPropDialog(activity: FragmentActivity, props: Map<String, String>) {
    AppPropBottomSheetDialogFragment()
      .putArguments(EXTRA_PROPS to props.toJson())
      .show(activity.supportFragmentManager, AppPropBottomSheetDialogFragment::class.java.name)
  }

  @RequiresApi(Build.VERSION_CODES.R)
  fun showAppInstallSourceDialog(activity: FragmentActivity, packageName: String) {
    AppInstallSourceBSDFragment()
      .putArguments(EXTRA_PACKAGE_NAME to packageName)
      .show(activity.supportFragmentManager, AppInstallSourceBSDFragment::class.java.name)
  }

  fun showAppIconsDialog(context: Context, drawables: List<Drawable>, isFirstMonochrome: Boolean) {
    val dialogDrawables = drawables.map { drawable ->
      drawable.constantState?.newDrawable(context.resources)?.mutate() ?: drawable
    }
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
    dialogDrawables.forEach { drawable ->
      flexLayout.addView(
        AppCompatImageView(context).apply {
          layoutParams = ViewGroup.MarginLayoutParams(48.dp, 48.dp).apply {
            setMargins(0, 8.dp, 16.dp, 8.dp)
          }
          scaleType = ImageView.ScaleType.FIT_XY
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
      .setIcon(
        dialogDrawables.firstOrNull()?.let {
          it.constantState?.newDrawable(context.resources)?.mutate() ?: it
        }
      )
      .setTitle(titleRes)
      .setView(scrollView)
      .setPositiveButton(android.R.string.ok, null)

    dialog.show()
    Telemetry.recordEvent(
      Constants.Event.FEATURE_DIALOG,
      mapOf(Telemetry.Param.CONTENT to context.getString(titleRes))
    )
  }
}
