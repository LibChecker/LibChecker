package com.absinthe.libchecker.ui.about;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.drakeet.about.Card;
import com.drakeet.about.CardViewBinder;
import com.drakeet.about.Category;
import com.drakeet.about.CategoryViewBinder;
import com.drakeet.about.Contributor;
import com.drakeet.about.ImageLoader;
import com.drakeet.about.License;
import com.drakeet.about.LicenseViewBinder;
import com.drakeet.about.Line;
import com.drakeet.about.LineViewBinder;
import com.drakeet.about.OnContributorClickedListener;
import com.drakeet.about.OnRecommendationClickedListener;
import com.drakeet.about.R;
import com.drakeet.multitype.MultiTypeAdapter;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.util.ArrayList;
import java.util.List;

import rikka.material.app.MaterialActivity;

/**
 * @author drakeet
 */
@SuppressWarnings("ALL")
public abstract class AbsAboutActivityProxy extends MaterialActivity {

  private Toolbar toolbar;
  private CollapsingToolbarLayout collapsingToolbar;
  private LinearLayout headerContentLayout;

  private List<Object> items;
  private MultiTypeAdapter adapter;
  private TextView slogan, version;
  private RecyclerView recyclerView;
  private @Nullable ImageLoader imageLoader;
  private boolean initialized;
  private @Nullable OnRecommendationClickedListener onRecommendationClickedListener;
  private @Nullable OnContributorClickedListener onContributorClickedListener;

  protected abstract void onCreateHeader(@NonNull ImageView icon, @NonNull TextView slogan, @NonNull TextView version);
  protected abstract void onItemsCreated(@NonNull List<Object> items);

  protected void onTitleViewCreated(@NonNull CollapsingToolbarLayout collapsingToolbar) {}

  public void setImageLoader(@NonNull ImageLoader imageLoader) {
    this.imageLoader = imageLoader;
    if (initialized) {
      adapter.notifyDataSetChanged();
    }
  }

  public @Nullable ImageLoader getImageLoader() {
    return imageLoader;
  }

  @LayoutRes
  protected int layoutRes() {
    return R.layout.about_page_main_activity;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(layoutRes());
    toolbar = findViewById(R.id.toolbar);
    ImageView icon = findViewById(R.id.icon);
    slogan = findViewById(R.id.slogan);
    version = findViewById(R.id.version);
    collapsingToolbar = findViewById(R.id.collapsing_toolbar);
    headerContentLayout = findViewById(R.id.header_content_layout);
    onTitleViewCreated(collapsingToolbar);
    onCreateHeader(icon, slogan, version);

    setSupportActionBar(toolbar);
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setDisplayShowHomeEnabled(true);
    }
    onApplyPresetAttrs();
    recyclerView = findViewById(R.id.list);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      applyEdgeToEdge();
    }
  }

  private boolean givenInsetsToDecorView = false;

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void applyEdgeToEdge() {
    Window window = getWindow();
    int navigationBarColor = ContextCompat.getColor(this, R.color.about_page_navigationBarColor);
    window.setNavigationBarColor(navigationBarColor);

    final AppBarLayout appBarLayout = findViewById(R.id.header_layout);
    final View decorView = window.getDecorView();
    final int originalRecyclerViewPaddingBottom =recyclerView.getPaddingBottom();

    givenInsetsToDecorView = false;
    WindowCompat.setDecorFitsSystemWindows(window, false);
    ViewCompat.setOnApplyWindowInsetsListener(decorView, new OnApplyWindowInsetsListener() {
      @Override
      public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat windowInsets) {
        Insets navigationBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
        boolean isGestureNavigation = navigationBarsInsets.bottom <= 20 * getResources().getDisplayMetrics().density;

        if (!isGestureNavigation) {
          ViewCompat.onApplyWindowInsets(decorView, windowInsets);
          givenInsetsToDecorView = true;
        } else if (givenInsetsToDecorView) {
          ViewCompat.onApplyWindowInsets(
            decorView,
            new WindowInsetsCompat.Builder()
              .setInsets(
                WindowInsetsCompat.Type.navigationBars(),
                Insets.of(navigationBarsInsets.left, navigationBarsInsets.top, navigationBarsInsets.right, 0)
              )
              .build()
          );
        }
        decorView.setPadding(windowInsets.getSystemWindowInsetLeft(), decorView.getPaddingTop(), windowInsets.getSystemWindowInsetRight(), decorView.getPaddingBottom());
        appBarLayout.setPadding(appBarLayout.getPaddingLeft(), windowInsets.getSystemWindowInsetTop(), appBarLayout.getPaddingRight(), appBarLayout.getPaddingBottom());
        recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(), recyclerView.getPaddingRight(), originalRecyclerViewPaddingBottom + navigationBarsInsets.bottom);
        return windowInsets;
      }
    });
  }

  @Override @SuppressWarnings("deprecation")
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    adapter = new MultiTypeAdapter();
    adapter.register(Category.class, new CategoryViewBinder());
    adapter.register(Card.class, new CardViewBinder());
    adapter.register(Line.class, new LineViewBinder());
    adapter.register(Contributor.class, new ContributorViewBinder(this));
    adapter.register(License.class, new LicenseViewBinder());
    items = new ArrayList<>();
    onItemsCreated(items);
    adapter.setItems(items);
    adapter.setHasStableIds(true);
    recyclerView.addItemDecoration(new DividerItemDecoration(adapter));
    recyclerView.setAdapter(adapter);
    initialized = true;
  }

  private void onApplyPresetAttrs() {
    final TypedArray a = obtainStyledAttributes(R.styleable.AbsAboutActivity);
    Drawable headerBackground = a.getDrawable(R.styleable.AbsAboutActivity_aboutPageHeaderBackground);
    if (headerBackground != null) {
      setHeaderBackground(headerBackground);
    }
    Drawable headerContentScrim = a.getDrawable(R.styleable.AbsAboutActivity_aboutPageHeaderContentScrim);
    if (headerContentScrim != null) {
      setHeaderContentScrim(headerContentScrim);
    }
    @ColorInt
    int headerTextColor = a.getColor(R.styleable.AbsAboutActivity_aboutPageHeaderTextColor, -1);
    if (headerTextColor != -1) {
      setHeaderTextColor(headerTextColor);
    }
    Drawable navigationIcon = a.getDrawable(R.styleable.AbsAboutActivity_aboutPageNavigationIcon);
    if (navigationIcon != null) {
      setNavigationIcon(navigationIcon);
    }
    a.recycle();
  }

  /**
   * Use {@link #setHeaderBackground(int)} instead.
   *
   * @param resId The resource id of header background
   */
  @Deprecated
  public void setHeaderBackgroundResource(@DrawableRes int resId) {
    setHeaderBackground(resId);
  }

  public void setHeaderBackground(@DrawableRes int resId) {
    setHeaderBackground(ContextCompat.getDrawable(this, resId));
  }

  public void setHeaderBackground(@NonNull Drawable drawable) {
    ViewCompat.setBackground(headerContentLayout, drawable);
  }

  /**
   * Set the drawable to use for the content scrim from resources. Providing null will disable
   * the scrim functionality.
   *
   * @param drawable the drawable to display
   */
  public void setHeaderContentScrim(@NonNull Drawable drawable) {
    collapsingToolbar.setContentScrim(drawable);
  }

  public void setHeaderContentScrim(@DrawableRes int resId) {
    setHeaderContentScrim(ContextCompat.getDrawable(this, resId));
  }

  public void setHeaderTextColor(@ColorInt int color) {
    collapsingToolbar.setCollapsedTitleTextColor(color);
    slogan.setTextColor(color);
    version.setTextColor(color);
  }

  /**
   * Set the icon to use for the toolbar's navigation button.
   *
   * @param resId Resource ID of a drawable to set
   */
  public void setNavigationIcon(@DrawableRes int resId) {
    toolbar.setNavigationIcon(resId);
  }

  public void setNavigationIcon(@NonNull Drawable drawable) {
    toolbar.setNavigationIcon(drawable);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem menuItem) {
    if (menuItem.getItemId() == android.R.id.home) {
      onBackPressed();
    }
    return super.onOptionsItemSelected(menuItem);
  }

  public Toolbar getToolbar() {
    return toolbar;
  }

  public CollapsingToolbarLayout getCollapsingToolbar() {
    return collapsingToolbar;
  }

  public List<Object> getItems() {
    return items;
  }

  public MultiTypeAdapter getAdapter() {
    return adapter;
  }

  public TextView getSloganTextView() {
    return slogan;
  }

  public TextView getVersionTextView() {
    return version;
  }

  public void setOnRecommendationClickedListener(@Nullable OnRecommendationClickedListener listener) {
    this.onRecommendationClickedListener = listener;
  }

  public @Nullable OnRecommendationClickedListener getOnRecommendationClickedListener() {
    return onRecommendationClickedListener;
  }

  public void setOnContributorClickedListener(@Nullable OnContributorClickedListener listener) {
    this.onContributorClickedListener = listener;
  }

  public @Nullable OnContributorClickedListener getOnContributorClickedListener() {
    return onContributorClickedListener;
  }
}
