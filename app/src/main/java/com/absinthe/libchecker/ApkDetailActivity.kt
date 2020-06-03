package com.absinthe.libchecker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.absinthe.libchecker.ktx.logd
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ApkDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apk_detail)

        intent.data?.scheme?.let {
            if (it == "content") {
                intent.data?.let {uri->
                    logd(uri.toString())
                    val zipFile = ZipFile(File(uri.toString()))
                    val entries = zipFile.entries()

                    var next: ZipEntry

                    while (entries.hasMoreElements()) {
                        next = entries.nextElement()

                        logd(next.name)
                    }
                    zipFile.close()
                }
            }
        }
    }
}