package com.absinthe.libchecker.utils

import android.app.Dialog
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.absinthe.libchecker.BuildConfig
import java.util.*

/**
 * Created by Absinthe at 2020/1/13
 *
 *
 * Dialog Stack
 *
 *
 * Make it display unique Dialog at the same time
 * to make app delegate.
 */
object DialogStack {

    private val stack: Stack<Any> = Stack()
    private const val tag = "DialogStack"
    private val isPrintStack = BuildConfig.DEBUG

    fun push(dialog: Any) {
        printStack()
        Log.i(tag, "Push Start")

        if (dialog !is Dialog && dialog !is DialogFragment) {
            return
        }

        if (stack.empty()) {
            stack.push(dialog)
        } else {
            when (val peekObject = stack.peek()) {
                is Dialog -> {
                    peekObject.hide()
                }
                is DialogFragment -> {
                    peekObject.dialog?.hide()
                }
            }
            stack.push(dialog)
        }

        dialog.apply {
            if (this is Dialog) {
                show()
            }
        }

        Log.i(tag, "Push End")
        printStack()
    }

    fun pop() {
        printStack()
        Log.i(tag, "Pop Start")

        if (stack.empty()) {
            return
        }

        stack.peek()?.let {
            try {
                if (it is Dialog) {
                    it.dismiss()
                } else if (it is DialogFragment) {
                    it.dismiss()
                }
                stack.pop()

                if (stack.isNotEmpty()) {
                    stack.peek()?.let { peek ->
                        if (peek is Dialog) {
                            peek.show()
                        } else if (peek is DialogFragment) {
                            peek.dialog?.show()
                        }
                    } ?: let {
                        stack.pop()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Log.i(tag, "Pop End")
        printStack()
    }

    private fun printStack() {
        if (isPrintStack) {
            Log.i(tag, "DialogStack:")

            for (obj in stack) {
                Log.i(tag, obj.javaClass.toString())
            }
            Log.i(tag, "--------------------------------------")
        }
    }
}