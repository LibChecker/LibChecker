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

public class ManifestReader {
    private final HashMap<String, Object> properties = new HashMap<>();
    private final String[] demands;

    public static Map<String, Object> getManifestProperties(File apk, String[] demands) throws IOException {
        return new ManifestReader(apk, demands).properties;
    }

    private ManifestReader(File apk, String[] demands) throws IOException {
        this.demands = demands;
        try(JarFile zip = new JarFile(apk)) {
            InputStream is = zip.getInputStream(zip.getEntry("AndroidManifest.xml"));
            byte[] bytes =  getBytesFromInputStream(is);
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

    private boolean contains(String name) {
        for (String demand: demands) {
            if (demand.equals(name)) return true;
        }
        return false;
    }

    private class ManifestTagVisitor extends NodeVisitor {
        public ManifestTagVisitor(NodeVisitor child) {
            super(child);
        }

        @Override
        public NodeVisitor child(String ns, String name) {
            NodeVisitor child = super.child(ns, name);
            switch (name) {
                case "application":
                    return new ApplicationTagVisitor(child);
                case "uses-sdk":
                    return new UsesSdkTagVisitor(child);
                default:
            }
            return child;
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

                    if(name != null && value != null) {
                        properties.put(name, value);
                    }
                }
                super.attr(ns, name, resourceId, type, obj);
            }

            @Override
            public void end() {
                if(name != null && value != null) {
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
                if(name != null && value != null) {
                    properties.put(name, value);
                }
                super.end();
            }
        }
    }
}
