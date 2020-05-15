package com.absinthe.libchecker.view.dialogfragment

import android.app.Dialog
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.absinthe.libchecker.view.LCDialogFragment
import com.absinthe.libchecker.view.LibDetailView
import com.absinthe.libchecker.viewmodel.DetailViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LibDetailDialogFragment : LCDialogFragment() {

    private lateinit var dialogView: LibDetailView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView =
            LibDetailView(requireContext())

        return MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
    }

    override fun onStart() {
        super.onStart()

        val viewModel = ViewModelProvider(this).get(DetailViewModel::class.java)
        viewModel.detailBean.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                dialogView.binding.apply {
                    tvLibName.text = "libizuko.so"
                    tvLabelName.text = it.label
                    tvContributorName.text = it.contributors.toString()
                    tvDescriptionName.text = it.description
                    tvRelativeName.text = it.relativeUrl

                    vfContainer.displayedChild = 1
                }
            } else {

            }
        })
        viewModel.requestNativeLibDetail("libizuko.so")
    }
}