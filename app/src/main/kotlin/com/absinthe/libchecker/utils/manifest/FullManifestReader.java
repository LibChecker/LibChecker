package com.absinthe.libchecker.utils.manifest;

import androidx.collection.ArrayMap;

import com.absinthe.libchecker.compat.IZipFile;
import com.absinthe.libchecker.compat.ZipFileCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import pxb.android.Res_value;
import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;
import timber.log.Timber;


public class FullManifestReader {
  private final String[] demands;
  public final ArrayMap<String, Object> properties = new ArrayMap<>();
  public final ArrayList<String> permissionList = new ArrayList<>();
  public final ArrayList<String> services = new ArrayList<>();
  public final ArrayList<String> activities = new ArrayList<>();
  public final ArrayList<String> receivers = new ArrayList<>();
  public final ArrayList<String> providers = new ArrayList<>();
  public final ArrayMap<String, Object> metadata = new ArrayMap<>();

  public FullManifestReader(File apk, String[] demands) {
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

  public FullManifestReader(byte[] bytes, String[] demands) {
    this.demands = demands;
    try {
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

  public static Map<String, Object> getManifestProperties(byte[] bytes, String[] demands) throws IOException {
    return new FullManifestReader(bytes, demands).properties;
  }

  public static Map<String, Object> getManifestProperties(File apk, String[] demands) throws IOException {
    return new FullManifestReader(apk, demands).properties;
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
    return true;
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
        case "uses-permission" -> new PermissionVisitor(child);
        case "overlay" -> {
          properties.put("overlay", true);
          yield new OverlayTagVisitor(child);
        }
        default -> child;
      };
    }

    @Override
    public void attr(String ns, String name, int resourceId, String raw, Res_value value) {
      if (contains(name)) {
        this.name = name;
        if (value.type == Res_value.TYPE_REFERENCE) {
          this.value = value.data;
        } else {
          this.value = value.toString();
        }

        if (name != null && value.type != Res_value.TYPE_NULL) {
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

    private class PermissionVisitor extends NodeVisitor {
      public String name = null;

      public PermissionVisitor(NodeVisitor child) {
        super(child);
      }

      @Override
      public void attr(String ns, String name, int resourceId, String raw, Res_value value) {
        if ("name".equals(name) && value.type == Res_value.TYPE_STRING) {
          this.name = value.toString();
        }
        super.attr(ns, name, resourceId, raw, value);
      }

      @Override
      public void end() {
        if (name != null) {
          permissionList.add(name);
        }
        super.end();
      }
    }

    private class ApplicationTagVisitor extends NodeVisitor {
      public String name = null;
      public Object value = null;

      public ApplicationTagVisitor(NodeVisitor child) {
        super(child);
      }

      @Override
      public NodeVisitor child(String ns, String name) {
        NodeVisitor child = super.child(ns, name);
        return switch (name) {
          case "service" -> new ComponentTagVisitor(child, services);
          case "activity" -> new ComponentTagVisitor(child, activities);
          case "receiver" -> new ComponentTagVisitor(child, receivers);
          case "provider" -> new ComponentTagVisitor(child, providers);
          case "meta-data" -> new MetadataTagVisitor(child);
          default -> child;
        };
      }

      @Override
      public void attr(String ns, String name, int resourceId, String raw, Res_value value) {
        if (contains(name)) {
          this.name = name;
          if (value.type == Res_value.TYPE_REFERENCE) {
            this.value = value.data;
          } else {
            this.value = value.toString();
          }

          if (name != null && value.type != Res_value.TYPE_NULL) {
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

    private class UsesSdkTagVisitor extends NodeVisitor {
      public String name = null;
      public Object value = null;

      public UsesSdkTagVisitor(NodeVisitor child) {
        super(child);
      }

      @Override
      public void attr(String ns, String name, int resourceId, String raw, Res_value value) {
        if (contains(name)) {
          this.name = name;
          if (value.type == Res_value.TYPE_REFERENCE) {
            this.value = value.data;
          } else {
            this.value = value.toString();
          }
          if (this.name != null) {
            properties.put(this.name, this.value);
          }
        }
        super.attr(ns, name, resourceId, raw, value);
      }

      @Override
      public void end() {
//        if (name != null && value != null) {
//          properties.put(name, value);
//        }
        super.end();
      }
    }

    private static class ComponentTagVisitor extends NodeVisitor {
      public String name = null;
      private final ArrayList<String> componentsList;

      public ComponentTagVisitor(NodeVisitor child, ArrayList<String> componentsList) {
        super(child);
        this.componentsList = componentsList;
      }

      @Override
      public void attr(String ns, String name, int resourceId, String raw, Res_value value) {
        if ("name".equals(name) && value.type == Res_value.TYPE_STRING) {
          this.name = value.toString();
        }
        super.attr(ns, name, resourceId, raw, value);
      }

      @Override
      public void end() {
        if (name != null) {
          componentsList.add(name);
        }
        super.end();
      }
    }

    private class MetadataTagVisitor extends NodeVisitor {
      public String name = null;
      public Object value = null;
      public Long resource = null;

      public MetadataTagVisitor(NodeVisitor child) {
        super(child);
      }

      @Override
      public void attr(String ns, String name, int resourceId, String raw, Res_value value) {
        if ("name".equals(name)) {
          this.name = value.toString();
        } else if ("resource".equals(name) && value.type == Res_value.TYPE_REFERENCE) {
          this.resource = (long) value.data;
        } else if ("value".equals(name)) {
          if (value.type == Res_value.TYPE_REFERENCE) {
            this.value = value.data;
          } else {
            this.value = value.toString();
          }
        }
        super.attr(ns, name, resourceId, raw, value);
      }

      @Override
      public void end() {
        if (name != null) {
          if (value != null) {
            metadata.put(name, value);
          } else if (resource != null) {
            metadata.put(name, resource);
          }
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
      public void attr(String ns, String name, int resourceId, String raw, Res_value value) {
        if (contains(name)) {
          this.name = name;
          if (value.type == Res_value.TYPE_REFERENCE) {
            this.value = value.data;
          } else {
            this.value = value.toString();
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
}
