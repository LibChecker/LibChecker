package com.absinthe.libchecker.database.backup

import android.content.SharedPreferences
import java.nio.ByteBuffer
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.GCMParameterSpec

/**
 * Encryption / Decryption service using the AES algorithm
 * example for nullbeans.com
 * https:// nullbeans.com/how-to-encrypt-decrypt-files-byte-arrays-in-java-using-aes-gcm/#Generating_an_AES_key
 */
class AESEncryptionManager {

  private val aesEncryptionHelper = AESEncryptionHelper()

  /**
   * This method will encrypt the given data
   * @param sharedPref : the sharedPref, to fetch the key
   * @param data : the data that will be encrypted
   * @return Encrypted data in a byte array
   */
  @Throws(
    NoSuchPaddingException::class,
    NoSuchAlgorithmException::class,
    InvalidAlgorithmParameterException::class,
    InvalidKeyException::class,
    BadPaddingException::class,
    IllegalBlockSizeException::class,
    InvalidKeySpecException::class
  )
  fun encryptData(
    sharedPref: SharedPreferences,
    encryptPassword: String?,
    data: ByteArray
  ): ByteArray {
    // Prepare the nonce
    val secureRandom = SecureRandom()

    // Noonce should be 12 bytes
    val iv = ByteArray(12)
    secureRandom.nextBytes(iv)

    // Prepare your key/password
    val secretKey = if (encryptPassword != null) {
      aesEncryptionHelper.getSecretKeyWithCustomPw(encryptPassword, iv)
    } else {
      aesEncryptionHelper.getSecretKey(sharedPref, iv)
    }

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val parameterSpec = GCMParameterSpec(128, iv)

    // Encryption mode on!
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

    // Encrypt the data
    val encryptedData = cipher.doFinal(data)

    // Concatenate everything and return the final data
    val byteBuffer = ByteBuffer.allocate(4 + iv.size + encryptedData.size)
    byteBuffer.putInt(iv.size)
    byteBuffer.put(iv)
    byteBuffer.put(encryptedData)
    return byteBuffer.array()
  }

  /**
   * This method will decrypt the given data
   * @param sharedPref : the sharedPref, to fetch the key
   * @param encryptedData : the data that will be decrypted
   * @return decrypted data in a byte array
   */
  @Throws(
    NoSuchPaddingException::class,
    NoSuchAlgorithmException::class,
    InvalidAlgorithmParameterException::class,
    InvalidKeyException::class,
    BadPaddingException::class,
    IllegalBlockSizeException::class,
    InvalidKeySpecException::class
  )
  fun decryptData(
    sharedPref: SharedPreferences,
    encryptPassword: String?,
    encryptedData: ByteArray
  ): ByteArray {
    // Wrap the data into a byte buffer to ease the reading process
    val byteBuffer = ByteBuffer.wrap(encryptedData)
    val noonceSize = byteBuffer.int

    // Make sure that the file was encrypted properly
    require(!(noonceSize < 12 || noonceSize >= 16)) { "Nonce size is incorrect. Make sure that the incoming data is an AES encrypted file." }
    val iv = ByteArray(noonceSize)
    byteBuffer[iv]

    // Prepare your key/password
    val secretKey = if (encryptPassword != null) {
      aesEncryptionHelper.getSecretKeyWithCustomPw(encryptPassword, iv)
    } else {
      aesEncryptionHelper.getSecretKey(sharedPref, iv)
    }

    // get the rest of encrypted data
    val cipherBytes = ByteArray(byteBuffer.remaining())
    byteBuffer[cipherBytes]
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val parameterSpec = GCMParameterSpec(128, iv)

    // Encryption mode on!
    cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

    // Encrypt the data
    return cipher.doFinal(cipherBytes)
  }
}
