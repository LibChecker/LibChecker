package com.absinthe.libchecker.view.dialogfragment

import android.app.Dialog
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import androidx.core.text.HtmlCompat
import androidx.core.view.isGone
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.librarymap.BaseMap
import com.absinthe.libchecker.recyclerview.LibStringAdapter
import com.absinthe.libchecker.view.LCDialogFragment
import com.absinthe.libchecker.view.detail.LibDetailView
import com.absinthe.libchecker.viewmodel.DetailViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

const val EXTRA_LIB_NAME = "EXTRA_LIB_NAME"
const val EXTRA_LIB_TYPE = "EXTRA_LIB_TYPE"
const val EXTRA_REGEX_NAME = "EXTRA_REGEX_NAME"

const val VF_CHILD_DETAIL = 0
const val VF_CHILD_LOADING = 1
const val VF_CHILD_FAILED = 2

class LibDetailDialogFragment : LCDialogFragment() {

    private val dialogView by lazy {
        LibDetailView(
            requireContext()
        )
    }
    private val libName by lazy { arguments?.getString(EXTRA_LIB_NAME) ?: "" }
    private val type by lazy { arguments?.getSerializable(EXTRA_LIB_TYPE) as LibStringAdapter.Mode }
    private val regexName by lazy { arguments?.getString(EXTRA_REGEX_NAME) }

    private fun List<String>.toContributorsString(): String {
        return this.joinToString(separator = ", ")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView.binding.apply {
            vfContainer.displayedChild = VF_CHILD_LOADING
            tvLibName.text = libName
            ivIcon.setImageResource(
                BaseMap.getMap(type).getMap()[libName]?.iconRes ?: R.drawable.ic_logo
            )
            tvCreateIssue.apply {
                isClickable = true
                movementMethod = LinkMovementMethod.getInstance()
                text = HtmlCompat.fromHtml(
                    "<a href='${ApiManager.GITHUB_NEW_ISSUE_URL}'> ${resources.getText(
                        R.string.create_an_issue
                    )} </a>"
                    , HtmlCompat.FROM_HTML_MODE_LEGACY
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
                        llLabel.isGone = !showLibName
                        llTeam.isGone = !showTeamName
                        llContributor.isGone = !showContributor
                        llDescription.isGone = !showLibDescription
                        llRelativeUrl.isGone = !showRelativeUrl

                        tvLabelName.text = it.label
                        tvTeamName.text = it.team
                        tvContributorName.text = it.contributors.toContributorsString()
                        tvDescriptionName.text = it.description
                        tvRelativeName.apply {
                            isClickable = true
                            movementMethod = LinkMovementMethod.getInstance()
                            text =
                                Html.fromHtml("<a href='${it.relativeUrl}'> ${it.relativeUrl} </a>")
                        }
                    }

                    vfContainer.displayedChild = VF_CHILD_DETAIL
                }
            } else {
                dialogView.binding.vfContainer.displayedChild = VF_CHILD_FAILED
            }
        })
        regexName?.let {
            viewModel.requestLibDetail(regexName!!, type, true)
        } ?: let {
            viewModel.requestLibDetail(libName, type)
        }
    }

    companion object {
        fun newInstance(
            libName: String,
            mode: LibStringAdapter.Mode,
            regexName: String? = null
        ): LibDetailDialogFragment {
            return LibDetailDialogFragment()
                .apply {
                    arguments = Bundle().apply {
                        putString(EXTRA_LIB_NAME, libName)
                        putSerializable(EXTRA_LIB_TYPE, mode)
                        putString(EXTRA_REGEX_NAME, regexName)
                    }
                }
        }
    }
}