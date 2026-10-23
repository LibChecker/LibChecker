package com.absinthe.libchecker.domain.settings.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.settings.model.CloudRulesDialogAction
import com.absinthe.libchecker.domain.settings.model.CloudRulesDialogState
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.BottomSheetScaffoldView
import com.absinthe.libchecker.view.app.RuleLoadingView
import com.absinthe.libraries.utils.view.HeightAnimatableViewFlipper
import com.google.android.material.button.MaterialButton

class CloudRulesDialogView(context: Context) : BottomSheetScaffoldView(context) {

  private var onAction: (CloudRulesDialogAction) -> Unit = {}

  private val viewFlipper = HeightAnimatableViewFlipper(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    // This flipper already animates child alpha; XML fades would compete with it.
  }

  private val loading = RuleLoadingView(context).apply {
    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 160.dp).also {
      it.gravity = Gravity.CENTER
    }
  }

  private val cloudRulesContentView = CloudRulesContentView(
    context = context,
    onUpdate = { onAction(CloudRulesDialogAction.Update) },
    onRetry = { onAction(CloudRulesDialogAction.Retry) }
  ).apply {
    layoutParams = FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
  }

  init {
    header.title.text = context.getString(R.string.cloud_rules)
    addContentView(viewFlipper)
    viewFlipper.addView(loading)
    viewFlipper.addView(cloudRulesContentView)
  }

  private class CloudRulesContentView(
    context: Context,
    private val onUpdate: () -> Unit,
    private val onRetry: () -> Unit
  ) : AViewGroup(context) {

    private val localVersion = CloudRulesVersionView(
      context,
      context.getString(R.string.rules_local_repo_version)
    ).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
    }

    private val arrow = AppCompatImageView(context).apply {
      layoutParams = LayoutParams(48.dp, 48.dp)
      setImageResource(R.drawable.ic_arrow_right)
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private val remoteVersion = CloudRulesVersionView(
      context,
      context.getString(R.string.rules_remote_repo_version)
    ).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
    }

    private val updateButton = MaterialButton(context).apply {
      layoutParams = LayoutParams(300.dp, ViewGroup.LayoutParams.WRAP_CONTENT).also {
        it.topMargin = 48.dp
      }
      isEnabled = false
      text = context.getString(R.string.rules_btn_update)
    }

    init {
      setPadding(0, 48.dp, 0, 48.dp)
      clipToPadding = false
      addView(localVersion)
      addView(arrow)
      addView(remoteVersion)
      addView(updateButton)
      updateButton.setOnClickListener { onUpdate() }
    }

    fun bind(state: CloudRulesDialogState.Content) {
      updateButton.setOnClickListener { onUpdate() }
      localVersion.bind(state.localVersion.toString())
      remoteVersion.bind(state.remoteVersion.toString())
      if (state.updateAvailable) {
        updateButton.apply {
          isEnabled = true
          text = context.getString(R.string.rules_btn_restart_to_update)
        }
      } else {
        updateButton.apply {
          isEnabled = false
          text = context.getString(R.string.rules_btn_update)
        }
      }
    }

    fun showRetryError(version: Int?) {
      localVersion.bind(version?.toString() ?: "?")
      remoteVersion.bind("?")
      updateButton.apply {
        isEnabled = true
        text = context.getString(R.string.toast_cloud_rules_update_error)
      }
      updateButton.setOnClickListener { onRetry() }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      localVersion.autoMeasure()
      arrow.autoMeasure()
      remoteVersion.autoMeasure()
      updateButton.autoMeasure()
      setMeasuredDimension(
        measuredWidth,
        paddingTop + localVersion.measuredHeight + updateButton.marginTop + updateButton.measuredHeight + paddingBottom
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      arrow.layout(arrow.toHorizontalCenter(this), paddingTop)
      localVersion.layout(
        arrow.left - 24.dp - localVersion.measuredWidth,
        localVersion.toViewVerticalCenter(arrow)
      )
      remoteVersion.layout(arrow.right + 24.dp, remoteVersion.toViewVerticalCenter(arrow))
      updateButton.layout(
        updateButton.toHorizontalCenter(this),
        localVersion.bottom + updateButton.marginTop
      )
    }
  }

  private class CloudRulesVersionView(
    context: Context,
    description: CharSequence
  ) : LinearLayout(context) {

    private val version = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceHeadlineLarge))
    }

    private val desc = AppCompatTextView(
      ContextThemeWrapper(
        context,
        R.style.TextView_SansSerifCondensedMedium
      )
    ).apply {
      layoutParams = LayoutParams(120.dp, ViewGroup.LayoutParams.WRAP_CONTENT)
      gravity = Gravity.CENTER
      text = description
    }

    init {
      orientation = VERTICAL
      gravity = Gravity.CENTER_HORIZONTAL
      addView(version)
      addView(desc)
    }

    fun bind(value: CharSequence) {
      version.text = value
      contentDescription = listOf(desc.text, version.text)
        .map { it.toString().trim() }
        .filter(String::isNotEmpty)
        .joinToString()
    }
  }

  fun bind(
    state: CloudRulesDialogState,
    onAction: (CloudRulesDialogAction) -> Unit
  ) {
    this.onAction = onAction
    when (state) {
      is CloudRulesDialogState.Error -> {
        cloudRulesContentView.showRetryError(state.localVersion)
        loading.stop()
        if (viewFlipper.displayedChildView != cloudRulesContentView) {
          viewFlipper.show(cloudRulesContentView)
        }
      }

      CloudRulesDialogState.Loading -> {
        loading.start()
        if (viewFlipper.displayedChildView != loading) {
          viewFlipper.show(loading)
        }
      }

      is CloudRulesDialogState.Content -> {
        cloudRulesContentView.bind(state)
        loading.stop()
        if (viewFlipper.displayedChildView != cloudRulesContentView) {
          viewFlipper.show(cloudRulesContentView)
        }
      }
    }
  }
}
