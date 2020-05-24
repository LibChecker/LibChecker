package com.absinthe.libchecker.view

import android.content.DialogInterface
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.DialogStack

open class LCDialogFragment : DialogFragment() {

    private var isDismissParent = false
    private var mListener: OnDismissListener? = null

    override fun show(manager: FragmentManager, tag: String?) {
        dialog?.window?.setWindowAnimations(R.style.Animation_Material_Dialog)

        super.show(manager, tag)
        DialogStack.push(this)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        DialogStack.pop()
        if (isDismissParent) {
            DialogStack.pop()
        }
        mListener?.onDismiss()

    }

    protected fun setWrapOnDismissListener(listener: OnDismissListener?) {
        mListener = listener
    }

    protected fun setDismissParent(flag: Boolean) {
        isDismissParent = flag
    }

    interface OnDismissListener {
        fun onDismiss()
    }
}