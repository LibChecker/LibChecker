package com.absinthe.libchecker.domain.app.detail.model

data class ProcessBarRenderState(
  val items: List<ProcessBarItemRenderState> = emptyList(),
  val visible: Boolean = false
) {

  companion object {
    fun create(
      processColors: Map<String, Int>,
      selectedProcess: String?,
      processMode: Boolean,
      hasNonGrantedPermissions: Boolean?
    ): ProcessBarRenderState {
      val items = processColors.map { (process, color) ->
        ProcessBarItemRenderState(
          process = process,
          color = color,
          selected = process == selectedProcess
        )
      }
      return ProcessBarRenderState(
        items = items,
        visible = items.isNotEmpty() && (processMode || hasNonGrantedPermissions != false)
      )
    }
  }
}

data class ProcessBarItemRenderState(
  val process: String,
  val color: Int,
  val selected: Boolean
)

sealed interface ProcessBarAction {
  data class ProcessSelectionChanged(
    val process: String?
  ) : ProcessBarAction
}
