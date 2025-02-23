package com.absinthe.libchecker.utils.manifest;

import androidx.collection.ArrayMap;

import com.absinthe.libchecker.compat.IZipFile;
import com.absinthe.libchecker.compat.ZipFileCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;
import timber.log.Timber;


public class ManifestReader {
  private final ArrayMap<String, Object> properties = new ArrayMap<>();
  private final String[] demands;

  private ManifestReader(File apk, String[] demands) {
    this.demands = demands;
    try (IZipFile zip = new ZipFileCompat(apk)) {
      InputStream is = zip.getInputStream(zip.getEntry("AndroidManifest.xml"));
      byte[] bytes = getBytesFromInputStream(is);
      AxmlReader reader = new AxmlReader(bytes != null ? bytes : new byte[0]);
      reader.accept(new AxmlVisitor() {
        @Override
        public NodeVisitor child(String ns, String name) {
          NodeVisitor child = super.child(ns, name);
          return new ManifestTagVisitor(child);
        }
      });
    } catch (Exception e) {
      Timber.e(e);
    }
  }

  public static Map<String, Object> getManifestProperties(File apk, String[] demands) throws IOException {
    return new ManifestReader(apk, demands).properties;
  }

  public static byte[] getBytesFromInputStream(InputStream inputStream) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      byte[] b = new byte[1024];
      int n;
      while ((n = inputStream.read(b)) != -1) {
        bos.write(b, 0, n);
      }
      return bos.toByteArray();
    } catch (Exception e) {
      Timber.w(e);
    }
    return null;
  }

  private boolean contains(String name) {
    for (String demand : demands) {
      if (demand.equals(name)) return true;
    }
    return false;
  }

  private class ManifestTagVisitor extends NodeVisitor {
    public String name = null;
    public Object value = null;

    public ManifestTagVisitor(NodeVisitor child) {
      super(child);
    }

    @Override
    public NodeVisitor child(String ns, String name) {
      NodeVisitor child = super.child(ns, name);
      return switch (name) {
        case "application" -> new ApplicationTagVisitor(child);
        case "uses-sdk" -> new UsesSdkTagVisitor(child);
        case "overlay" -> {
          properties.put("overlay", true);
          yield new OverlayTagVisitor(child);
        }
        default -> child;
      };
    }

    @Override
    public void attr(String ns, String name, int resourceId, int type, Object obj) {
      if (contains(name)) {
        this.name = name;
        value = obj;

        if (name != null && value != null) {
          properties.put(name, value);
        }
      }
      super.attr(ns, name, resourceId, type, obj);
    }

    @Override
    public void end() {
      if (name != null && value != null) {
        properties.put(name, value);
      }
      super.end();
    }

    private class ApplicationTagVisitor extends NodeVisitor {
      public String name = null;
      public Object value = null;

      public ApplicationTagVisitor(NodeVisitor child) {
        super(child);
      }

      @Override
      public void attr(String ns, String name, int resourceId, int type, Object obj) {
        if (contains(name)) {
          this.name = name;
          value = obj;

          if (name != null && value != null) {
            properties.put(name, value);
          }
        }
        super.attr(ns, name, resourceId, type, obj);
      }

      @Override
      public void end() {
        if (name != null && value != null) {
          properties.put(name, value);
        }
        super.end();
      }
    }

    private class UsesSdkTagVisitor extends NodeVisitor {
      public String name = null;
      public Object value = null;

      public UsesSdkTagVisitor(NodeVisitor child) {
        super(child);
      }

      @Override
      public void attr(String ns, String name, int resourceId, int type, Object obj) {
        if (contains(name)) {
          this.name = name;
          value = obj;
        }
        super.attr(ns, name, resourceId, type, obj);
      }

      @Override
      public void end() {
        if (name != null && value != null) {
          properties.put(name, value);
        }
        super.end();
      }
    }

    private class OverlayTagVisitor extends NodeVisitor {
      public String name = null;
      public Object value = null;

      public OverlayTagVisitor(NodeVisitor child) {
        super(child);
      }

      @Override
      public void attr(String ns, String name, int resourceId, int type, Object obj) {
        if (contains(name)) {
          this.name = name;
          value = obj;
        }
        super.attr(ns, name, resourceId, type, obj);
      }

      @Override
      public void end() {
        if (name != null && value != null) {
          properties.put(name, value);
        }
        super.end();
      }
    }
  }
}
