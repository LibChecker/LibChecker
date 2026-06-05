package com.absinthe.libchecker.ui.base

import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType

internal sealed interface IBinding<VB : ViewBinding> {
  val binding: VB

  fun <T : ViewBinding> inflateBinding(inflater: LayoutInflater): T {
    var clazz: Class<*>? = javaClass
    while (clazz != null) {
      val method = clazz.filterBindingMethod()
      if (method != null) {
        @Suppress("UNCHECKED_CAST")
        return method.invoke(null, inflater) as T
      }
      clazz = clazz.superclass
    }
    error("No Binding type argument found.")
  }

  private fun Class<*>.filterBindingMethod(): Method? {
    return (genericSuperclass as? ParameterizedType)?.actualTypeArguments
      ?.asSequence()
      ?.filterIsInstance<Class<*>>()
      ?.firstOrNull { it.simpleName.endsWith("Binding") }
      ?.getDeclaredMethod("inflate", LayoutInflater::class.java)
      ?.also { it.isAccessible = true }
  }
}
