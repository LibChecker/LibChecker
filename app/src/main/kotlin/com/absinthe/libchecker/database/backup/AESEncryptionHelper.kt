package com.absinthe.libchecker.database.backup

import android.annotation.SuppressLint
import android.content.SharedPreferences
import java.io.File
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 *  MIT License
 *
 *  Copyright (c) 2022 Raphael Ebner
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
class AESEncryptionHelper {

  companion object {
    private const val BACKUP_SECRET_KEY = "backupsecretkey"
    private const val TAG = "debug_AESEncryptionHelper"
  }

  /**
   * This method will convert a file to ByteArray
   * @param file : the Path where the file is located
   * @return ByteArray of the file
   */
  @Throws(Exception::class)
  fun readFile(file: File): ByteArray {
    return file.readBytes()
  }

  /**
   * This method will convert a ByteArray to a file, and saves it to the path
   * @param fileData : the ByteArray
   * @param file : the path where the ByteArray should be saved
   */
  @Throws(Exception::class)
  fun saveFile(fileData: ByteArray, file: File) {
    file.outputStream().buffered().use { it.write(fileData) }
  }

  /**
   * This method will convert a random password, saved in sharedPreferences to a SecretKey
   * @param sharedPref : the sharedPref, to fetch / save the key
   * @param iv : the encryption nonce
   * @return SecretKey
   */
  @SuppressLint("ApplySharedPref")
  fun getSecretKey(sharedPref: SharedPreferences, iv: ByteArray): SecretKey {
    // get key: String from sharedpref
    var password = sharedPref.getString(BACKUP_SECRET_KEY, null)

    // If no key is stored in shared pref, create one and save it
    if (password == null) {
      // generate random string
      val stringLength = 15
      val charset = ('a'..'z') + ('A'..'Z') + ('1'..'9')
      password = (1..stringLength)
        .map { charset.random() }
        .joinToString("")

      val secretKey = generateSecretKey(password, iv)
      // the key can be saved plain, because i am using EncryptedSharedPreferences
      val editor = sharedPref.edit()
      editor.putString(BACKUP_SECRET_KEY, password)
      // I use .commit because when using .apply the needed app restart is faster then apply and the preferences wont be saved
      editor.commit()

      return secretKey
    }

    // generate secretKey, and return it
    return generateSecretKey(password, iv)
  }

  /**
   * This method will convert a custom password to a SecretKey
   * @param encryptPassword : the custom user password as String
   * @param iv : the encryption nonce
   * @return SecretKey
   */
  fun getSecretKeyWithCustomPw(encryptPassword: String, iv: ByteArray): SecretKey {
    // generate secretKey, and return it
    return generateSecretKey(encryptPassword, iv)
  }

  /**
   * Function to generate a 128 bit key from the given password and iv
   * @param password
   * @param iv
   * @return Secret key
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeySpecException
   */
  @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
  private fun generateSecretKey(password: String, iv: ByteArray?): SecretKey {
    // convert random string to secretKey
    val spec: KeySpec = PBEKeySpec(password.toCharArray(), iv, 65536, 128) //  AES-128
    val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val key = secretKeyFactory.generateSecret(spec).encoded
    return SecretKeySpec(key, "AES")
  }
}
