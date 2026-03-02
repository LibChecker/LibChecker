package com.absinthe.libchecker.annotation

import androidx.annotation.IntDef

const val ET_NOT_SET = -2
const val ET_NOT_ELF = -1
const val ET_NONE = 0
const val ET_REL = 1
const val ET_EXEC = 2
const val ET_DYN = 3
const val ET_CORE = 4
const val ET_LOPROC = 0xff00
const val ET_HIPROC = 0xffff

@IntDef(ET_NOT_SET, ET_NOT_ELF, ET_NONE, ET_REL, ET_EXEC, ET_DYN, ET_CORE, ET_LOPROC, ET_HIPROC)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ElfType
