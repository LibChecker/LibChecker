package com.absinthe.libchecker.ui.base

interface IListControllerHost {
  fun setListController(controller: IListController)
  fun clearListController(controller: IListController)
  fun isCurrentListController(controller: IListController): Boolean
}
