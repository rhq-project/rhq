/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugin.pc;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;

import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorMetadataParser;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * A utility to test that a set of server plugins are valid.
 * This has a main() in which you can pass a list of serverplugin jar filenames
 * as arguments. If you do not pass arguments, you can set a system
 * property {@link #SYSPROP_VALIDATE_SERVERPLUGINS} whose value is a comma-separated
 * list of server plugin jar filenames. If you do not set that system property
 * and you do not pass arguments, this will look for any and all plugin descriptors
 * it can find in jar files found in the class's classloader and validate all
 * of those plugin jars.
 *
 * @author John Mazzitelli
 */
public class ServerPluginValidatorUtil {
    // this is a system property that can contain comma-separated list of plugin jar filenames
    private static final String SYSPROP_VALIDATE_SERVERPLUGINS = "rhq.test.serverplugins";

    private static final Log LOG = LogFactory.getLog(ServerPluginValidatorUtil.class);
    private static final String PLUGIN_DESCRIPTOR_PATH = "META-INF/rhq-serverplugin.xml";

    /**
     * See {@link #validatePlugins(String[])} for information on what plugins get validated.
     * 
     * The last line this will output will be "!OK!" and exit with an exit code of 0 if everything is OK.
     * The last line this will output will be "!FAILURE!" and exit with an exit code of 1 if one or more plugins failed validation.
     * 
     * @param args 0 or more plugin jar file paths
     */
    public static void main(String[] args) {
        try {
            if (new ServerPluginValidatorUtil().validatePlugins(args)) {
                System.out.println("!OK!");
                System.exit(0);
            } else {
                System.out.println("!FAILED!");
                System.exit(1);
            }
        } catch (Exception e) {
            System.out.println("!FAILED!");
            System.out.println("Could not complete server plugin validation due to the following exception:");
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    /**
     * If one or more argument strings are provided, they will be assumed to be paths to the plugin jars to validate.
     * If no arguments are passed, but the system property {@link #SYSPROP_VALIDATE_SERVERPLUGINS} is set, its value
     * is assumed to be a comma-separated list of plugin filenames.
     * If no args are passed in, this class's classloader will be used to find the plugins to validate.
     *
     * @param pluginFileNames 0 or more plugin jar file paths
     * 
     * @return 0 if all plugins validated successfully, 1 on error.
     *
     * @throws Exception on some error that caused the validation to abort
     */
    public boolean validatePlugins(String[] pluginFileNames) throws Exception {
        List<URL> jars;

        try {
            if (pluginFileNames != null && pluginFileNames.length > 0) {
                jars = new ArrayList<URL>(pluginFileNames.length);
                for (String arg : pluginFileNames) {
                    URL jarUrl = new File(arg).toURI().toURL();
                    jars.add(jarUrl);
                    LOG.info("Plugin jar: " + jarUrl);
                }
            } else {
                String sysprop = System.getProperty(SYSPROP_VALIDATE_SERVERPLUGINS);
                if (sysprop != null && !sysprop.startsWith("$")) {
                    String[] serverplugins = sysprop.split(",");
                    jars = new ArrayList<URL>(serverplugins.length);
                    for (String serverplugin : serverplugins) {
                        URL jarUrl = new File(serverplugin).toURI().toURL();
                        jars.add(jarUrl);
                        LOG.info("Plugin jar: " + jarUrl);
                    }
                } else {
                    jars = findPluginJars();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        boolean status = validatePlugins(jars);
        return status;
    }

    public boolean validatePlugins(List<URL> jars) throws Exception {

        boolean success = true; // assume all goes well; we'll set this to false if we hit any error
        ClassLoaderManager classloaderManager = null;

        try {
            Map<URL, ServerPluginDescriptorType> descriptors = new HashMap<URL, ServerPluginDescriptorType>();
            for (URL jar : jars) {
                try {
                    LOG.info("Parsing server plugin [" + jar + "]");
                    ServerPluginDescriptorType descriptor = ServerPluginDescriptorUtil.loadPluginDescriptorFromUrl(jar);
                    descriptors.put(jar, descriptor);
                } catch (Exception e) {
                    LOG.error("Failed to parse descriptor from plugin [" + jar + "]", e);
                }
            }
            // make sure we successfully processed all the plugins that are in our finder
            boolean sizesMatch = (descriptors.size() == jars.size());
            if (!sizesMatch) {
                success = false;
                LOG.error("Only [" + descriptors.size() + "] out of [" + jars.size()
                    + "] plugin descriptors are valid.");
            } else {
                LOG.info("All [" + jars.size() + "] plugin descriptors are valid.");
            }

            classloaderManager = new ClassLoaderManager(descriptors, getClass().getClassLoader(), null);

            // examine all the resource types defined in all plugins and validate some things about them
            for (Map.Entry<URL, ServerPluginDescriptorType> entry : descriptors.entrySet()) {
                URL pluginUrl = entry.getKey();
                ServerPluginDescriptorType descriptor = entry.getValue();

                String pluginName = descriptor.getName();
                if (pluginName == null) {
                    LOG.error("No plugin name in [" + pluginUrl + "]");
                    success = false;
                    continue;
                } else {
                    LOG.info("Validating plugin [" + pluginName + "] from [" + pluginUrl + "]");
                }

                ServerPluginType pluginType = new ServerPluginType(descriptor);
                PluginKey pluginKey = PluginKey.createServerPluginKey(pluginType.stringify(), pluginName);
                ClassLoader classloader = classloaderManager.obtainServerPluginClassLoader(pluginKey);
                ServerPluginEnvironment env = new ServerPluginEnvironment(pluginUrl, classloader, descriptor);

                success = success && validatePluginComponentClass(env);
                success = success && validatePluginVersion(env);
                success = success && validatePluginConfiguration(env);
                success = success && validateScheduledJobs(env);

                // now see if there is specific validation to be done based on the type of plugin
                ServerPluginValidator validator = getValidator(pluginType);
                if (validator != null) {
                    boolean validatorStatus = validator.validate(env);
                    if (!validatorStatus) {
                        LOG.error(pluginType.toString() + " validator detected a problem in plugin ["
                            + env.getPluginUrl() + "]");
                    }
                    success = success && validatorStatus;
                }
            }
        } finally {
            // do any cleanup here
            if (classloaderManager != null) {
                classloaderManager.shutdown();
            }
        }

        return success;
    }

    private boolean validateScheduledJobs(ServerPluginEnvironment env) {
        boolean success = true;
        try {
            ServerPluginDescriptorMetadataParser.getScheduledJobs(env.getPluginDescriptor());
        } catch (Exception e) {
            LOG.error("Invalid scheduled jobs for plugin [" + env.getPluginUrl() + "]", e);
            success = false;
        }
        return success;
    }

    private boolean validatePluginConfiguration(ServerPluginEnvironment env) {
        boolean success = true;
        try {
            ServerPluginDescriptorMetadataParser.getPluginConfigurationDefinition(env.getPluginDescriptor());
        } catch (Exception e) {
            LOG.error("Invalid plugin configuration for plugin [" + env.getPluginUrl() + "]", e);
            success = false;
        }
        return success;
    }

    private boolean validatePluginVersion(ServerPluginEnvironment env) {
        boolean success = true;

        try {
            File pluginFile = new File(env.getPluginUrl().toURI());
            ComparableVersion version;
            version = ServerPluginDescriptorUtil.getPluginVersion(pluginFile, env.getPluginDescriptor());
            if (version == null) {
                throw new NullPointerException("version is null");
            }
        } catch (Exception e) {
            LOG.error("Invalid version for plugin [" + env.getPluginUrl() + "]", e);
            success = false;
        }
        return success;
    }

    private boolean validatePluginComponentClass(ServerPluginEnvironment env) {

        boolean success = true;

        String clazz = ServerPluginDescriptorMetadataParser.getPluginComponentClassName(env.getPluginDescriptor());

        // the plugin component is optional - null is allowed
        if (clazz != null) {
            try {
                Class<?> componentClazz = Class.forName(clazz, false, env.getPluginClassLoader());
                if (!ServerPluginComponent.class.isAssignableFrom(componentClazz)) {
                    success = false;
                    LOG.error("Component class [" + clazz + "] from plugin [" + env.getPluginUrl()
                        + "] does not implement " + ServerPluginComponent.class);
                }
            } catch (Exception e) {
                success = false;
                LOG.error("Component class [" + clazz + "] from plugin [" + env.getPluginUrl()
                    + "] could not be loaded", e);
            }
        }
        return success;
    }

    private List<URL> findPluginJars() throws Exception {
        List<URL> retUrls = new ArrayList<URL>();

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> descriptorUrls = classloader.getResources(PLUGIN_DESCRIPTOR_PATH);
        while (descriptorUrls.hasMoreElements()) {
            URL descriptorUrl = descriptorUrls.nextElement();
            URLConnection connection = descriptorUrl.openConnection();
            if (connection instanceof JarURLConnection) {
                URL jarUrl = ((JarURLConnection) connection).getJarFileURL();
                retUrls.add(jarUrl);
                LOG.info("Found plugin jar: " + jarUrl);
            } else {
                LOG.warn("Found a plugin descriptor outside of a jar, skipping: " + descriptorUrl);
            }
        }

        return retUrls;
    }

    private ServerPluginValidator getValidator(ServerPluginType pluginType) throws Exception {

        String pkg = ServerPluginValidator.class.getPackage().getName();
        String simpleName = pluginType.getDescriptorType().getSimpleName().replaceAll("DescriptorType$", "");
        String subpkg = simpleName.replaceAll("Plugin$", "").toLowerCase();
        String className = pkg + '.' + subpkg + '.' + simpleName + "Validator";

        ServerPluginValidator validator = null;
        Class<?> clazz = null;

        try {
            clazz = Class.forName(className);
        } catch (Exception ignore) {
        }

        if (clazz != null && ServerPluginValidator.class.isAssignableFrom(clazz)) {
            validator = (ServerPluginValidator) clazz.newInstance();
        }

        return validator;
    }
}