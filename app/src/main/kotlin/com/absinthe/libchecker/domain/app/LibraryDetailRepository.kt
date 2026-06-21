package com.absinthe.libchecker.domain.app

import com.absinthe.libchecker.api.bean.LibDetailBean

interface LibraryDetailRepository {
  suspend fun requestLibraryDetail(categoryDir: String, libPath: String): LibDetailBean

  suspend fun getRepoPushedAt(owner: String, repo: String): String?
}
