package com.microsoft.appcenter;

import android.app.Application;

public class AppCenter {
  @SafeVarargs
  public static void start(Application application, String appSecret, Class<? extends AppCenterService>... services) {
  }
}
