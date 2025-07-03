package android.content.pm;

import android.content.IntentFilter;

import java.io.File;
import java.util.ArrayList;

public class PackageParser {

  public Package parsePackage(File packageFile, int flags) throws PackageParserException {
    throw new RuntimeException("Stub");
  }

  public static class PackageParserException extends Exception {
  }

  public static abstract class Component<II extends IntentInfo> {
    public final ArrayList<II> intents = null;
    public final String className = null;
  }

  public static abstract class IntentInfo extends IntentFilter {

  }

  public final static class Package {
    public final ArrayList<Activity> activities = new ArrayList<Activity>(0);
    public final ArrayList<Activity> receivers = new ArrayList<Activity>(0);
    public final ArrayList<Service> services = new ArrayList<Service>(0);
  }

  public final static class Activity extends Component<ActivityIntentInfo> {

  }

  public final static class ActivityIntentInfo extends IntentInfo {

  }

  public final static class Service extends Component<ServiceIntentInfo> {

  }

  public final static class ServiceIntentInfo extends IntentInfo {

  }
}
