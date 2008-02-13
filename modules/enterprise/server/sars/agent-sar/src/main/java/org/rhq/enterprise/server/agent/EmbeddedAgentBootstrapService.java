/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.agent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.util.StringPropertyReplacer;
import org.rhq.enterprise.agent.AgentConfiguration;
import org.rhq.enterprise.agent.AgentConfigurationConstants;
import org.rhq.enterprise.agent.AgentConfigurationUpgrade;

/**
 * This is an MBean service that can be used to bootstrap a JON Agent that is embedded in a JON Server. This will create
 * a standalone classloader that will completely isolate the agent from any JON Server classloader (other than the
 * top-level system classloader). Note that this service MBean interface does not expose any agent-specific classes to
 * clients and it does not have direct access to any class outside of a select few classes found in the agent jar that
 * is in this service's classpath.
 *
 * <p>Note that because this service is deployed with the agent jars in it, you must deploy it in exploded form.</p>
 *
 * @author John Mazzitelli
 */
public class EmbeddedAgentBootstrapService implements EmbeddedAgentBootstrapServiceMBean {
    private Log log = LogFactory.getLog(EmbeddedAgentBootstrapService.class);

    /**
     * The agent itself.
     */
    private Object agent = null;

    /**
     * The location of the configuration file - can be a file path or path within classloader.
     */
    private String configFile = "META-INF/embedded-agent-configuration.xml";

    /**
     * The preferences node name that identifies the configuration set used to configure the services.
     */
    private String preferencesNodeName = "embedded";

    /**
     * Properties that will be used to override preferences found in the preferences node and the configuration
     * preferences file.
     */
    private Properties configurationOverrides = null;

    /**
     * Arguments passed to the agent's main method.
     */
    private String[] arguments = null;

    /**
     * The location of the agent's resources, like jar files (jar files are located under the lib subdirectory).
     */
    private File embeddedAgentDirectory = null;

    /**
     * If <code>true</code>, this service will be told to start the agent immediately at startup by the JON Server.
     */
    private Boolean enabled = Boolean.FALSE;

    /**
     * If <code>true</code>, this bootstrap service will revert the agent's configuration back to the original
     * configuration file. Otherwise, the configuration will be that which is currently persisted in the preferences
     * store.
     */
    private Boolean resetConfigurationAtStartup = Boolean.FALSE;

    /**
     * This starts the agent in a completely isolated classloader in a completely isolated thread.
     *
     * @see EmbeddedAgentBootstrapServiceMBean#startAgent()
     */
    public void startAgent() throws Exception {
        //-- if this method signature changes, you must also change StartupServlet.startEmbeddedAgent

        if (!enabled.booleanValue()) {
            log.info("Will not start the embedded JON agent - it is disabled");
            return;
        }

        if ((agent == null) && (embeddedAgentDirectory != null)) {
            log.info("Starting the embedded JON Agent...");

            // we need to store the preferences prior to starting the agent
            if (resetConfigurationAtStartup.booleanValue()) {
                log.debug("Resetting the embedded agent's configuration back to its original factory settings");
                reloadAgentConfiguration();
                cleanDataDirectory();
            } else {
                log.debug("Loading the embedded agent's pre-existing configuration from preferences");
                prepareConfigurationPreferences();
            }

            // We need to build the agent's classloader so it is completely isolated.
            final URL[] jars = getEmbeddedAgentClasspath();
            final URL[] nativeDirs = getEmbeddedAgentNativeLibraryDirectories();
            final ClassLoader agentClassLoader = new EmbeddedAgentClassLoader(jars, nativeDirs);

            // we need to instantiate and start the agent in its own thread so it can have
            // a default context classloader that is our isolated classloader
            final Exception[] error = new Exception[1];
            final Object[] agentObject = new Object[1];

            Runnable agentRunnable = new Runnable() {
                public void run() {
                    try {
                        Class<?> agentClass = agentClassLoader.loadClass("org.rhq.enterprise.agent.AgentMain");
                        Constructor<?> agentConstructor = agentClass.getConstructor(new Class[] { String[].class });
                        Object agentInstance = agentConstructor.newInstance(new Object[] { getAgentArguments() });

                        agentClass.getMethod("start", new Class[0]).invoke(agentInstance, new Object[0]);

                        agentObject[0] = agentInstance;
                        error[0] = null;
                    } catch (Throwable t) {
                        agentObject[0] = null;
                        error[0] = new Exception("Cannot start the embedded agent. Cause: " + t, t);
                    }
                }
            };

            // create our thread that starts the agent with the isolated class loader as its context
            Thread agentThread = new Thread(agentRunnable, "Embedded JON Agent Main");
            agentThread.setDaemon(true);
            agentThread.setContextClassLoader(agentClassLoader);
            agentThread.start();
            agentThread.join();

            // at this point in time, the embedded agent bootstrap thread has finished and
            // the agent should have been started
            if (error[0] != null) {
                log.error("Failed to start the embedded JON Agent. Cause: " + error[0]);
                throw error[0];
            }

            log.info("Embedded JON Agent has been started!");
            this.agent = agentObject[0];
        }

        return;
    }

    public void stopAgent() throws Exception {
        if (agent != null) {
            log.info("Stopping the embedded JON Agent...");

            // all this funky threading/reflection is so we execute the command
            // in the isoloated context of the embedded agent
            final Exception[] error = new Exception[] { null };
            final Runnable agentRunnable = new Runnable() {
                public void run() {
                    try {
                        agent.getClass().getMethod("shutdown", new Class[0]).invoke(agent, new Object[0]);
                    } catch (Throwable t) {
                        error[0] = new Exception("Failed to stop the embedded JON Agent. Cause: " + t);
                    }
                }
            };

            // create our thread that executes the agent command with the isolated class loader as its context
            Thread agentThread = new Thread(agentRunnable, "Embedded JON Agent Shutdown Request");
            agentThread.setDaemon(true);
            agentThread.setContextClassLoader(agent.getClass().getClassLoader());
            agentThread.start();

            try {
                agentThread.join();
            } catch (InterruptedException ignore) {
            }

            if (error[0] == null) {
                agent = null;
                log.info("Embedded JON Agent has been stopped!");
            } else {
                log.warn(error[0].toString());
                throw error[0];
            }

            return;
        }
    }

    // this method is needed so the JBossAS server will call it when shutting down the service
    public void stop() throws Exception {
        stopAgent();
    }

    public boolean isAgentStarted() {
        return agent != null;
    }

    public String getAgentEnabled() {
        //-- if this method signature changes, you must also change StartupServlet.startEmbeddedAgent
        return enabled.toString();
    }

    public void setAgentEnabled(String flag) {
        enabled = Boolean.valueOf(flag.trim());
    }

    public String getResetConfiguration() {
        return resetConfigurationAtStartup.toString();
    }

    public void setResetConfiguration(String flag) {
        resetConfigurationAtStartup = Boolean.valueOf(flag.trim());
    }

    public File getEmbeddedAgentDirectory() {
        return embeddedAgentDirectory;
    }

    public void setEmbeddedAgentDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            enabled = Boolean.FALSE;
            embeddedAgentDirectory = null;
            throw new IllegalArgumentException(
                "Invalid embedded agent directory specified - embedded agent has been disabled: " + directory);
        }

        embeddedAgentDirectory = directory;
    }

    public String getConfigurationFile() {
        return configFile;
    }

    public void setConfigurationFile(String location) {
        configFile = StringPropertyReplacer.replaceProperties(location);
    }

    public String getPreferencesNodeName() {
        return preferencesNodeName;
    }

    public void setPreferencesNodeName(String node) {
        preferencesNodeName = node;
    }

    public Properties getConfigurationOverrides() {
        return configurationOverrides;
    }

    public void setConfigurationOverrides(Properties overrides) {
        configurationOverrides = overrides;
    }

    public String[] getAgentArguments() {
        ArrayList<String> args = new ArrayList<String>();

        if (arguments != null) {
            for (String argument : arguments) {
                args.add(argument);
            }
        }

        args.add("--pref");
        args.add(getPreferencesNodeName());

        return args.toArray(new String[args.size()]);
    }

    public void setAgentArguments(String[] args) {
        arguments = args;
    }

    public void reloadAgentConfiguration() throws Exception {
        getPreferencesNode().clear();

        prepareConfigurationPreferences();
    }

    public void cleanDataDirectory() {
        AgentConfiguration config = new AgentConfiguration(getPreferencesNode());
        File data_dir = config.getDataDirectory();

        cleanDataFile(data_dir);

        // it is conceivable the comm services data directory was configured in a different
        // place than where the agent's data directory is - make sure we clean out that other data dir
        File comm_data_dir = config.getServiceContainerPreferences().getDataDirectory();
        if (!comm_data_dir.getAbsolutePath().equals(data_dir.getAbsolutePath())) {
            cleanDataFile(comm_data_dir);
        }

        return;
    }

    public Properties getAgentConfiguration() {
        try {
            Properties properties = new Properties();
            Preferences prefs = getPreferencesNode();

            for (String key : prefs.keys()) {
                properties.setProperty(key, prefs.get(key, "?"));
            }

            return properties;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void executeAgentPromptCommand(final String command) {
        // all this funky threading/reflection is so we execute the command
        // in the isoloated context of the embedded agent
        final Exception[] error = new Exception[] { null };
        final Runnable agentRunnable = new Runnable() {
            public void run() {
                try {
                    Class<?> agentClass = agent.getClass();
                    Method agentMethod = agentClass.getMethod("executePromptCommand", new Class[] { String.class });

                    agentMethod.invoke(agent, new Object[] { command });
                } catch (Throwable t) {
                    error[0] = new Exception("Cannot execute embedded agent prompt command [" + command + "]. Cause: "
                        + t);
                }
            }
        };

        // create our thread that executes the agent command with the isolated class loader as its context
        Thread agentThread = new Thread(agentRunnable, "Embedded JON Agent Prompt Command");
        agentThread.setDaemon(true);
        agentThread.setContextClassLoader(agent.getClass().getClassLoader());
        agentThread.start();

        try {
            agentThread.join();
            if (error[0] == null) {
                log.info("Embedded agent executed the command [" + command
                    + "] - see the embedded agent output file for the results");
            } else {
                log.warn(error[0].toString());
            }
        } catch (InterruptedException ignore) {
        }

        return;
    }

    /**
     * This will return URLs to all of the jars and resources that the embedded agent will have access to in its
     * isolated classloader. Only those classes within these jars will be accessible to the embedded agent. Even classes
     * that are in this bootstrap service are not going to be available to it.
     *
     * @return URLs to embedded agent jars that will be in the embedded agent's classloader
     *
     * @throws Exception if failed to find the jars
     */
    private URL[] getEmbeddedAgentClasspath() throws Exception {
        File lib = new File(embeddedAgentDirectory, "lib");

        if (!lib.exists() || !lib.isDirectory()) {
            throw new Exception("There is no lib directory under [" + embeddedAgentDirectory
                + "]; cannot get agent jars");
        }

        File[] jarFiles = lib.listFiles();
        ArrayList<URL> classpathUrls = new ArrayList<URL>(jarFiles.length + 1);

        classpathUrls.add(embeddedAgentDirectory.toURL()); // allows the agent to find resourcees in here, like log4j.xml

        for (File jarFile : jarFiles) {
            classpathUrls.add(jarFile.toURL());
        }

        return classpathUrls.toArray(new URL[classpathUrls.size()]);
    }

    /**
     * Returns URLs to all directories where the agent's native libraries can be found.
     *
     * @return URLs where native libraries can be found
     *
     * @throws Exception if failed to find the native library directories
     */
    private URL[] getEmbeddedAgentNativeLibraryDirectories() throws Exception {
        ArrayList<URL> nativeLibraryDirUrls = new ArrayList<URL>();

        // we will assume our native libraries are under the same lib directory where the jars are
        // we will also look for native libraries in subdirectories under lib
        File lib = new File(embeddedAgentDirectory, "lib");

        if (!lib.exists() || !lib.isDirectory()) {
            throw new Exception("There is no lib directory under [" + embeddedAgentDirectory
                + "]; cannot get native library directories");
        }

        addDirectories(lib, nativeLibraryDirUrls);

        return nativeLibraryDirUrls.toArray(new URL[nativeLibraryDirUrls.size()]);
    }

    private void addDirectories(File dir, ArrayList<URL> list) throws Exception {
        if (dir.isDirectory()) {
            // add the given directory to the list and add all its subdirectories to the list
            list.add(dir.toURL());

            for (File dirEntry : dir.listFiles()) {
                addDirectories(dirEntry, list);
            }
        }

        return;
    }

    /**
     * This will ensure the agent's configuration preferences are populated. If need be, the configuration file is
     * loaded and all overrides are overlaid on top of the preferences. The preferences are also upgraded to ensure they
     * conform to the latest configuration schema version.
     *
     * @return the agent configuration
     *
     * @throws Exception
     */
    private AgentConfiguration prepareConfigurationPreferences() throws Exception {
        Preferences preferences_node = getPreferencesNode();
        AgentConfiguration config = new AgentConfiguration(preferences_node);

        if (config.getAgentConfigurationVersion() == 0) {
            config = loadConfigurationFile();
        }

        // now that the configuration preferences are loaded, we need to override them with any bootstrap override properties
        Properties overrides = getConfigurationOverrides();
        if (overrides != null) {
            for (Map.Entry<Object, Object> entry : overrides.entrySet()) {
                String key = entry.getKey().toString();
                String value = entry.getValue().toString();

                // allow ${var} notation in the values so we can provide variable replacements in the values
                value = StringPropertyReplacer.replaceProperties(value);

                preferences_node.put(key, value);
            }
        }

        // let's make sure our configuration is upgraded to the latest schema
        AgentConfigurationUpgrade.upgradeToLatest(config.getPreferences());

        return config;
    }

    /**
     * Loads the {@link #getConfigurationFile() configuration file}. The file location will first be checked for
     * existence on the file system and then as a URL. If it cannot be found, it will be assumed the file location
     * specifies the file as found in the current class loader and the file will be searched there. An exception is
     * thrown if the file cannot be found anywhere.
     *
     * @return the configuration that was loaded
     *
     * @throws IOException                       if failed to load the configuration file
     * @throws InvalidPreferencesFormatException if the configuration file had an invalid format
     * @throws BackingStoreException             if failed to access the preferences persistence store
     * @throws Exception                         on other failures
     */
    private AgentConfiguration loadConfigurationFile() throws Exception {
        String file_name = getConfigurationFile();
        String preferences_node_name = getPreferencesNodeName();
        InputStream config_file_input_stream = null;

        // first see if the file was specified as a path on the local file system
        try {
            File config_file = new File(file_name);

            if (config_file.exists()) {
                config_file_input_stream = new FileInputStream(config_file);
            }
        } catch (Exception e) {
            // isn't really an error - this just isn't a file on the local file system
        }

        // see if the file was specified as a URL
        if (config_file_input_stream == null) {
            try {
                URL config_file = new URL(file_name);

                config_file_input_stream = config_file.openStream();
            } catch (Exception e) {
                // isn't really an error - this just isn't a URL
            }
        }

        // if neither a file path or URL, assume the config file can be found in the classloader
        if (config_file_input_stream == null) {
            config_file_input_stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(file_name);
        }

        if (config_file_input_stream == null) {
            throw new IOException("Bad config file: " + file_name);
        }

        // We need to clear out any previous configuration in case the current config file doesn't specify a preference
        // that already exists in the preferences node.  In this case, the configuration file wants to fall back on the
        // default value and if we don't clear the preferences, we aren't guaranteed the value stored in the backing
        // store is the default value.
        // But first we need to backup these original preferences in case the config file fails to load -
        // we'll restore the original values in that case.

        Preferences preferences_node = getPreferencesNode();
        ByteArrayOutputStream backup = new ByteArrayOutputStream();
        preferences_node.exportSubtree(backup);
        preferences_node.clear();

        // now load in the preferences
        try {
            Preferences.importPreferences(config_file_input_stream);

            if (new AgentConfiguration(preferences_node).getAgentConfigurationVersion() == 0) {
                throw new IllegalArgumentException("Bad node name: " + preferences_node_name);
            }
        } catch (Exception e) {
            // a problem occurred importing the config file; let's restore our original values
            try {
                Preferences.importPreferences(new ByteArrayInputStream(backup.toByteArray()));
            } catch (Exception e1) {
                // its conceivable the same problem occurred here as with the original exception (backing store problem?)
                // let's throw the original exception, not this one
            }

            throw e;
        }

        AgentConfiguration agent_configuration = new AgentConfiguration(preferences_node);

        return agent_configuration;
    }

    /**
     * Returns the preferences for this agent. The node returned is where all preferences are to be stored.
     *
     * @return the agent preferences
     */
    private Preferences getPreferencesNode() {
        Preferences top_node = Preferences.userRoot().node(AgentConfigurationConstants.PREFERENCE_NODE_PARENT);
        Preferences preferences_node = top_node.node(getPreferencesNodeName());

        return preferences_node;
    }

    /**
     * This will delete the given file and if its a directory, will recursively delete its contents and its
     * subdirectories.
     *
     * @param file the file/directory to delete
     */
    private void cleanDataFile(File file) {
        boolean deleted;

        File[] doomed_files = file.listFiles();
        if (doomed_files != null) {
            for (File doomed_file : doomed_files) {
                cleanDataFile(doomed_file); // call this method recursively
            }
        }

        deleted = file.delete();

        if (!deleted) {
            log.warn("Cannot clean data file [" + file + "]");
        }

        return;
    }
}