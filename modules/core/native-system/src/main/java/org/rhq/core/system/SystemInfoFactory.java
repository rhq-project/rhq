/*
* RHQ Management Platform
* Copyright (C) 2005-2014 Red Hat, Inc.
* All rights reserved.
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License, version 2, as
* published by the Free Software Foundation, and/or the GNU Lesser
* General Public License, version 2.1, also as published by the Free
* Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License and the GNU Lesser General Public License
* for more details.
*
* You should have received a copy of the GNU General Public License
* and the GNU Lesser General Public License along with this program;
* if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/
package org.rhq.core.system;

import java.io.File;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.template.TemplateEngine;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * Builds {@link SystemInfo} objects based on the native operating system the VM is running on.
 *
 * @author John Mazzitelli
 */
public class SystemInfoFactory {
    /**
     * All template engine replacement variables that this factory's template engine will know about
     * will be prefixed with this string.
     */
    public static final String TOKEN_PREFIX = "rhq.system.";

    private static final Log LOG = LogFactory.getLog(SystemInfoFactory.class);

    private static final String NATIVE_LIBRARY_CLASS_NAME = "org.hyperic.sigar.Sigar";

    private static boolean nativeLibraryLoadable;
    private static Throwable nativeLibraryLoadThrowable;
    private static boolean disabled;
    private static boolean initialized = false;

    private static final ThreadFactory threadFactory = new ThreadFactory() {
        final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            // There's no need to block the JVM while waiting for a process completion or pumping process streams
            t.setDaemon(true);
            t.setName("systeminfo-" + threadNumber.getAndIncrement());
            return t;
        }
    };

    /**
     * Used to execute tasks watching for process completion or pumping process streams within the plugin container.
     * This should be 'final' but shutdown() is not really final either.
     */
    private static ExecutorService executor = Executors.newCachedThreadPool(threadFactory);

    private static SystemInfo javaSystemInfo = new JavaSystemInfo(executor);

    private static SystemInfo cachedSystemInfo;

    // to avoid having this class need the native JNI classes at compile or load time, we'll build up
    // the small amount of methods/constants we need to call via reflection and store them here
    private static Map<NativeApi, AccessibleObject> nativeApis = new HashMap<NativeApi, AccessibleObject>();

    // these are keys to the nativeApis map - they are names of SIGAR's static methods and constants we'll be calling
    private enum NativeApi {
        load, VERSION_STRING, BUILD_DATE, NATIVE_VERSION_STRING, NATIVE_BUILD_DATE
    }

    /**
     * If the native system is both {@link #isNativeSystemInfoAvailable() available} and
     * {@link #isNativeSystemInfoDisabled() enabled}, this will return the native system's version string. Otherwise, a
     * generic Java version message is returned.
     *
     * @return native system version string
     */
    public static synchronized String getNativeSystemInfoVersion() {
        String version = null;
        Throwable error = null;

        initialize(); // make sure we've loaded the native libraries, if appropriate

        if (!isNativeSystemInfoDisabled() && isNativeSystemInfoAvailable()) {
            try {
                version = "Version=" + invokeApi(NativeApi.VERSION_STRING) + " (" + invokeApi(NativeApi.BUILD_DATE)
                    + "); Native version=" + invokeApi(NativeApi.NATIVE_VERSION_STRING) + " ("
                    + invokeApi(NativeApi.NATIVE_BUILD_DATE) + ")";
            } catch (Throwable t) {
                error = t;
            }
        }

        if (version == null) {
            version = "Native system not supported - Java version is " + System.getProperty("java.version");

            if (error != null) {
                version += " : " + error;
            }
        }

        return version;
    }

    /**
     * This will tell the factory to not {@link #createSystemInfo() create} any native objects and to not load the
     * native libraries - even if the native library {@link #isNativeSystemInfoAvailable() is available}.
     */
    public static synchronized void disableNativeSystemInfo() {
        // if we are switching, clear the cached system info so we'll get a new one later
        if (!disabled) {
            cachedSystemInfo = null;
        }
        disabled = true;
    }

    /**
     * This will allow the factory to load the native libraries and {@link #createSystemInfo() create} native objects.
     * Note that this will have no effect if there are no native libraries
     * {@link #isNativeSystemInfoAvailable() available}.
     */
    public static synchronized void enableNativeSystemInfo() {
        // if we are switching, clear the cached system info so we'll get a new one later
        if (disabled) {
            cachedSystemInfo = null;
        }
        disabled = false;
    }

    /**
     * Returns <code>true</code> if this factory was told to {@link #disableNativeSystemInfo() disable} the native
     * layer. This only indicates if it was disabled; this has nothing to do with whether or not the native libraries
     * {@link #isNativeSystemInfoAvailable() actually exist and are available}.
     *
     * @return <code>true</code> if the native layer is disabled
     */
    public static synchronized boolean isNativeSystemInfoDisabled() {
        return disabled;
    }

    /**
     * If there is a native library available for the JVM's platform/operating system, <code>true</code> is returned. If
     * the JVM's platform does not have native libraries available, <code>false</code> is returned (in which case, the
     * only {@link SystemInfo} implementation that is available is {@link JavaSystemInfo}).
     *
     * @return <code>true</code> if there are native library APIs available for the JVM platform
     * @see JavaSystemInfo
     */
    public static boolean isNativeSystemInfoAvailable() {
        initialize();

        return nativeLibraryLoadable;
    }

    /**
     * Returns a Throwable describing why the native library failed to initialize, or null if the native library
     * initialized successfully
     *
     * @return a Throwable describing why the native library failed to initialize, or null if the native library
     *         initialized successfully
     */
    public static Throwable getNativeLibraryLoadThrowable() {
        return nativeLibraryLoadThrowable;
    }

    /**
     * This returns <code>true</code> iff the native libraries have actually been initialized. This will return <code>
     * false</code> if this factory has been {@link #disableNativeSystemInfo() disabled} from the start (i.e. prior to
     * the first call to {@link #createSystemInfo()} or {@link #getNativeSystemInfoVersion()}). It will also return
     * <code>false</code> after the native libraries have been {@link #shutdown()}.
     *
     * @return <code>true</code> if the native libraries have been loaded and initialized
     */
    public static synchronized boolean isNativeSystemInfoInitialized() {
        return initialized;
    }

    /**
     * Returns the appropriate {@link SystemInfo} implementation based on the platform/operating system the JVM is
     * running on.
     *
     * @return a {@link NativeSystemInfo} implementation or a {@link JavaSystemInfo} if the native libraries are
     *         {@link #isNativeSystemInfoAvailable() not available for the platform} or have been
     *         {@link #disableNativeSystemInfo() disabled}.
     */
    public static synchronized SystemInfo createSystemInfo() {
        if (cachedSystemInfo == null) {
            initialize(); // make sure we've loaded the native libraries, if appropriate

            SystemInfo nativePlatform = null;

            if (!isNativeSystemInfoDisabled() && isNativeSystemInfoAvailable()) {
                // we could use SIGAR here, but this should be enough
                if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
                    nativePlatform = new WindowsNativeSystemInfo();
                } else {
                    // we either don't know what OS it is or we don't have a specific native subclass for it;
                    // but we know we have a native library for it! so just create the generic NativePlatform to represent it.
                    nativePlatform = new NativeSystemInfo();
                }
            }

            if (nativePlatform == null) {
                nativePlatform = javaSystemInfo;
            }

            cachedSystemInfo = nativePlatform;
        }
        return cachedSystemInfo;
    }

    /**
     * Under some circumstances, you may want to force this factory to provide a Java-only {@link SystemInfo}
     * implementation, regardless of whether the factory has {@link #disableNativeSystemInfo() disabled} the native
     * layer or not. In the cases when you absolutely, positively have to have a non-native, Java-only implementation,
     * you can use this method.
     *
     * @return Java-only {@link SystemInfo} implementation
     */
    public static SystemInfo createJavaSystemInfo() {
        return javaSystemInfo;
    }

    /**
     * This method is reserved for those objects (like shutdown hooks) that are aware of the VM going down - at which
     * time it would be appropriate to give the native libraries a chance to clean up after themselves.
     */
    public static synchronized void shutdown() {
        // Unclear if this should be 'shutdownNow()' or not
        // This is used to execute sub processes; so it seems polite to wait for completion
        executor.shutdown();
        // This is needed by tests
        executor = Executors.newCachedThreadPool(threadFactory);
        javaSystemInfo = new JavaSystemInfo(executor);

        if (initialized) {
            // initialized is only ever set to true if the native layer was actually initialized
            // we don't want or need to check enabled/disabled here; we could have disabled after
            // the native library was initialized

            // Does not look like there are any static SIGAR methods that need to be called at shutdown;
            // Sigar.finalize() takes care of instance cleanup
            //         try
            //         {
            //            Sigar.XXXshutdownXXX();
            //         }
            //         catch ( Throwable t )
            //         {
            //         }

            initialized = false;
            cachedSystemInfo = null;
        }
    }

    /**
     * This will initialize the native layer, if applicable. If the native layer was already initialized, this does
     * nothing and returns immediately.
     */
    private static synchronized void initialize() {
        if (!initialized) {
            if (!isNativeSystemInfoDisabled()) {
                try {
                    Class<?> clazz = Class.forName(NATIVE_LIBRARY_CLASS_NAME);

                    nativeApis.put(NativeApi.load, clazz.getMethod(NativeApi.load.name(), new Class[0]));
                    nativeApis.put(NativeApi.VERSION_STRING, clazz.getField(NativeApi.VERSION_STRING.name()));
                    nativeApis.put(NativeApi.BUILD_DATE, clazz.getField(NativeApi.BUILD_DATE.name()));
                    nativeApis.put(NativeApi.NATIVE_VERSION_STRING, clazz.getField(NativeApi.NATIVE_VERSION_STRING.name()));
                    nativeApis.put(NativeApi.NATIVE_BUILD_DATE, clazz.getField(NativeApi.NATIVE_BUILD_DATE.name()));

                    invokeApi(NativeApi.load);

                    nativeLibraryLoadable = true;

                    initialized = true; // we only set this to true when we actually initialized the native layer
                } catch (Throwable t) {
                    // We don't have the JNI classes (sigar.jar) and/or the native shared library (e.g. libsigar-amd64-linux.so).
                    // This might be expected (e.g. the admin console WAR (Embedded Jopr) does not include SIGAR), so don't log
                    // anything, but store the Throwable, so callers can log the cause when appropriate.
                    nativeLibraryLoadable = false;
                    nativeLibraryLoadThrowable = t;

                    LOG.warn("Native library not available on this platform: " + ThrowableUtil.getAllMessages(t));
                    LOG.debug("Native library failure stack trace follows: ", t);

                    disabled = true; // automatically disable native system info if the native library is not loadable
                }
            }
        }

        return;
    }

    /**
     * In order for this class to not have any compile or load time dependencies on the SIGAR jar, use this method to
     * invoke static methods on SIGAR's main class. This allows applications that want to use this factory but be able
     * to work even if an exception occurs when loading SIGAR.
     *
     * @param api  the static method to invoke or a constant name whose value is to be retrieved
     * @param args optional set of arguments to pass to the static method (may be <code>null</code>)
     * @return the return object of the call to the static method or the value of the static constant
     * @throws Throwable if failed to invoke the static method or get the static constant value for any reason (Java JNI
     *                   jar not available, native library not available, etc)
     */
    private static final Object invokeApi(NativeApi api, Object... args) throws Throwable {
        AccessibleObject accessibleObject = nativeApis.get(api);

        if (accessibleObject instanceof Method) {
            return ((Method) accessibleObject).invoke(null, args);
        } else {
            return ((Field) accessibleObject).get(null);
        }
    }

    /**
     * The native libraries are located under a root directory - this returns that root directory. It is assumed this
     * root directory is located in the same directory where the SIGAR Java API jar file is found. This is probably not
     * needed anymore. However, it may be useful in the future, so I'll leave it here. SIGAR has something else that is
     * related - the system property "org.hyperic.sigar.path" points to the directory where the native libraries can be
     * found.
     *
     * @return the root directory
     * @throws Exception if the root directory could not be determined or does not exist
     */
    private static String findNativeLibrariesRootDirectory() throws Exception {
        String rootDir = null;
        File jniLocation = null;
        URL jarLocation;

        if (isNativeSystemInfoAvailable()) {
            rootDir = System.getProperty("rhq.native-libraries-root-directory");
            if (rootDir == null) {
                jarLocation = Class.forName(NATIVE_LIBRARY_CLASS_NAME).getProtectionDomain().getCodeSource()
                    .getLocation();
                if (jarLocation != null) {
                    // the root directory where all JNI libraries are located is the
                    // same directory as where the SIGAR jar is found
                    jniLocation = new File(jarLocation.toURI()).getParentFile();
                }

                if ((jniLocation != null) && jniLocation.exists()) {
                    rootDir = jniLocation.getAbsolutePath();
                } else {
                    throw new Exception("Native JNI libraries cannot be found: " + "jar-location=[" + jarLocation
                        + "], jni-location=[" + jniLocation + "]");
                }
            }
        }

        LOG.debug("Root directory to the native library is: " + rootDir);

        return rootDir;
    }

    /**
     * Prevent instantiation.
     */
    private SystemInfoFactory() {
    }

    public static TemplateEngine fetchTemplateEngine() {

        try {
            SystemInfo systemInfo = createSystemInfo();

            Map<String, String> tokens = new HashMap<String, String>();

            // support several standard Java system properties
            String[] syspropsToSupport = new String[] { "java.io.tmpdir", "file.separator", "line.separator",
                "path.separator", "java.home", "java.version", "user.timezone", "user.region", "user.country",
                "user.language" };
            for (String sysprop : syspropsToSupport) {
                tokens.put(TOKEN_PREFIX + "sysprop." + sysprop, System.getProperty(sysprop, sysprop)); // default is the name itself, just to show it wasn't there
            }

            tokens.put(TOKEN_PREFIX + "hostname", systemInfo.getHostname());
            tokens.put(TOKEN_PREFIX + "os.name", systemInfo.getOperatingSystemName());
            tokens.put(TOKEN_PREFIX + "os.version", systemInfo.getOperatingSystemVersion());
            tokens.put(TOKEN_PREFIX + "os.type", systemInfo.getOperatingSystemType().toString());
            tokens.put(TOKEN_PREFIX + "architecture", systemInfo.getSystemArchitecture());
            try {
                tokens.put(TOKEN_PREFIX + "cpu.count", Integer.toString(systemInfo.getNumberOfCpus()));
            } catch (Exception e) {
                tokens.put(TOKEN_PREFIX + "cpu.count", "?");
            }

            List<NetworkAdapterInfo> allNetworkAdapters = null;
            try {
                allNetworkAdapters = systemInfo.getAllNetworkAdapters();
            } catch (Exception e) {
            }
            if (allNetworkAdapters != null) {
                for (NetworkAdapterInfo networkAdapter : allNetworkAdapters) {
                    String key = TOKEN_PREFIX + "interfaces." + networkAdapter.getName();
                    tokens.put(key + ".mac", networkAdapter.getMacAddressString());
                    tokens.put(key + ".type", networkAdapter.getType());
                    tokens.put(key + ".flags", networkAdapter.getAllFlags());

                    if (!networkAdapter.getUnicastAddresses().isEmpty()) {
                        tokens.put(key + ".address", networkAdapter.getUnicastAddresses().get(0).getHostAddress());
                    }
                    if (!networkAdapter.getMulticastAddresses().isEmpty()) {
                        tokens.put(key + ".multicast.address", networkAdapter.getMulticastAddresses().get(0)
                            .getHostAddress());
                    }
                }
            }

            // create a base IP address - this one is known to java and should always exist no matter what platform we are on
            try {
                try {
                    tokens.put(TOKEN_PREFIX + "interfaces.java.address", InetAddress
                        .getByName(systemInfo.getHostname()).getHostAddress());
                } catch (Exception e) {
                    tokens.put(TOKEN_PREFIX + "interfaces.java.address", InetAddress.getLocalHost().getHostAddress());
                }
            } catch (Exception e2) {
            }

            TemplateEngine templateEngine = new TemplateEngine(tokens);
            return templateEngine;
        } catch (Exception e) {
            // some rare exception occurred that we didn't expect, rather than blow up entirely, just don't provide any values
            return new TemplateEngine(new HashMap<String, String>());
        }
    }

}
