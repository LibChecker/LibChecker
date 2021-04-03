package com.absinthe.libchecker.view.detail

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ViewFlipper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.app.IHeaderView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable

const val VF_LOADING = 0
const val VF_CONTENT = 1
const val VF_NOT_FOUND = 2

class LibDetailBottomSheetView(context: Context) : AViewGroup(context), IHeaderView {

    private val header = BottomSheetHeaderView(context).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        title.text = context.getString(R.string.lib_detail_dialog_title)
    }

    val icon = AppCompatImageView(context).apply {
        val iconSize = 48.dp
        layoutParams = LayoutParams(iconSize, iconSize).also {
            it.topMargin = 4.dp
        }
        setBackgroundResource(R.drawable.bg_gray_circle)
    }

    val title = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifCondensedMedium)).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.topMargin = 4.dp
        }
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    }

    val viewFlipper = ViewFlipper(context).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setInAnimation(context, R.anim.anim_fade_in)
        setOutAnimation(context, R.anim.anim_fade_out)
    }

    private val loading = LottieAnimationView(context).apply {
        layoutParams = FrameLayout.LayoutParams(200.dp, 200.dp).also {
            it.gravity = Gravity.CENTER
        }
        imageAssetsFolder = "/"
        repeatCount = LottieDrawable.INFINITE
        setAnimation("lib_detail_rocket.json")
    }

    private val notFoundView = NotFoundView(context).apply {
        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).also {
            it.gravity = Gravity.CENTER
        }
    }

    val libDetailContentView = LibDetailContentView(context).apply {
        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
    }

    init {
        val padding = 24.dp
        setPadding(padding, 16.dp, padding, padding)
        addView(header)
        addView(icon)
        addView(title)
        addView(viewFlipper)
        viewFlipper.addView(loading)
        viewFlipper.addView(libDetailContentView)
        viewFlipper.addView(notFoundView)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        loading.playAnimation()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        header.measure((measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(), header.defaultHeightMeasureSpec(this))
        icon.autoMeasure()
        title.measure((measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(), title.defaultHeightMeasureSpec(this))
        viewFlipper.measure((measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(), viewFlipper.defaultHeightMeasureSpec(this))
        setMeasuredDimension(measuredWidth, paddingTop + paddingBottom + header.measuredHeight + title.measuredHeight + icon.measuredHeight + title.marginTop + icon.marginTop + viewFlipper.measuredHeight + 16.dp)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        header.layout(paddingStart, paddingTop)
        icon.layout(icon.toHorizontalCenter(this), header.bottom + icon.marginTop)
        title.layout(title.toHorizontalCenter(this), icon.bottom + title.marginTop)
        viewFlipper.layout(paddingStart, title.bottom.coerceAtLeast(icon.bottom) + 16.dp)
    }

    override fun getHeaderView(): BottomSheetHeaderView {
        return header
    }

    class LibDetailItemView(context: Context) : AViewGroup(context) {

        val icon = AppCompatImageView(context).apply {
            layoutParams = LayoutParams(24.dp, 24.dp)
        }

        val text = AppCompatTextView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.marginStart = 8.dp
            }
        }

        init {
            setPadding(8.dp, 8.dp, 8.dp, 8.dp)
            setBackgroundResource(R.drawable.bg_lib_detail_item)
            addView(icon)
            addView(text)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            icon.autoMeasure()
            text.measure((measuredWidth - paddingStart - paddingEnd - icon.measuredWidth - text.marginStart).toExactlyMeasureSpec(), text.defaultHeightMeasureSpec(this))
            setMeasuredDimension(measuredWidth, text.measuredHeight.coerceAtLeast(icon.measuredHeight) + paddingTop + paddingBottom)
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            icon.layout(paddingStart, icon.toVerticalCenter(this))
            text.layout(icon.right + text.marginStart, text.toVerticalCenter(this))
        }
    }

    class NotFoundView(context: Context) : AViewGroup(context) {

        private val icon = AppCompatImageView(context).apply {
            layoutParams = LayoutParams(64.dp, 64.dp)
            setImageResource(R.drawable.ic_failed)
        }

        private val notFoundText = AppCompatTextView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            text = context.getString(R.string.not_found)
            setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBody2))
        }

        private val createNewIssueText = AppCompatTextView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.topMargin = 16.dp
            }
            text = context.getString(R.string.create_an_issue)
            setLinkTextColor(context.getColor(R.color.colorPrimary))
            compoundDrawablePadding = 4.dp
            setCompoundDrawables(context.getDrawable(R.drawable.ic_github), null, null, null)
            isClickable = true
            movementMethod = LinkMovementMethod.getInstance()
            text = HtmlCompat.fromHtml(
                "<a href='${ApiManager.GITHUB_NEW_ISSUE_URL}'> ${
                    resources.getText(
                        R.string.create_an_issue
                    )
                } </a>", HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }

        init {
            addView(icon)
            addView(notFoundText)
            addView(createNewIssueText)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            icon.autoMeasure()
            notFoundText.autoMeasure()
            createNewIssueText.autoMeasure()
            setMeasuredDimension(measuredWidth, icon.measuredHeight + notFoundText.measuredHeight + createNewIssueText.marginTop + createNewIssueText.measuredHeight)
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            icon.layout(icon.toHorizontalCenter(this), 0)
            notFoundText.layout(notFoundText.toHorizontalCenter(this), icon.bottom)
            createNewIssueText.layout(createNewIssueText.toHorizontalCenter(this), notFoundText.bottom + createNewIssueText.marginTop)
        }
    }

    class LibDetailContentView(context: Context) : AViewGroup(context) {

        val label = LibDetailItemView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            icon.setImageResource(R.drawable.ic_label)
            text.setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2))
        }

        val team = LibDetailItemView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            icon.setImageResource(R.drawable.ic_team)
            text.setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2))
        }

        val contributor = LibDetailItemView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            icon.setImageResource(R.drawable.ic_github)
            text.setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2))
        }

        val description = LibDetailItemView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            icon.setImageResource(R.drawable.ic_content)
            text.setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBody2))
        }

        val relativeLink = LibDetailItemView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            icon.setImageResource(R.drawable.ic_url)
            text.setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBody2))
        }

        init {
            addView(label)
            addView(team)
            addView(contributor)
            addView(description)
            addView(relativeLink)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            label.measure((measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(), label.defaultHeightMeasureSpec(this))
            team.measure((measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(), team.defaultHeightMeasureSpec(this))
            contributor.measure((measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(), contributor.defaultHeightMeasureSpec(this))
            description.measure((measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(), description.defaultHeightMeasureSpec(this))
            relativeLink.measure((measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(), relativeLink.defaultHeightMeasureSpec(this))
            setMeasuredDimension(measuredWidth, label.measuredHeight + team.measuredHeight + contributor.measuredHeight + description.measuredHeight + relativeLink.measuredHeight + 16.dp * 4)
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            label.layout(0, 0)
            val marginVertical = 8.dp
            team.layout(0, label.bottom + marginVertical)
            contributor.layout(0, team.bottom + marginVertical)
            description.layout(0, contributor.bottom + marginVertical)
            relativeLink.layout(0, description.bottom + marginVertical)
        }

    }
}