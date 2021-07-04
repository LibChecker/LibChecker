package com.absinthe.libchecker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;

public class StaticLibraryReader {
    private final HashMap<String, Object> staticLibs = new HashMap<>();

    private StaticLibraryReader(File apk) throws IOException {
        try (JarFile zip = new JarFile(apk)) {
            InputStream is = zip.getInputStream(zip.getEntry("AndroidManifest.xml"));
            byte[] bytes = getBytesFromInputStream(is);
            AxmlReader reader = new AxmlReader(bytes);
            reader.accept(new AxmlVisitor() {
                @Override
                public NodeVisitor child(String ns, String name) {
                    NodeVisitor child = super.child(ns, name);
                    return new ManifestTagVisitor(child);
                }
            });
        }
    }

    public static Map<String, Object> getStaticLibrary(File apk) throws IOException {
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
            e.printStackTrace();
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
        public Object version = null;

        public StaticLibraryVisitor(NodeVisitor child) {
            super(child);
        }

        @Override
        public void attr(String ns, String name, int resourceId, int type, Object obj) {
            if (type == 3 && "name".equals(name)) {
                this.name = (String) obj;
            }
            if ("version".equals(name)) {
                version = obj;
            }
            super.attr(ns, name, resourceId, type, obj);
        }

        @Override
        public void end() {
            if (name != null && version != null) {
                staticLibs.put(name, version);
            }
            super.end();
        }
    }
}
