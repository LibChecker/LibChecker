package com.absinthe.libchecker.constant.librarymap

import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.LibChip

object ProviderLibMap {
    val MAP: HashMap<String, LibChip> = hashMapOf(
        Pair(
            "com.huawei.hms.update.provider.UpdateProvider",
            LibChip(R.drawable.ic_lib_huawei, "HMS Update")
        ),
        Pair(
            "com.huawei.updatesdk.fileprovider.UpdateSdkFileProvider",
            LibChip(R.drawable.ic_lib_huawei, "HMS Update")
        ),
        Pair(
            "com.huawei.hms.support.api.push.PushProvider",
            LibChip(R.drawable.ic_lib_huawei, "Huawei Push")
        )
    )
}