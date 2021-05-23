package com.absinthe.libchecker.api

import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val BRANCH_MASTER = "master"
private const val WORKING_BRANCH = BRANCH_MASTER

object ApiManager {

    private const val GITHUB_ROOT_URL =
        "https://raw.githubusercontent.com/zhaobozhen/LibChecker-Rules/$WORKING_BRANCH/"
    private const val GITEE_ROOT_URL =
        "https://gitee.com/zhaobozhen/LibChecker-Rules/raw/$WORKING_BRANCH/"

    const val GITHUB_NEW_ISSUE_URL =
        "https://github.com/zhaobozhen/LibChecker-Rules/issues/new?labels=&template=library-name.md&title=%5BNew+Rule%5D"

    private val root
        get() = when (GlobalValues.repo) {
            Constants.REPO_GITHUB -> GITHUB_ROOT_URL
            Constants.REPO_GITEE -> GITEE_ROOT_URL
            else -> GITHUB_ROOT_URL
        }

    private val retrofit by lazy {
        val okhttpBuilder = OkHttpClient.Builder()
            .connectTimeout(30 * 1000, TimeUnit.MILLISECONDS)
            .readTimeout(30 * 1000, TimeUnit.MILLISECONDS)
            .writeTimeout(30 * 1000, TimeUnit.MILLISECONDS)
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .client(okhttpBuilder.build())
            .baseUrl(root)
            .build()
    }

    fun <T> create(service: Class<T>): T = retrofit.create(service)

    inline fun <reified T> create(): T = create(T::class.java)
}