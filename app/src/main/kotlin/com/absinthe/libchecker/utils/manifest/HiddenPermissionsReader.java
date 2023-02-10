package com.absinthe.libchecker.utils.manifest;

import androidx.collection.ArrayMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.jar.JarFile;

import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;

public class HiddenPermissionsReader {
  private final ArrayMap<String, Object> permissionMap = new ArrayMap<>();

  private HiddenPermissionsReader(File apk) throws IOException {
    try (JarFile zip = new JarFile(apk)) {
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
    }
  }

  public static Map<String, Object> getHiddenPermissions(File apk) throws IOException {
    return new HiddenPermissionsReader(apk).permissionMap;
  }

  private static byte[] getBytesFromInputStream(InputStream inputStream) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      byte[] b = new byte[1024];
      int n;
      while ((n = inputStream.read(b)) != -1) {
        bos.write(b, 0, n);
      }
      return bos.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private class ManifestTagVisitor extends NodeVisitor {
    public ManifestTagVisitor(NodeVisitor child) {
      super(child);
    }

    @Override
    public NodeVisitor child(String ns, String name) {
      NodeVisitor child = super.child(ns, name);
      if ("uses-permission".equals(name)) {
        return new PermissionVisitor(child);
      }
      return child;
    }
  }

  private class PermissionVisitor extends NodeVisitor {
    public String name = null;
    public Object maxSdkVersion = null;

    public PermissionVisitor(NodeVisitor child) {
      super(child);
    }

    @Override
    public void attr(String ns, String name, int resourceId, int type, Object obj) {
      if (type == 3 && "name".equals(name)) {
        this.name = (String) obj;
      }
      if ("maxSdkVersion".equals(name)) {
        maxSdkVersion = obj;
      }
      super.attr(ns, name, resourceId, type, obj);
    }

    @Override
    public void end() {
      if (name != null && maxSdkVersion != null) {
        permissionMap.put(name, maxSdkVersion);
      }
      super.end();
    }
  }
}
