package com.absinthe.libchecker.utils.manifest;

import androidx.collection.ArrayMap;

import com.absinthe.libchecker.compat.IZipFile;
import com.absinthe.libchecker.compat.ZipFileCompat;
import com.absinthe.libchecker.features.applist.detail.bean.StaticLibItem;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import pxb.android.Res_value;
import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;
import timber.log.Timber;

public class StaticLibraryReader {
  private final ArrayMap<String, StaticLibItem> staticLibs = new ArrayMap<>();

  private StaticLibraryReader(File apk) {
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

  public static Map<String, StaticLibItem> getStaticLibrary(File apk) throws IOException {
    return new StaticLibraryReader(apk).staticLibs;
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

  public static int extractIntPart(String str) {
    int result = 0, length = str.length();
    for (int offset = 0; offset < length; offset++) {
      char c = str.charAt(offset);
      if ('0' <= c && c <= '9')
        result = result * 10 + (c - '0');
      else
        break;
    }
    return result;
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
