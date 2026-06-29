package com.absinthe.libchecker.di

import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.database.Repositories
import org.koin.dsl.module

val appModule = module {
  single<LCRepository> { Repositories.lcRepository }
}
