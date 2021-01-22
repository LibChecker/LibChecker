package com.absinthe.libchecker.ui.fragment

import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.ui.main.IListContainer
import com.absinthe.libchecker.ui.main.MainActivity
import rikka.material.widget.BorderViewDelegate

abstract class BaseListControllerFragment<T : ViewBinding>(layoutId: Int) : BaseFragment<T>(layoutId), IListController {

    protected var borderDelegate: BorderViewDelegate? = null

    override fun onVisibilityChanged(visible: Boolean) {
        super.onVisibilityChanged(visible)
        if (visible) {
            if ((requireActivity() as IListContainer).controller != this) {
                (requireActivity() as IListContainer).controller = this@BaseListControllerFragment
                requireActivity().invalidateOptionsMenu()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        scheduleAppbarRaisingStatus()
    }

    override fun getAppBar() = (activity as MainActivity?)?.appBar

    override fun getBorderViewDelegate() = borderDelegate

    override fun scheduleAppbarRaisingStatus() {
        getAppBar()?.setRaised(!(getBorderViewDelegate()?.isShowingTopBorder ?: true))
    }
}