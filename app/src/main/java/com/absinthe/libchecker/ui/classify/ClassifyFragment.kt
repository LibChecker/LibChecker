package com.absinthe.libchecker.ui.classify

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.absinthe.libchecker.R
import java.lang.StringBuilder

class ClassifyFragment : Fragment() {

    private lateinit var homeViewModel: ClassifyViewModel
    lateinit var textView: TextView

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
                ViewModelProviders.of(this).get(ClassifyViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_classify, container, false)
        textView = root.findViewById(R.id.text_home)
        homeViewModel.text.observe(viewLifecycleOwner, Observer {
//            textView.text = it
        })

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val manager = activity?.packageManager
        val list = manager?.getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)

        if (list != null) {
            val sb = StringBuilder()
            for (info in list) {
                info.sourceDir
                if (info.nativeLibraryDir.substringAfterLast('/') != "arm64")
                sb.append(info.nativeLibraryDir.substringAfterLast('/'))
                    .append("//")
            }
            textView.text = sb.toString()
        }
    }
}
