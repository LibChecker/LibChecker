package com.absinthe.libchecker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

open class BaseNavFragment : Fragment() {
    protected var isNavigationViewInit = false//记录是否已经初始化过一次视图
    private var lastView: View? = null//记录上次创建的view

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //如果fragment的view已经创建则不再重新创建
        if (lastView == null) {
            lastView = super.onCreateView(inflater, container, savedInstanceState)
        }
        return lastView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if(!isNavigationViewInit){//初始化过视图则不再进行view和data初始化
            super.onViewCreated(view, savedInstanceState)
            isNavigationViewInit = true
        }
    }
}