package com.absinthe.libchecker.utils.extensions

import com.absinthe.libchecker.utils.HashUtils
import java.io.File

fun File.md5() = HashUtils.md5(this.readBytes())

fun File.sha1() = HashUtils.sha1(this.readBytes())

fun File.sha256() = HashUtils.sha256(this.readBytes())

fun File.sha512() = HashUtils.sha512(this.readBytes())
