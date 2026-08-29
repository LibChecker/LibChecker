package com.absinthe.libchecker.domain.settings.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.settings.model.GetUpdatesAction
import com.absinthe.libchecker.domain.settings.model.GetUpdatesDialogState
import com.absinthe.libchecker.domain.settings.model.GetUpdatesItem
import com.absinthe.libchecker.ui.adapter.BindOnlyAdapter
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.BottomSheetScaffoldView
import com.google.android.material.button.MaterialButton

class GetUpdatesDialogView(context: Context) : BottomSheetScaffoldView(context) {

  private var onAction: (GetUpdatesAction) -> Unit = {}
  private val adapter = BindOnlyAdapter<GetUpdatesItem, MaterialButton>(
    viewFactory = { context ->
      MaterialButton(context).apply {
        layoutParams = LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      }
    },
    bindView = { item ->
      setIconResource(item.iconRes)
      text = item.text
      setOnClickListener { onAction(item.action) }
    }
  )

  private val recyclerView = RecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    setPadding(16.dp, 0, 16.dp, 16.dp)
    adapter = this@GetUpdatesDialogView.adapter
    layoutManager = LinearLayoutManager(context)
  }

  init {
    header.title.text = context.getString(R.string.settings_get_updates)
    addContentView(recyclerView)
  }

  fun bind(
    state: GetUpdatesDialogState,
    onAction: (GetUpdatesAction) -> Unit
  ) {
    this.onAction = onAction
    adapter.setList(state.items)
  }
}
