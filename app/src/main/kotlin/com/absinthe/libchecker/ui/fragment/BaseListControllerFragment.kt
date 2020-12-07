package com.absinthe.libchecker.ui.fragment

import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.ui.main.IListContainer

abstract class BaseListControllerFragment<T : ViewBinding>(layoutId: Int) : BaseFragment<T>(layoutId), IListController {

    override fun onVisibilityChanged(visible: Boolean) {
        super.onVisibilityChanged(visible)
        if (visible) {
            if ((requireActivity() as IListContainer).controller != this) {
                (requireActivity() as IListContainer).controller = this@BaseListControllerFragment
                requireActivity().invalidateOptionsMenu()
            }
        }
    }
}