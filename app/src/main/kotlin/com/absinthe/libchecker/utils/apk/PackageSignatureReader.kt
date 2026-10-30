package com.absinthe.libchecker.utils.apk

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.content.res.Configuration
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.apk.ApkSignatureSchemeDetector
import com.absinthe.libchecker.utils.extensions.md5
import com.absinthe.libchecker.utils.extensions.sha1
import com.absinthe.libchecker.utils.extensions.sha256
import com.absinthe.libchecker.utils.extensions.toHexString
import java.io.File
import java.security.interfaces.DSAPublicKey
import java.security.interfaces.RSAPublicKey
import java.text.DateFormat
import javax.security.cert.X509Certificate
import okio.source
import timber.log.Timber

object PackageSignatureReader {
  fun describeSignature(
    context: Context,
    dateFormat: DateFormat,
    signature: Signature,
    signatureSchemes: List<String>
  ): LibStringItem {
    val bytes = signature.toByteArray()
    val certificate = X509Certificate.getInstance(bytes)
    val serialNumber = "0x${certificate.serialNumber.toString(16)}"
    val source = buildString {
      fun field(label: Int, value: Any?) {
        append(context.getString(label))
        append(":")
        appendLine(value)
      }

      field(R.string.signature_scheme_version, signatureSchemes.joinToString(", "))
      field(R.string.signature_version, certificate.version + 1)
      field(R.string.signature_serial_number, "${certificate.serialNumber}($serialNumber)")
      field(R.string.signature_issuer, certificate.issuerDN)
      field(R.string.signature_subject, certificate.subjectDN)
      field(R.string.signature_validity_not_before, dateFormat.format(certificate.notBefore))
      field(R.string.signature_validity_not_after, dateFormat.format(certificate.notAfter))
      field(R.string.signature_public_key_format, certificate.publicKey.format)
      field(R.string.signature_public_key_algorithm, certificate.publicKey.algorithm)
      when (val key = certificate.publicKey) {
        is RSAPublicKey -> {
          field(R.string.signature_public_key_exponent, "${key.publicExponent}(0x${key.publicExponent.toString(16)})")
          field(R.string.signature_public_key_modulus_size, key.modulus.toString(2).length)
          field(R.string.signature_public_key_modulus, key.modulus.toByteArray().toHexString(":"))
        }

        is DSAPublicKey -> {
          field(R.string.signature_public_key_y, key.y)
        }

        else -> {
          field(R.string.signature_public_key_type, key.javaClass.simpleName)
        }
      }
      field(R.string.signature_algorithm_name, certificate.sigAlgName)
      field(R.string.signature_algorithm_oid, certificate.sigAlgOID)
      field(R.string.signature_md5, bytes.md5(":"))
      field(R.string.signature_sha1, bytes.sha1(":"))
      field(R.string.signature_sha256, bytes.sha256(":"))
      append(context.getString(R.string.signature_char_string))
      append(":")
      append(signature.toCharsString())
    }
    return LibStringItem(serialNumber, 0, source, null)
  }

  /**
   * Get signatures of an app
   * @param context Context
   * @return List of LibStringItem
   */
  fun PackageInfo.getSignatures(context: Context): Sequence<LibStringItem> {
    val locale = GlobalValues.locale
    val localedContext = context.createConfigurationContext(
      Configuration(context.resources.configuration).apply {
        setLocale(locale)
      }
    )
    val dateFormat = DateFormat.getDateTimeInstance(
      DateFormat.LONG,
      DateFormat.LONG,
      locale
    )
    val signatureArray = if (OsUtils.atLeastP() && signingInfo != null) {
      if (signingInfo!!.hasMultipleSigners()) {
        signingInfo!!.apkContentsSigners
      } else {
        signingInfo!!.signingCertificateHistory
      }
    } else {
      @Suppress("DEPRECATION")
      this.signatures
    }
    val signatureSchemes = getSignatureSchemes()
    return signatureArray.orEmpty().asSequence().map {
      PackageSignatureReader.describeSignature(localedContext, dateFormat, it, signatureSchemes)
    }
  }

  fun PackageInfo.getSignatureSchemes(): List<String> {
    val sourceDir = applicationInfo?.sourceDir
    if (sourceDir.isNullOrBlank()) {
      Timber.w("Failed to get signature schemes for package without sourceDir: $packageName")
      return emptyList()
    }

    return runCatching {
      ApkSignatureSchemeDetector.detect(File(sourceDir))
    }.getOrElse {
      Timber.w(it, "Failed to get signature schemes for package: $packageName")
      emptyList()
    }
  }
}
