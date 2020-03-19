// Code mostly taken from zmq integration in Java.
// https://github.com/zeromq/jzmq/blob/3384ea1c04876426215fe76b5d1aabc58c099ca0/jzmq-jni/src/main/java/org/zeromq/EmbeddedLibraryTools.java

package org.wasmer;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class EmbeddedLibraryTools {

    public static final boolean LOADED_EMBEDDED_LIBRARY;

    static {
        LOADED_EMBEDDED_LIBRARY = loadEmbeddedLibrary();
    }

    private EmbeddedLibraryTools() {
    }

    public static String getCurrentPlatformIdentifier() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("windows")) {
            osName = "windows";
        } else if (osName.contains("mac os x")) {
            osName = "darwin";
        } else {
            osName = osName.replaceAll("\\s+", "_");
        }
        return osName + "-" + System.getProperty("os.arch");
    }

    public static Collection<String> getEmbeddedLibraryList() {

        final Collection<String> result = new ArrayList<String>();
        final Collection<String> files = catalogClasspath();

        for (final String file : files) {
            if (file.startsWith("NATIVE")) {
                result.add(file);
            }
        }

        return result;

    }

    private static void catalogArchive(final File jarfile, final Collection<String> files) {
        JarFile j = null;
        try {
            j = new JarFile(jarfile);
            final Enumeration<JarEntry> e = j.entries();
            while (e.hasMoreElements()) {
                final JarEntry entry = e.nextElement();
                if (!entry.isDirectory()) {
                    files.add(entry.getName());
                }
            }

        } catch (IOException x) {
            System.err.println(x.toString());
        } finally {
            try {
                j.close();
            } catch (Exception e) {
            }
        }

    }

    private static Collection<String> catalogClasspath() {

        final List<String> files = new ArrayList<String>();
        final String[] classpath = System.getProperty("java.class.path", "").split(File.pathSeparator);

        for (final String path : classpath) {
            final File tmp = new File(path);
            if (tmp.isFile() && path.toLowerCase().endsWith(".jar")) {
                catalogArchive(tmp, files);
            } else if (tmp.isDirectory()) {
                final int len = tmp.getPath().length() + 1;
                catalogFiles(len, tmp, files);
            }
        }

        return files;

    }

    private static void catalogFiles(final int prefixlen, final File root, final Collection<String> files) {
        final File[] ff = root.listFiles();
        if (ff == null) {
            throw new IllegalStateException("invalid path listed: " + root);
        }

        for (final File f : ff) {
            if (f.isDirectory()) {
                catalogFiles(prefixlen, f, files);
            } else {
                files.add(f.getPath().substring(prefixlen));
            }
        }
    }

    private static boolean loadEmbeddedLibrary() {

        boolean usingEmbedded = false;

        // attempt to locate embedded native library within JAR at following location:
        // /NATIVE/${os.arch}/${os.name}/[libwasmer.so|libwasmer.dylib|wasmer.dll]
        String[] libs;
        final String libsFromProps = System.getProperty("wasmer-native");
        if (libsFromProps == null)
            libs = new String[]{"libjava_ext_wasm.so", "libjava_ext_wasm.dylib", "java_ext_wasm.dll"};
        else
            libs = libsFromProps.split(",");
        StringBuilder url = new StringBuilder();
        url.append("/org/wasmer/native/");
        url.append(getCurrentPlatformIdentifier()).append("/");
        URL nativeLibraryUrl = null;
        // loop through extensions, stopping after finding first one
        for (String lib : libs) {
            nativeLibraryUrl = Module.class.getResource(url.toString() + lib);
            if (nativeLibraryUrl != null)
                break;
        }

        if (nativeLibraryUrl != null) {
            // native library found within JAR, extract and load
            try {
                final File libfile = File.createTempFile("java_ext_wasm", ".lib");
                libfile.deleteOnExit(); // just in case

                final InputStream in = nativeLibraryUrl.openStream();
                final OutputStream out = new BufferedOutputStream(new FileOutputStream(libfile));

                int len = 0;
                byte[] buffer = new byte[8192];
                while ((len = in.read(buffer)) > -1)
                    out.write(buffer, 0, len);
                out.close();
                in.close();
                System.load(libfile.getAbsolutePath());

                usingEmbedded = true;

            } catch (IOException x) {
                // mission failed, do nothing
            }

        } // nativeLibraryUrl exists
        return usingEmbedded;
    }
}
