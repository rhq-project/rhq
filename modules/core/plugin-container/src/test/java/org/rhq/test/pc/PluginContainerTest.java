/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.test.pc;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.jmock.Mockery;
import org.testng.ITestResult;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Listeners;

import org.rhq.core.clientapi.server.bundle.BundleServerService;
import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.clientapi.server.content.ContentServerService;
import org.rhq.core.clientapi.server.core.CoreServerService;
import org.rhq.core.clientapi.server.discovery.DiscoveryServerService;
import org.rhq.core.clientapi.server.drift.DriftServerService;
import org.rhq.core.clientapi.server.event.EventServerService;
import org.rhq.core.clientapi.server.inventory.ResourceFactoryServerService;
import org.rhq.core.clientapi.server.measurement.MeasurementServerService;
import org.rhq.core.clientapi.server.operation.OperationServerService;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.util.file.FileUtil;
import org.rhq.test.JMockTest;

/**
 * This class is similar to {@link JMockTest} in that it can be used both
 * as a base class to your test or as a TestNG {@link Listeners listener}
 * on your test class.
 * <p>
 * This class is used to declaratively setup a plugin container a test wants
 * to use using the {@link PluginContainerSetup} annotation on a test method or class.
 * 
 * @author Lukas Krejci
 */
public class PluginContainerTest extends JMockTest {
    private static final File ROOT;
    private static boolean cleanedUp = false;

    private static final String PLUGINS_DIR_NAME = "plugins";

    private static final String DATA_DIR_NAME = "data";
    private static final String TMP_DIR_NAME = "tmp";

    private static final ThreadLocal<PluginContainerConfiguration> STATICALLY_ACCESSIBLE_PLUGIN_CONTAINER_CONFIGURATION = new ThreadLocal<PluginContainerConfiguration>();

    private static final ThreadLocal<PluginContainerSetup> CURRENT_SETUP = new ThreadLocal<PluginContainerSetup>();

    private static final Map<String, Object> SERVERSIDE_FAKES = Collections
        .synchronizedMap(new HashMap<String, Object>());

    protected PluginContainerConfiguration pluginContainerConfiguration;

    static {
        File f = new File(System.getProperty("java.io.tmpdir"), "plugin-container-test-" + System.currentTimeMillis());
        while (f.exists() || !f.mkdir()) {
            f = new File(System.getProperty("java.io.tmpdir"), "plugin-container-test-" + System.currentTimeMillis());
        }

        ROOT = f;
    }

    /**
     * During the tests, one might need to "fake" responses that come to the plugin container
     * from external sources, like RHQ server. These are usually mocked out using the facilities
     * provided by JMock.
     * <p>
     * This method together with {@link #setServerSideFake(String, Object)} provides a generic
     * "storage" for tests to share these objects and is only provided as a convenience to 
     * the test writers. There's nothing that would manadate using it.
     *  
     * @param name
     * @return
     */
    public static Object getServerSideFake(String name) {
        return SERVERSIDE_FAKES.get(name);
    }

    /**
     * The opposite of {@link #getServerSideFake(String)}.
     */
    public static void setServerSideFake(String name, Object object) {
        SERVERSIDE_FAKES.put(name, object);
    }

    /**
     * Returns the {@link PluginContainerConfiguration} as configured using 
     * the {@link PluginContainerSetup} annotation on the current test (or null
     * if no such thing is configured).
     * @return
     */
    public static PluginContainerConfiguration getCurrentPluginContainerConfiguration() {
        return STATICALLY_ACCESSIBLE_PLUGIN_CONTAINER_CONFIGURATION.get();
    }

    @Override
    protected void initBeforeTest(Object testObject, Method testMethod) {
        super.initBeforeTest(testObject, testMethod);

        CURRENT_SETUP.set(getSetup(testMethod));

        if (CURRENT_SETUP.get() != null) {
            try {
                initPluginContainerConfiguration(testObject, testMethod);
                initDirectoryStructure();
                deployPlugins(testObject, pluginContainerConfiguration.getPluginDirectory(), CURRENT_SETUP.get());

                PluginContainer.getInstance().setConfiguration(pluginContainerConfiguration);

                if (CURRENT_SETUP.get().clearInventoryDat()) {
                    File inventoryDat = new File(pluginContainerConfiguration.getDataDirectory(), "inventory.dat");
                    inventoryDat.delete();
                }

                if (CURRENT_SETUP.get().startImmediately()) {
                    startConfiguredPluginContainer();
                }
            } catch (Throwable t) {
                throw new IllegalStateException("Failed to setup the plugin container.", t);
            }
        }
    }

    @Override
    protected void tearDownAfterTest(ITestResult testResult) {
        try {
            PluginContainer.getInstance().shutdown();
        } finally {
            try {
                deletePlugins(pluginContainerConfiguration.getPluginDirectory());
            } catch (Throwable t) {
                //hmmm
            }

            super.tearDownAfterTest(testResult);
        }
    }

    /**
     * Starts the plugin container. This method is supposed to be called from tests that
     * need to start the plugin container manually after it has been configured using
     * the {@link PluginContainerSetup} annotation.
     */
    public static void startConfiguredPluginContainer() {
        PluginContainer.getInstance().setConfiguration(getCurrentPluginContainerConfiguration());

        PluginContainer.getInstance().initialize();

        try {
            while (!PluginContainer.getInstance().isStarted()) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("Thread interrupted while waiting for plugin container to start.", e);
        }

        InventoryManager im = PluginContainer.getInstance().getInventoryManager();
        for (int i = 0; i < CURRENT_SETUP.get().numberOfInitialDiscoveries(); ++i) {
            im.executeServerScanImmediately();
            im.executeServiceScanImmediately();
        }
    }

    /**
     * This method can be called in the after methods of the tests to clear up data
     * left after the plugin container run.
     * <p>
     * This is not done automatically to support sharing the plugin container among
     * multiple tests to simulate upgrades, etc.
     * 
     * @throws IOException
     */
    public static void clearStorageOfCurrentPluginContainer() throws IOException {
        File f = STATICALLY_ACCESSIBLE_PLUGIN_CONTAINER_CONFIGURATION.get().getPluginDirectory();
        f = f.getParentFile();

        if (f.exists()) {
            FileUtil.purge(f, false);
            //FileUtils.cleanDirectory(f);

            f.delete();
        }
    }

    /**
     * This method clears the storage of all tests made. This is useful in {@link AfterSuite}
     * method to clean up after all the tests (annotated with {@link PluginContainerSetup}) 
     * that have been run.
     * 
     * @throws IOException
     */
    public synchronized static void clearStorage() throws IOException {
        if (!cleanedUp) {
            FileUtil.purge(ROOT, false);
            //FileUtils.cleanDirectory(ROOT);
            ROOT.delete();
            cleanedUp = true;
        }
    }

    /**
     * If PluginContainerTest is used as a base class to your tests (and not as a {@link Listeners listener},
     * this method is provided to automatically clean up after all the plugin container tests that ran
     * in the test suite.
     * <p>
     * If you use PluginContainerTest as a listener, you have to call {@link #clearStorage()} method 
     * on your own.
     * 
     * @throws IOException
     */
    @AfterSuite
    public void cleanUpAfterPluginContainerTests() throws IOException {
        clearStorage();
    }

    /**
     * This method returns the {@link PluginContainerConfiguration} that will be used in 
     * the current test as was configured by the PluginContainerSetup annotation. 
     * <p>
     * If your test class inherits from PluginContainerTest, you can override this method 
     * to provide custom configuration.
     * 
     * @param testObject the object of the current test
     * @param testMethod the test method currently being executed on the test object
     * @return
     */
    protected PluginContainerConfiguration createPluginConfigurationToUse(Object testObject, Method testMethod) {
        PluginContainerSetup setup = CURRENT_SETUP.get();

        if (setup.pluginConfigurationProviderMethod().isEmpty()) {
            return createDefaultPluginConfiguration(setup, context);
        } else {
            String providerName = setup.pluginConfigurationProviderMethod();
            Class<?> testClass = testMethod.getDeclaringClass();
            try {
                Method pluginContainerProvider = testClass.getMethod(providerName, (Class<?>[]) null);

                if (!PluginContainerConfiguration.class.isAssignableFrom(pluginContainerProvider.getReturnType())) {
                    throw new IllegalStateException("The configured pluginConfigurationProviderMethod '" + providerName
                        + "' on the test class '" + testClass + "' does not return a PluginContainerConfiguration.");
                }

                return (PluginContainerConfiguration) pluginContainerProvider.invoke(testObject, (Object[]) null);
            } catch (SecurityException e) {
                throw new IllegalStateException("The configured pluginConfigurationProviderMethod '" + providerName
                    + "' could not be found on the test class '" + testClass + "'.", e);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("The configured pluginConfigurationProviderMethod '" + providerName
                    + "' could not be found on the test class '" + testClass + "'.", e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to invoke method '" + providerName + "' on test class '"
                    + testClass + "'.", e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("Failed to invoke method '" + providerName + "' on test class '"
                    + testClass + "'.", e);
            }
        }
    }

    /**
     * This method is called after the test to tear down resources associated with the 
     * current plugin container configuration.
     */
    protected void tearDownPluginContainerConfiguration() {
        pluginContainerConfiguration = null;
        STATICALLY_ACCESSIBLE_PLUGIN_CONTAINER_CONFIGURATION.set(null);
    }

    private void initPluginContainerConfiguration(Object testObject, Method testMethod) {
        pluginContainerConfiguration = createPluginConfigurationToUse(testObject, testMethod);
        STATICALLY_ACCESSIBLE_PLUGIN_CONTAINER_CONFIGURATION.set(pluginContainerConfiguration);
    }

    private static PluginContainerSetup getSetup(Method method) {
        PluginContainerSetup setup = method.getAnnotation(PluginContainerSetup.class);

        if (setup == null) {
            setup = method.getDeclaringClass().getAnnotation(PluginContainerSetup.class);
        }

        return setup;
    }

    private void initDirectoryStructure() {
        File pluginDir = pluginContainerConfiguration.getPluginDirectory();
        File dataDir = pluginContainerConfiguration.getDataDirectory();
        File tempDir = pluginContainerConfiguration.getTemporaryDirectory();

        pluginDir.mkdirs();
        dataDir.mkdirs();
        tempDir.mkdirs();
    }

    private static PluginContainerConfiguration createDefaultPluginConfiguration(PluginContainerSetup setup,
        Mockery context) {
        PluginContainerConfiguration conf = new PluginContainerConfiguration();

        File tmpDir = createTemporaryDirectory(setup);

        conf.setPluginDirectory(new File(tmpDir, PLUGINS_DIR_NAME));
        conf.setDataDirectory(new File(tmpDir, DATA_DIR_NAME));
        conf.setTemporaryDirectory(new File(tmpDir, TMP_DIR_NAME));
        conf.setInsideAgent(setup.inAgent());
        conf.setPluginFinder(new FileSystemPluginFinder(conf.getPluginDirectory()));
        conf.setCreateResourceClassloaders(false);

        //we're not interested in any scans happening out of our control
        conf.setAvailabilityScanInitialDelay(Long.MAX_VALUE);
        conf.setConfigurationDiscoveryInitialDelay(Long.MAX_VALUE);
        conf.setContentDiscoveryInitialDelay(Long.MAX_VALUE);
        conf.setEventSenderInitialDelay(Long.MAX_VALUE);
        conf.setMeasurementCollectionInitialDelay(Long.MAX_VALUE);
        conf.setServerDiscoveryInitialDelay(Long.MAX_VALUE);
        conf.setServiceDiscoveryInitialDelay(Long.MAX_VALUE);

        ServerServices serverServices = new ServerServices();
        serverServices.setBundleServerService(context.mock(BundleServerService.class));
        serverServices.setConfigurationServerService(context.mock(ConfigurationServerService.class));
        serverServices.setContentServerService(context.mock(ContentServerService.class));
        serverServices.setCoreServerService(context.mock(CoreServerService.class));
        serverServices.setDiscoveryServerService(context.mock(DiscoveryServerService.class));
        serverServices.setEventServerService(context.mock(EventServerService.class));
        serverServices.setMeasurementServerService(context.mock(MeasurementServerService.class));
        serverServices.setOperationServerService(context.mock(OperationServerService.class));
        serverServices.setResourceFactoryServerService(context.mock(ResourceFactoryServerService.class));
        serverServices.setDriftServerService(context.mock(DriftServerService.class));

        conf.setServerServices(serverServices);

        return conf;
    }

    private void deployPlugins(Object testObject, File destination, PluginContainerSetup setup) throws IOException {
        for (String plugin : setup.plugins()) {
            copyPluginToDestination(testObject, plugin, destination);
        }
    }

    private void deletePlugins(File deployDirectory) throws IOException {
        if (deployDirectory.exists()) {
            FileUtil.purge(deployDirectory, false);
        }
    }

    private static File createTemporaryDirectory(PluginContainerSetup setup) {
        String name;
        boolean mustBeNew = true;
        if (setup.sharedGroup().length() > 0) {
            name = setup.sharedGroup();
            mustBeNew = false;
        } else {
            name = Long.toString(System.currentTimeMillis());
        }

        File ret = new File(ROOT, name);

        while (mustBeNew && (ret.exists() || !ret.mkdir())) {
            ret = new File(ROOT, Long.toString(System.currentTimeMillis()));
        }

        return ret;
    }

    private File copyPluginToDestination(Object testObject, String plugin, File destination) throws IOException {
        URI pluginUri = URI.create(plugin);
        URL pluginUrl;
        if ("classpath".equals(pluginUri.getScheme())) {
            String path = pluginUri.getPath();
            pluginUrl = testObject.getClass().getResource(path);
        } else {
            pluginUrl = pluginUri.toURL();
        }

        String pluginFileName = pluginUrl.getPath().substring(pluginUrl.getPath().lastIndexOf('/') + 1);

        File pluginJar = new File(destination, pluginFileName);
        FileUtils.copyURLToFile(pluginUrl, pluginJar);

        return pluginJar;
    }
}
