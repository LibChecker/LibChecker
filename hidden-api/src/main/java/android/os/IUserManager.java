package android.os;

import android.content.pm.UserInfo;

import java.util.List;

public interface IUserManager extends IInterface {
  /**
   * Get users
   * @param excludePartial default true
   * @param excludeDying default false
   * @param excludePreCreated default true
   * @return list of UserInfo
   */
  List<UserInfo> getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated);

  abstract class Stub extends Binder implements IUserManager {

    public static IUserManager asInterface(IBinder obj) {
      throw new UnsupportedOperationException();
    }
  }
}
