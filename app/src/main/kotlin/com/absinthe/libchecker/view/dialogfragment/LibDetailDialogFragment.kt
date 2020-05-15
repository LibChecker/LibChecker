package com.absinthe.libchecker.view.dialogfragment

import android.app.Dialog
import android.os.Bundle
import com.absinthe.libchecker.view.LCDialogFragment
import com.absinthe.libchecker.view.LibDetailView
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

}