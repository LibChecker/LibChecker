package androidx.appcompat.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import timber.log.Timber

/**
 * Fatal Exception: java.lang.IllegalStateException
 * The specified child already has a parent. You must call removeView() on the child's parent first.
 * android.view.ViewGroup.addViewInner (ViewGroup.java:5601)
 * android.view.ViewGroup.addView (ViewGroup.java:5387)
 * android.view.ViewGroup.addView (ViewGroup.java:5327)
 * android.view.ViewGroup.addView (ViewGroup.java:5299)
 * androidx.appcompat.widget.Toolbar.addChildrenForExpandedActionView (Toolbar.java:2388)
 * androidx.appcompat.widget.Toolbar$ExpandedActionViewMenuPresenter.collapseItemActionView (Toolbar.java:2783)
 * androidx.appcompat.view.menu.MenuBuilder.collapseItemActionView (MenuBuilder.java:1384)
 * androidx.appcompat.view.menu.MenuBuilder.clear (MenuBuilder.java:607)
 * androidx.appcompat.app.ToolbarActionBar.populateOptionsMenu (ToolbarActionBar.java:457)
 * androidx.appcompat.app.ToolbarActionBar$1.run (ToolbarActionBar.java:58)
 */
class ToolbarFix @JvmOverloads constructor(
  context: Context, attrs: AttributeSet? = null
) : Toolbar(context, attrs) {
  private val hiddenViews by lazy {
    Toolbar::class.java.getDeclaredField("mHiddenViews").apply {
      isAccessible = true
    }.get(this) as? ArrayList<*>
  }

  override fun addChildrenForExpandedActionView() {
    Timber.d("ToolbarFix: addChildrenForExpandedActionView")

    hiddenViews?.forEach { view ->
      if (view is View) {
        Timber.d("ToolbarFix: Removing hidden view: ${view.javaClass.simpleName}, hasParent=${view.parent != null}")
        (view.parent as? ViewGroup)?.removeView(view)
      }
    }
    super.addChildrenForExpandedActionView()
  }
}
