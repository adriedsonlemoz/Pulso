package br.com.pulsobateria.integration

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/** Pin de certificado(s) instalado(s), sem consulta irrestrita a aplicativos. */
object TrustedPartner {
 @Suppress("DEPRECATION")
 fun signerFingerprint(context: Context, packageName: String): String? {
  val pm = context.packageManager
  return runCatching {
   val signatures = if (Build.VERSION.SDK_INT >= 28) {
    pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners
   } else pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
   signatures?.takeIf { it.isNotEmpty() }?.map { signature ->
    MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
     .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
   }?.sorted()?.joinToString(",")
  }.getOrNull()
 }
}
