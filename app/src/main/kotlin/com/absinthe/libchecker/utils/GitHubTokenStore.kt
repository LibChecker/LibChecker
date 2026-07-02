package com.absinthe.libchecker.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.absinthe.libchecker.LibCheckerApp
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import timber.log.Timber

object GitHubTokenStore {

  private const val PREF_NAME = "github_api_token_store"
  private const val PREF_TOKEN = "github_api_token"
  private const val PREF_TOKEN_IV = "github_api_token_iv"
  private const val PREF_TOKEN_CIPHERTEXT = "github_api_token_ciphertext"
  private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
  private const val KEY_ALIAS = "libchecker_github_api_token"
  private const val TRANSFORMATION = "AES/GCM/NoPadding"
  private const val GCM_TAG_LENGTH_BITS = 128

  private val preferences
    get() = LibCheckerApp.app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

  @Synchronized
  fun get(): String {
    val iv = preferences.getString(PREF_TOKEN_IV, null) ?: return String()
    val ciphertext = preferences.getString(PREF_TOKEN_CIPHERTEXT, null) ?: return String()

    return runCatching {
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(
        Cipher.DECRYPT_MODE,
        getOrCreateKey(),
        GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(iv, Base64.NO_WRAP))
      )
      String(
        cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)),
        Charsets.UTF_8
      ).trim()
    }.onFailure {
      Timber.w(it, "Failed to read GitHub API token.")
    }.getOrDefault(String())
  }

  @Synchronized
  fun set(token: String): Boolean {
    val trimmedToken = token.trim()
    if (trimmedToken.isEmpty()) {
      return clear()
    }

    return runCatching {
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
      val ciphertext = cipher.doFinal(trimmedToken.toByteArray(Charsets.UTF_8))

      preferences.edit {
        remove(PREF_TOKEN)
        putString(PREF_TOKEN_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
        putString(PREF_TOKEN_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
      }
    }.onFailure {
      Timber.w(it, "Failed to save GitHub API token.")
    }.isSuccess
  }

  @Synchronized
  fun clear(): Boolean {
    return runCatching {
      preferences.edit {
        remove(PREF_TOKEN)
        remove(PREF_TOKEN_IV)
        remove(PREF_TOKEN_CIPHERTEXT)
      }
    }.onFailure {
      Timber.w(it, "Failed to clear GitHub API token.")
    }.isSuccess
  }

  private fun getOrCreateKey(): SecretKey {
    val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
      load(null)
    }
    val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    if (existingKey != null) {
      return existingKey
    }

    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
    keyGenerator.init(
      KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .build()
    )
    return keyGenerator.generateKey()
  }
}
