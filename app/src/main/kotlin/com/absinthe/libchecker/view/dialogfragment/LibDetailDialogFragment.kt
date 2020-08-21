package com.absinthe.libchecker.view.dialogfragment

import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.constant.LibType
import com.absinthe.libchecker.constant.NATIVE
import com.absinthe.libchecker.constant.librarymap.BaseMap
import com.absinthe.libchecker.view.detail.LibDetailView
import com.absinthe.libchecker.viewmodel.DetailViewModel

const val EXTRA_LIB_NAME = "EXTRA_LIB_NAME"
const val EXTRA_LIB_TYPE = "EXTRA_LIB_TYPE"
const val EXTRA_REGEX_NAME = "EXTRA_REGEX_NAME"

const val VF_CHILD_DETAIL = 0
const val VF_CHILD_LOADING = 1
const val VF_CHILD_FAILED = 2

class LibDetailDialogFragment : DialogFragment() {

    private val dialogView by lazy { LibDetailView(requireContext()) }
    private val libName by lazy { arguments?.getString(EXTRA_LIB_NAME) ?: "" }
    private val type by lazy { arguments?.getInt(EXTRA_LIB_TYPE) ?: NATIVE }
    private val regexName by lazy { arguments?.getString(EXTRA_REGEX_NAME) }
    private val viewModel by viewModels<DetailViewModel>()

    private fun List<String>.toContributorsString(): String {
        return this.joinToString(separator = ", ")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView.binding.apply {
            vfContainer.displayedChild = VF_CHILD_LOADING
            tvLibName.text = libName
            ivIcon.load(BaseMap.getMap(type).getMap()[libName]?.iconRes ?: R.drawable.ic_logo) {
                crossfade(true)
                placeholder(R.drawable.ic_logo)
            }
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

        return AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
    }

    override fun onStart() {
        super.onStart()

        viewModel.detailBean.observe(requireActivity(), {
            if (it != null) {
                dialogView.binding.apply {
                    tvLabelName.text = it.label
                    tvTeamName.text = it.team
                    tvContributorName.text = it.contributors.toContributorsString()
                    tvDescriptionName.text = it.description
                    tvRelativeName.apply {
                        isClickable = true
                        movementMethod = LinkMovementMethod.getInstance()
                        text = HtmlCompat.fromHtml("<a href='${it.relativeUrl}'> ${it.relativeUrl} </a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
                    }

                    vfContainer.displayedChild = VF_CHILD_DETAIL
                }
            } else {
                dialogView.binding.vfContainer.displayedChild = VF_CHILD_FAILED
            }
        })
        regexName?.let {
            viewModel.requestLibDetail(it, type, true)
        } ?: let {
            viewModel.requestLibDetail(libName, type)
        }
    }

    companion object {
        fun newInstance(
            libName: String,
            @LibType type: Int,
            regexName: String? = null
        ): LibDetailDialogFragment {
            return LibDetailDialogFragment()
                .apply {
                    arguments = Bundle().apply {
                        putString(EXTRA_LIB_NAME, libName)
                        putInt(EXTRA_LIB_TYPE, type)
                        putString(EXTRA_REGEX_NAME, regexName)
                    }
                }
        }
    }
}