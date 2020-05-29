package com.absinthe.libchecker.view.dialogfragment

import android.app.Dialog
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.librarymap.NativeLibMap
import com.absinthe.libchecker.view.LCDialogFragment
import com.absinthe.libchecker.view.LibDetailView
import com.absinthe.libchecker.viewmodel.DetailViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

const val EXTRA_LIB_NAME = "EXTRA_LIB_NAME"

const val VF_CHILD_DETAIL = 0
const val VF_CHILD_LOADING = 1
const val VF_CHILD_FAILED = 2

class LibDetailDialogFragment : LCDialogFragment() {

    private lateinit var dialogView: LibDetailView
    private val libName by lazy { arguments?.getString(EXTRA_LIB_NAME) ?: "" }

    private fun List<String>.toContributorsString(): String {
        return this.joinToString(separator = ", ")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView =
            LibDetailView(requireContext())
        dialogView.binding.apply {
            vfContainer.displayedChild = VF_CHILD_LOADING
            tvLibName.text = libName
            ivIcon.setImageResource(NativeLibMap.MAP[libName]?.iconRes ?: R.drawable.ic_logo)
            tvCreateIssue.apply {
                isClickable = true
                movementMethod = LinkMovementMethod.getInstance()
                text = Html.fromHtml(
                    "<a href='${ApiManager.GITHUB_NEW_ISSUE_URL}'> ${resources.getText(
                        R.string.create_an_issue
                    )} </a>"
                )
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
    }

    override fun onStart() {
        super.onStart()

        val viewModel = ViewModelProvider(this).get(DetailViewModel::class.java)
        viewModel.detailBean.observe(requireActivity(), Observer {
            if (it != null) {
                dialogView.binding.apply {
                    GlobalValues.config.apply {

                        if (showLibName) {
                            tvLabelName.text = it.label
                        } else {
                            llLabel.visibility = View.GONE
                        }

                        if (showTeamName) {
                            tvTeamName.text = it.team
                        } else {
                            llTeam.visibility = View.GONE
                        }

                        if (showContributor) {
                            tvContributorName.text = it.contributors.toContributorsString()
                        } else {
                            llContributor.visibility = View.GONE
                        }

                        if (showLibDescription) {
                            tvDescriptionName.text = it.description
                        } else {
                            llDescription.visibility = View.GONE
                        }

                        if (showRelativeUrl) {
                            tvRelativeName.apply {
                                isClickable = true
                                movementMethod = LinkMovementMethod.getInstance()
                                text =
                                    Html.fromHtml("<a href='${it.relativeUrl}'> ${it.relativeUrl} </a>")
                            }
                        } else {
                            llRelativeUrl.visibility = View.GONE
                        }
                    }

                    vfContainer.displayedChild = VF_CHILD_DETAIL
                }
            } else {
                dialogView.binding.vfContainer.displayedChild = VF_CHILD_FAILED
            }
        })
        viewModel.requestNativeLibDetail(libName)
    }

    companion object {
        fun newInstance(libName: String): LibDetailDialogFragment {
            return LibDetailDialogFragment()
                .apply {
                    arguments = Bundle().apply {
                        putString(EXTRA_LIB_NAME, libName)
                    }
                }
        }
    }
}