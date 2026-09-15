package com.absinthe.libchecker.domain.app.repository

class PackageListLoadException(cause: Exception) : RuntimeException("Unable to load the complete installed package list", cause)
