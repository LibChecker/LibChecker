package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

import androidx.annotation.RequiresApi;

public interface IPackageManager extends IInterface {

  ParceledListSlice<PackageInfo> getInstalledPackages(int flags, int userId);

  @RequiresApi(33)
  ParceledListSlice<PackageInfo> getInstalledPackages(long flags, int userId);

  InstallSourceInfo getInstallSourceInfo(String packageName);

  @RequiresApi(34)
  InstallSourceInfo getInstallSourceInfo(String packageName, int callingUid);

  abstract class Stub extends Binder implements IPackageManager {

    public static IPackageManager asInterface(IBinder obj) {
      throw new UnsupportedOperationException();
    }
  }
}
