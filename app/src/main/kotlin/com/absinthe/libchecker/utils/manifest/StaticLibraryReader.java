package com.absinthe.libchecker.utils.manifest;

import androidx.collection.ArrayMap;

import com.absinthe.libchecker.domain.app.detail.model.StaticLibItem;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import pxb.android.Res_value;
import pxb.android.axml.NodeVisitor;

public class StaticLibraryReader {
  private final ArrayMap<String, StaticLibItem> staticLibs = new ArrayMap<>();

  private StaticLibraryReader(File apk) {
    ManifestReader.acceptManifest(apk, () -> new ManifestTagVisitor(null));
  }

  public static Map<String, StaticLibItem> getStaticLibrary(File apk) throws IOException {
    return new StaticLibraryReader(apk).staticLibs;
  }

  private class ManifestTagVisitor extends NodeVisitor {
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

    private class ApplicationTagVisitor extends NodeVisitor {
      public ApplicationTagVisitor(NodeVisitor child) {
        super(child);
      }

      @Override
      public NodeVisitor child(String ns, String name) {
        NodeVisitor child = super.child(ns, name);
        if ("uses-static-library".equals(name)) {
          return new StaticLibraryVisitor(child);
        }
        return child;
      }
    }
  }

  private class StaticLibraryVisitor extends NodeVisitor {
    public String name = null;
    public Integer version = null;
    public String certDigest = null;

    public StaticLibraryVisitor(NodeVisitor child) {
      super(child);
    }

    @Override
    public void attr(String ns, String name, int resourceId, String raw, Res_value value) {
      if ("name".equals(name) && value.type == Res_value.TYPE_STRING) {
        this.name = value.toString();
      } else if ("version".equals(name) && value.type == Res_value.TYPE_INT_DEC) {
        version = value.data;
      } else if ("certDigest".equals(name) && value.type == Res_value.TYPE_STRING) {
        this.certDigest = value.toString();
      }
      super.attr(ns, name, resourceId, raw, value);
    }

    @Override
    public void end() {
      if (name != null && version != null) {
        StaticLibItem item = new StaticLibItem(name, version, certDigest, "");
        staticLibs.put(name, item);
      }
      super.end();
    }
  }
}
