package com.microsoft.appcenter.crashes;

import androidx.annotation.NonNull;

import com.microsoft.appcenter.AppCenterService;

public class Crashes implements AppCenterService {
  public static void trackError(@NonNull Throwable throwable) {
  }
}
