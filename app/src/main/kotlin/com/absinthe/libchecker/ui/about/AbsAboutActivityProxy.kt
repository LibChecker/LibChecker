package com.absinthe.libchecker.ui.about

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.about.Card
import com.drakeet.about.CardViewBinder
import com.drakeet.about.Category
import com.drakeet.about.CategoryViewBinder
import com.drakeet.about.Contributor
import com.drakeet.about.ImageLoader
import com.drakeet.about.License
import com.drakeet.about.LicenseViewBinder
import com.drakeet.about.Line
import com.drakeet.about.LineViewBinder
import com.drakeet.about.OnContributorClickedListener
import com.drakeet.about.OnRecommendationClickedListener
import com.drakeet.about.R
import com.drakeet.multitype.MultiTypeAdapter
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import rikka.material.app.MaterialActivity

abstract class AbsAboutActivityProxy : MaterialActivity() {

    private var imageLoader: ImageLoader? = null

    private var givenInsetsToDecorView = false
    private var initialized = false

    private lateinit var toolbar: Toolbar
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var headerContentLayout: LinearLayout

    private var items = ArrayList<Any>()

    private lateinit var adapter: MultiTypeAdapter
    private lateinit var slogan: TextView
    private lateinit var version: TextView
    private lateinit var recyclerView: RecyclerView

    var onRecommendationClickedListener: OnRecommendationClickedListener? = null
    var onContributorClickedListener: OnContributorClickedListener? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutRes())
        toolbar = findViewById(R.id.toolbar)
        val icon: ImageView = findViewById(R.id.icon)
        slogan = findViewById(R.id.slogan)
        version = findViewById(R.id.version)
        collapsingToolbar = findViewById(R.id.collapsing_toolbar)
        headerContentLayout = findViewById(R.id.header_content_layout)
        onTitleViewCreated(collapsingToolbar)
        onCreateHeader(icon, slogan, version)

        setSupportActionBar(toolbar)
        val actionBar = supportActionBar

        actionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }

        onApplyPresetAttrs()
        recyclerView = findViewById(R.id.list)
        applyEdgeToEdge()
    }


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        adapter = MultiTypeAdapter()
        adapter.register(Category::class.java, CategoryViewBinder())
        adapter.register(Card::class.java, CardViewBinder())
        adapter.register(Line::class.java, LineViewBinder())
        adapter.register(Contributor::class.java, ContributorViewBinder(this))
        adapter.register(License::class.java, LicenseViewBinder())
        items = ArrayList()
        onItemsCreated(items)
        adapter.items = items
        adapter.setHasStableIds(true)
        recyclerView.addItemDecoration(DividerItemDecoration(adapter))
        recyclerView.adapter = adapter
        initialized = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    protected abstract fun onCreateHeader(icon: ImageView, slogan: TextView, version: TextView)

    protected abstract fun onItemsCreated(items: MutableList<Any>)

    protected open fun onTitleViewCreated(collapsingToolbar: CollapsingToolbarLayout) {

    }

    private fun applyEdgeToEdge() {
        val window = getWindow()
        val navigationBarColor = ContextCompat.getColor(this, R.color.about_page_navigationBarColor)
        window.navigationBarColor = navigationBarColor

        val appBarLayout: AppBarLayout = findViewById(R.id.header_layout);
        val decorView = window.getDecorView();
        val originalRecyclerViewPaddingBottom = recyclerView.getPaddingBottom();

        givenInsetsToDecorView = false

        WindowCompat.setDecorFitsSystemWindows(window, false)


        ViewCompat.setOnApplyWindowInsetsListener(decorView) { v: View?, windowInsets: WindowInsetsCompat ->
            val navigationBarsInsets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val isGestureNavigation: Boolean = navigationBarsInsets.bottom <= 24 * resources.displayMetrics.density
            if (!isGestureNavigation) {
                ViewCompat.onApplyWindowInsets(
                    decorView,
                    windowInsets
                )
                givenInsetsToDecorView = true
            } else if (givenInsetsToDecorView) {
                ViewCompat.onApplyWindowInsets(
                    decorView,
                    WindowInsetsCompat.Builder()
                        .setInsets(
                            WindowInsetsCompat.Type.navigationBars(),
                            Insets.of(
                                navigationBarsInsets.left,
                                navigationBarsInsets.top,
                                navigationBarsInsets.right,
                                0
                            )
                        ).build()
                )
            }
            decorView.setPadding(
                windowInsets.systemWindowInsetLeft,
                decorView.paddingTop,
                windowInsets.systemWindowInsetRight,
                decorView.paddingBottom
            )
            appBarLayout.setPadding(
                appBarLayout.paddingLeft,
                windowInsets.systemWindowInsetTop,
                appBarLayout.paddingRight,
                appBarLayout.paddingBottom
            )
            recyclerView.setPadding(
                recyclerView.paddingLeft,
                recyclerView.paddingTop,
                recyclerView.paddingRight,
                originalRecyclerViewPaddingBottom + navigationBarsInsets.bottom
            )
            windowInsets
        }
    }

    fun onApplyPresetAttrs() {
        val a = obtainStyledAttributes(R.styleable.AbsAboutActivity)

        val headerBackground = a.getDrawable(R.styleable.AbsAboutActivity_aboutPageHeaderBackground)

        headerBackground?.let { setHeaderBackground(it) }

        val headerContentScrim = a.getDrawable(R.styleable.AbsAboutActivity_aboutPageHeaderContentScrim)

        headerContentScrim?.let { setHeaderContentScrim(it) }

        @ColorInt val headerTextColor = a.getColor(R.styleable.AbsAboutActivity_aboutPageHeaderTextColor, -1)

        if (headerTextColor != -1) {
            setHeaderTextColor(headerTextColor)
        }

        val navigationIcon = a.getDrawable(R.styleable.AbsAboutActivity_aboutPageNavigationIcon)

        navigationIcon?.let { setNavigationIcon(it) }

        a.recycle()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setImageLoader(imageLoader: ImageLoader) {
        this.imageLoader = imageLoader
        if (initialized) {
            adapter.notifyDataSetChanged()
        }
    }

    fun getImageLoader() = this.imageLoader

    @LayoutRes
    protected fun layoutRes() = R.layout.about_page_main_activity

    fun setHeaderBackground(@DrawableRes resId: Int) {
        setHeaderBackground(ContextCompat.getDrawable(this, resId)!!)
    }

    fun setHeaderBackground(drawable: Drawable) {
        ViewCompat.setBackground(headerContentLayout, drawable)
    }

  /**
   * Set the drawable to use for the content scrim from resources. Providing null will disable
   * the scrim functionality.
   *
   * @param drawable the drawable to display
   */
    fun setHeaderContentScrim(drawable: Drawable?) {
        collapsingToolbar.contentScrim = drawable
    }

    fun setHeaderContentScrim(@DrawableRes resId: Int) {
        setHeaderContentScrim(ContextCompat.getDrawable(this, resId))
    }

    fun setHeaderTextColor(@ColorInt color: Int) {
        collapsingToolbar.setCollapsedTitleTextColor(color)
        slogan.setTextColor(color)
        version.setTextColor(color)
    }

    /**
     * Set the icon to use for the toolbar's navigation button.
     *
     * @param resId Resource ID of a drawable to set
     */
    fun setNavigationIcon(@DrawableRes resId: Int) {
        toolbar.setNavigationIcon(resId)
    }

    fun setNavigationIcon(drawable: Drawable?) {
        toolbar.navigationIcon = drawable
    }

    fun getToolbar(): Toolbar {
        return toolbar
    }

    fun getCollapsingToolbar(): CollapsingToolbarLayout {
        return collapsingToolbar
    }

    fun getItems(): List<Any?> {
        return items
    }

    fun getAdapter(): MultiTypeAdapter {
        return adapter
    }

    fun getSloganTextView(): TextView {
        return slogan
    }

    fun getVersionTextView(): TextView {
        return version
    }

    fun putOnRecommendationClickedListener( listener: OnRecommendationClickedListener?) {
        onRecommendationClickedListener = listener
    }

    fun provideOnRecommendationClickedListener(): OnRecommendationClickedListener? {
        return onRecommendationClickedListener
    }

    fun putOnContributorClickedListener(listener: OnContributorClickedListener?) {
        onContributorClickedListener = listener
    }

    fun provideOnContributorClickedListener(): OnContributorClickedListener? {
        return onContributorClickedListener
    }
}
