package android.content.pm

import android.os.Binder
import android.os.IBinder
import android.os.IInterface

interface IPackageManager : IInterface {

  fun getInstallSourceInfo(packageName: String): InstallSourceInfo?

  fun getInstallSourceInfo(packageName: String, callingUid: Int): InstallSourceInfo?

  abstract class Stub : Binder(), IPackageManager {

    companion object {
      @JvmStatic
      fun asInterface(obj: IBinder): IPackageManager? {
        throw UnsupportedOperationException()
      }
    }
  }
}