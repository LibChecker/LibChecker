package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

public interface IPackageManager extends IInterface {

  InstallSourceInfo getInstallSourceInfo(String packageName);

  InstallSourceInfo getInstallSourceInfo(String packageName, int callingUid);

  abstract class Stub extends Binder implements IPackageManager {

    public static IPackageManager asInterface(IBinder obj) {
      throw new UnsupportedOperationException();
    }
  }
}
