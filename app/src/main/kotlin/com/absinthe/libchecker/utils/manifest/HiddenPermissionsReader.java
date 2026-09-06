package com.absinthe.libchecker.utils.manifest;

import androidx.collection.ArrayMap;


import java.io.File;
import java.io.IOException;
import java.util.Map;

import pxb.android.Res_value;
import pxb.android.axml.NodeVisitor;

public class HiddenPermissionsReader {
  private final ArrayMap<String, Object> permissionMap = new ArrayMap<>();

  private HiddenPermissionsReader(File apk) {
    ManifestReader.acceptManifest(apk, () -> new ManifestTagVisitor(null));
  }

  public static Map<String, Object> getHiddenPermissions(File apk) throws IOException {
    return new HiddenPermissionsReader(apk).permissionMap;
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
    public void attr(String ns, String name, int resourceId, String raw, Res_value value) {
      if ("name".equals(name) && value.type == Res_value.TYPE_STRING) {
        this.name = value.toString();
      } else if ("maxSdkVersion".equals(name) && value.type == Res_value.TYPE_INT_DEC) {
        this.maxSdkVersion = value.data;
      }
      super.attr(ns, name, resourceId, raw, value);
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
