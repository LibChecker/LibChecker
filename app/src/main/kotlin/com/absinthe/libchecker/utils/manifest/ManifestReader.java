package com.absinthe.libchecker.utils.manifest;

import androidx.collection.ArrayMap;

import com.absinthe.libchecker.compat.IZipFile;
import com.absinthe.libchecker.compat.ZipFileCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;

import pxb.android.Res_value;
import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;
import timber.log.Timber;


public class ManifestReader {
  static final int MAX_MANIFEST_BYTES = 16 * 1024 * 1024;
  private final ArrayMap<String, Object> properties = new ArrayMap<>();
  private final String[] demands;

  private ManifestReader(File apk, String[] demands) {
    this.demands = demands;
    acceptManifest(apk, () -> new ManifestTagVisitor(null));
  }

  private ManifestReader(byte[] bytes, String[] demands) {
    this.demands = demands;
    acceptManifest(bytes, () -> new ManifestTagVisitor(null));
  }

  public static Map<String, Object> getManifestProperties(byte[] bytes, String[] demands) throws IOException {
    return new ManifestReader(bytes, demands).properties;
  }

  public static Map<String, Object> getManifestProperties(File apk, String[] demands) throws IOException {
    return new ManifestReader(apk, demands).properties;
  }

  public static byte[] getBytesFromInputStream(InputStream inputStream) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      byte[] b = new byte[1024];
      int n;
      while ((n = inputStream.read(b)) != -1) {
        if (n > MAX_MANIFEST_BYTES - bos.size()) {
          throw new IOException("Manifest exceeds size limit");
        }
        bos.write(b, 0, n);
      }
      return bos.toByteArray();
    } catch (Exception e) {
      Timber.w(e);
    }
    return null;
  }

  static void acceptManifest(File apk, Supplier<NodeVisitor> visitor) {
    try (IZipFile zip = new ZipFileCompat(apk)) {
      ZipEntry entry = zip.getEntry("AndroidManifest.xml");
      if (entry == null || entry.getSize() > MAX_MANIFEST_BYTES) return;
      try (InputStream input = zip.getInputStream(entry)) {
        acceptManifest(getBytesFromInputStream(input), visitor);
      }
    } catch (Exception e) {
      Timber.w(e);
    }
  }

  static void acceptManifest(byte[] bytes, Supplier<NodeVisitor> visitor) {
    if (bytes == null || bytes.length > MAX_MANIFEST_BYTES) return;
    try {
      new AxmlReader(bytes).accept(new AxmlVisitor() {
        @Override
        public NodeVisitor child(String ns, String name) {
          return visitor.get();
        }
      });
    } catch (Exception e) {
      Timber.w(e);
    }
  }

  private boolean contains(String name) {
    for (String demand : demands) {
      if (demand.equals(name)) return true;
    }
    return false;
  }

  private class ManifestTagVisitor extends AttributeTagVisitor {
    public ManifestTagVisitor(NodeVisitor child) {
      super(child, true);
    }

    @Override
    public NodeVisitor child(String ns, String name) {
      NodeVisitor child = super.child(ns, name);
      return switch (name) {
        case "application" -> new AttributeTagVisitor(child, true);
        case "uses-sdk" -> new AttributeTagVisitor(child, false);
        case "overlay" -> {
          properties.put("overlay", true);
          yield new AttributeTagVisitor(child, false);
        }
        default -> child;
      };
    }
  }

  private class AttributeTagVisitor extends NodeVisitor {
    private final boolean writeOnAttribute;
    private String name;
    private Object value;

    public AttributeTagVisitor(NodeVisitor child, boolean writeOnAttribute) {
      super(child);
      this.writeOnAttribute = writeOnAttribute;
    }

    @Override
    public void attr(String ns, String name, int resourceId, String raw, Res_value value) {
      if (contains(name)) {
        this.name = name;
        this.value = value.type == Res_value.TYPE_REFERENCE ? value.data : value.toString();
        if (writeOnAttribute && name != null && value.type != Res_value.TYPE_NULL) {
          properties.put(name, this.value);
        }
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
