package com.absinthe.libchecker.utils.manifest;

import androidx.collection.ArrayMap;


import java.io.File;
import java.io.IOException;
import java.util.Map;

import pxb.android.Res_value;
import pxb.android.axml.NodeVisitor;


public class ApplicationReader {
  private final ArrayMap<String, Object> properties = new ArrayMap<>();

  private ApplicationReader(File apk) throws IOException {
    ManifestReader.acceptManifest(apk, () -> new ManifestTagVisitor(null));
  }

  public static Map<String, Object> getManifestProperties(File apk) throws IOException {
    return new ApplicationReader(apk).properties;
  }

  private ApplicationReader(byte[] bytes) throws IOException {
    ManifestReader.acceptManifest(bytes, () -> new ManifestTagVisitor(null));
  }

  public static Map<String, Object> getManifestProperties(byte[] bytes) throws IOException {
    return new ApplicationReader(bytes).properties;
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
      if ("application".equals(name)) {
        return new ApplicationTagVisitor(child);
      }
      return child;
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
      public void attr(String ns, String name, int resourceId, String raw, Res_value value) {
        this.name = name;
        if (value.type == Res_value.TYPE_REFERENCE) {
          this.value = value.data;
        } else {
          this.value = value.toString();
        }

        if (name != null && value.type != Res_value.TYPE_NULL) {
          properties.put(name, this.value);
        }
        super.attr(ns, name, resourceId, raw, value);
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
