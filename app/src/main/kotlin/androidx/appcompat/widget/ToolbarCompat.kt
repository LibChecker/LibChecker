package androidx.appcompat.widget

import android.content.Context
import android.util.AttributeSet

class ToolbarCompat : Toolbar {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

  // Avoid java.lang.IllegalStateException: The specified child already has a parent. You must call removeView() on the child's parent first.
  override fun addChildrenForExpandedActionView() {
    runCatching {
      super.addChildrenForExpandedActionView()
    }
  }

  // Avoid java.lang.IllegalStateException: The specified child already has a parent. You must call removeView() on the child's parent first.
  override fun removeChildrenForExpandedActionView() {
    runCatching {
      super.removeChildrenForExpandedActionView()
    }
  }
}
