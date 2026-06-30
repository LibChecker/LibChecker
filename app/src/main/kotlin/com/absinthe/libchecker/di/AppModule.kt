package com.absinthe.libchecker.di

import com.absinthe.libchecker.database.LCDao
import com.absinthe.libchecker.database.LCDatabase
import com.absinthe.libchecker.database.LCRepository
import org.koin.dsl.module

val appModule = module {
  single<LCDatabase> { LCDatabase.getDatabase() }
  single<LCDao> { get<LCDatabase>().lcDao() }
  single<LCRepository> { LCRepository(get()) }
}
