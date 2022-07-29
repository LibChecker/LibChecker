package com.microsoft.appcenter.analytics;

import com.microsoft.appcenter.AppCenterService;
import java.util.Map;

public class Analytics extends AppCenterService {
  public static void trackEvent(String name, Map<String, String> properties, int flags) {
  }

  public static void trackEvent(String name, EventProperties properties) {
  }

  public static void trackEvent(String name, EventProperties properties, int flags) {
  }
}
