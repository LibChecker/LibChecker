package com.absinthe.libchecker.ui.fragment.applist

import android.os.Bundle
import android.system.ErrnoException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.absinthe.libchecker.databinding.FragmentManifestAnalysisBinding
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.view.EXTRA_PKG_NAME
import com.absinthe.libchecker.viewmodel.DetailViewModel
import java.io.*
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class ManifestAnalysisFragment : Fragment() {

    private val viewModel by lazy { ViewModelProvider(requireActivity()).get(DetailViewModel::class.java) }
    private val packageName by lazy { arguments?.getString(EXTRA_PKG_NAME) ?: "" }

    private lateinit var binding: FragmentManifestAnalysisBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentManifestAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initData(packageName)
    }

    private fun initData(packageName: String) {
        try {
            val packageInfo = PackageUtils.getPackageInfo(packageName)
            val file = File(packageInfo.applicationInfo.sourceDir)
            val zipFile = ZipFile(file)
            val inputStream = BufferedInputStream(FileInputStream(file))
            val zin = ZipInputStream(inputStream)

            var ze = zin.nextEntry
            val sb = StringBuilder()

            while (ze != null) {

                if (!ze.isDirectory && ze.name == "AndroidManifest.xml") {
                    val br = BufferedReader(InputStreamReader(zipFile.getInputStream(ze)))

                    var line = br.readLine()
                    while (line != null) {
                        sb.append(line)
                        line = br.readLine()
                    }
                    br.close()
                    break
                }
                ze = zin.nextEntry
            }
            zipFile.close()
            zin.closeEntry()

            binding.text.text = sb.toString()
        } catch (e: ErrnoException) {
            e.printStackTrace()
        }
    }

    companion object {
        fun newInstance(packageName: String): ManifestAnalysisFragment {
            return ManifestAnalysisFragment()
                .apply {
                    arguments = Bundle().apply {
                        putString(EXTRA_PKG_NAME, packageName)
                    }
                }
        }
    }
}