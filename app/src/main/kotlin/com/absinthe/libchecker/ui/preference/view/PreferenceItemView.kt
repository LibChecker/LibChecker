package com.absinthe.libchecker.ui.preference.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.preference.model.PreferenceItemRenderState
import com.absinthe.libchecker.utils.extensions.getColor
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.card.MaterialCardView

class PreferenceItemView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

  private val iconFrame by lazy { findViewById<FrameLayout>(R.id.icon_frame) }
  private val icon by lazy { findViewById<View>(android.R.id.icon) }
  private val widgetFrame by lazy { findViewById<View>(android.R.id.widget_frame) }
  private val chevron by lazy { findViewById<View>(R.id.settings_preference_chevron) }
  private val title by lazy { findViewById<View>(android.R.id.title) }
  private val summary by lazy { findViewById<View>(android.R.id.summary) }
  private var badge: BadgeDrawable? = null

  override fun onFinishInflate() {
    super.onFinishInflate()
    title.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    summary.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }

  fun bind(state: PreferenceItemRenderState) {
    bindShape(state)
    widgetFrame.isVisible = state.toggleChecked != null
    chevron.isVisible = state.showChevron
    bindBadge(state.badgeDescription != null)
    contentDescription = buildContentDescription(state)
  }

  private fun bindShape(state: PreferenceItemRenderState) {
    val outerRadius = resources.getDimension(R.dimen.settings_preference_corner_radius)
    val innerRadius = resources.getDimension(R.dimen.settings_preference_inner_corner_radius)
    val topRadius = if (state.groupPosition.usesOuterTopCorners) outerRadius else innerRadius
    val bottomRadius = if (state.groupPosition.usesOuterBottomCorners) outerRadius else innerRadius
    shapeAppearanceModel = shapeAppearanceModel.toBuilder()
      .setTopLeftCornerSize(topRadius)
      .setTopRightCornerSize(topRadius)
      .setBottomLeftCornerSize(bottomRadius)
      .setBottomRightCornerSize(bottomRadius)
      .build()

    updateLayoutParams<ViewGroup.MarginLayoutParams> {
      topMargin = if (state.groupPosition.usesOuterTopCorners) {
        0
      } else {
        resources.getDimensionPixelSize(R.dimen.settings_preference_card_spacing)
      }
      bottomMargin = 0
    }
  }

  @SuppressLint("RestrictedApi")
  private fun bindBadge(visible: Boolean) {
    badge?.let { BadgeUtils.detachBadgeDrawable(it, icon) }
    badge = null
    if (!visible) {
      return
    }

    badge = BadgeDrawable.create(context).apply {
      backgroundColor = R.color.material_red_500.getColor(context)
      badgeGravity = BadgeDrawable.TOP_END
      clearNumber()
    }.also {
      BadgeUtils.attachBadgeDrawable(it, icon, iconFrame)
    }
  }

  private fun buildContentDescription(state: PreferenceItemRenderState): String {
    return buildList {
      state.title?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
      state.summary?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
      state.toggleChecked?.let {
        add(
          context.getString(
            if (it) {
              R.string.array_dark_mode_on
            } else {
              R.string.array_dark_mode_off
            }
          )
        )
      }
      state.badgeDescription?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
    }.joinToString()
  }
}
