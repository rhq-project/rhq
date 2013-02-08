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
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.StringPropertyReplacer;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.agent.AgentConfiguration;
import org.rhq.enterprise.agent.AgentConfigurationConstants;
import org.rhq.enterprise.agent.AgentConfigurationUpgrade;

/**
 * This is an MBean service that can be used to bootstrap a RHQ Agent that is embedded in a RHQ Server. This will create
 * a standalone classloader that will completely isolate the agent from any RHQ Server classloader (other than the
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
    private String configFile = "embedded-agent-configuration.xml";

    /**
     * The location of the configuration overrides properties file - can be a URL, file path or path within classloader.
     */
    private String overridesFile = null; // the jboss-service.xml service descriptor must set this

    /**
     * The preferences node name that identifies the configuration set used to configure the services.
     */
    private String preferencesNodeName;

    /**
     * Properties that will be used to override preferences found in the preferences node and the configuration
     * preferences file.
     */
    private Properties configurationOverrides = new Properties();

    /**
     * Arguments passed to the agent's main method.
     */
    private String[] arguments = null;

    /**
     * The location of the agent's resources, like jar files (jar files are located under the lib subdirectory).
     */
    private File embeddedAgentDirectory = null;

    /**
     * If <code>true</code>, this service will be told to start the agent immediately at startup by the RHQ Server.
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
    @Override
    public void startAgent() throws Exception {
        //-- if this method signature changes, you must also change StartupBean.startEmbeddedAgent

        if (!enabled.booleanValue()) {
            log.info("Will not start the embedded RHQ agent - it is disabled");
            return;
        }

        if ((agent == null) && (embeddedAgentDirectory != null)) {
            log.info("Starting the embedded RHQ Agent...");

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
                        Object agentInstance = agentConstructor
                            .newInstance(new Object[] { getAgentArgumentsAsArray() });

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
            Thread agentThread = new Thread(agentRunnable, "Embedded RHQ Agent Main");
            agentThread.setDaemon(true);
            agentThread.setContextClassLoader(agentClassLoader);
            agentThread.start();
            agentThread.join();

            // at this point in time, the embedded agent bootstrap thread has finished and
            // the agent should have been started
            if (error[0] != null) {
                log.error("Failed to start the embedded RHQ Agent. Cause: " + error[0]);
                throw error[0];
            }

            log.info("Embedded RHQ Agent has been started!");
            this.agent = agentObject[0];
        }

        return;
    }

    @Override
    public void stopAgent() throws Exception {
        if (agent != null) {
            log.info("Stopping the embedded RHQ Agent...");

            // all this funky threading/reflection is so we execute the command
            // in the isoloated context of the embedded agent
            final Exception[] error = new Exception[] { null };
            final Runnable agentRunnable = new Runnable() {
                public void run() {
                    try {
                        agent.getClass().getMethod("shutdown", new Class[0]).invoke(agent, new Object[0]);
                    } catch (Throwable t) {
                        error[0] = new Exception("Failed to stop the embedded RHQ Agent. Cause: " + t);
                    }
                }
            };

            // create our thread that executes the agent command with the isolated class loader as its context
            Thread agentThread = new Thread(agentRunnable, "Embedded RHQ Agent Shutdown Request");
            agentThread.setDaemon(true);
            agentThread.setContextClassLoader(agent.getClass().getClassLoader());
            agentThread.start();

            try {
                agentThread.join();
            } catch (InterruptedException ignore) {
            }

            if (error[0] == null) {
                agent = null;
                log.info("Embedded RHQ Agent has been stopped!");
            } else {
                log.warn(error[0].toString());
                throw error[0];
            }

            return;
        }
    }

    public void start() throws Exception {
        return; // no-op
    }

    // this method is needed so the JBossAS server will call it when shutting down the service
    @Override
    public void stop() throws Exception {
        stopAgent();
    }

    @Override
    public boolean isAgentStarted() {
        return agent != null;
    }

    @Override
    public String getAgentEnabled() {
        //-- if this method signature changes, you must also change StartupBean.startEmbeddedAgent
        return enabled.toString();
    }

    @Override
    public void setAgentEnabled(String flag) {
        enabled = Boolean.valueOf(replaceProperties(flag));
    }

    @Override
    public String getResetConfiguration() {
        return resetConfigurationAtStartup.toString();
    }

    @Override
    public void setResetConfiguration(String flag) {
        resetConfigurationAtStartup = Boolean.valueOf(replaceProperties(flag));
    }

    @Override
    public String getEmbeddedAgentDirectory() {
        return embeddedAgentDirectory.getAbsolutePath();
    }

    @Override
    public void setEmbeddedAgentDirectory(String directoryStr) {
        File directory = null;

        if (directoryStr != null) {
            directoryStr = replaceProperties(directoryStr);
            directory = new File(directoryStr);
        }

        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            enabled = Boolean.FALSE;
            directory = null;
            log.warn("Invalid embedded agent directory specified - embedded agent has been disabled: " + directory);
        }

        embeddedAgentDirectory = directory;
    }

    @Override
    public String getConfigurationFile() {
        return configFile;
    }

    @Override
    public void setConfigurationFile(String location) {
        configFile = replaceProperties(replaceProperties(location));
    }

    @Override
    public String getPreferencesNodeName() {
        return preferencesNodeName;
    }

    @Override
    public void setPreferencesNodeName(String node) {
        if (node != null) {
            node = replaceProperties(node);
        }

        if (node == null || node.trim().length() == 0) {
            try {
                node = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                node = "${jboss.bind.address}";
            }
            node = node + "-embedded";
        }
        preferencesNodeName = replaceProperties(node);
        System.setProperty("rhq.server.embedded-agent.preferences-node", preferencesNodeName);
    }

    @Override
    public Properties getConfigurationOverrides() {
        //-- if this method signature changes, you must also change StartupBean.startEmbeddedAgent
        return configurationOverrides;
    }

    @Override
    public void setConfigurationOverrides(Properties overrides) {
        //-- if this method signature changes, you must also change StartupBean.startEmbeddedAgent

        if (overrides == null) {
            return; // do nothing
        }

        for (String propName : overrides.stringPropertyNames()) {
            String origValue = overrides.getProperty(propName);
            String newValue = replaceProperties(origValue);
            overrides.setProperty(propName, newValue);
        }

        configurationOverrides.clear();
        configurationOverrides.putAll(overrides);

        // perform some checking to setup defaults if need be

        String agent_name = configurationOverrides.getProperty("rhq.agent.name", "");
        if (agent_name.trim().length() == 0 || "-".equals(agent_name)) {
            // the rhq.server.embedded-agent.name was not specified or left blank,
            // that is the system propery used to define the rhq.agent.name - so let's create our own name
            try {
                agent_name = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                agent_name = "${jboss.bind.address}";
            }
            agent_name = agent_name + "-embedded";
            agent_name = replaceProperties(agent_name);
            configurationOverrides.put("rhq.agent.name", agent_name);
        }

        return;
    }

    @Override
    public String getAgentArguments() {
        // this only returns a human readable string - to use the args, use getAgentArgumentsAsArray instead
        String[] allArgs = getAgentArgumentsAsArray();
        return Arrays.toString(allArgs);
    }

    // we want to use this because it adds some required args
    private String[] getAgentArgumentsAsArray() {
        ArrayList<String> args = new ArrayList<String>();

        if (arguments != null) {
            for (String argument : arguments) {
                String realArg = replaceProperties(argument);
                args.add(realArg);
            }
        }

        args.add("--pref=" + getPreferencesNodeName());

        return args.toArray(new String[args.size()]);
    }

    @Override
    public void setAgentArguments(String args) {
        if (args != null) {
            arguments = args.split(",");
        } else {
            arguments = new String[0];
        }
    }

    @Override
    public void reloadAgentConfiguration() throws Exception {
        getPreferencesNode().clear();

        prepareConfigurationPreferences();
    }

    @Override
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

    @Override
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

    @Override
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
        Thread agentThread = new Thread(agentRunnable, "Embedded RHQ Agent Prompt Command");
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

    @Override
    public String getConfigurationOverridesFile() {
        return overridesFile;
    }

    @Override
    public void setConfigurationOverridesFile(String location) {
        if (location == null) {
            overridesFile = null;
        } else {
            // substitute ${} replacement variables found in the location string
            overridesFile = replaceProperties(location);
            try {
                loadConfigurationOverridesFromFile();
            } catch (Exception e) {
                log.warn("Cannot load embedded agent config overrides file [" + overridesFile + "]", e);
            }
        }
    }

    private void loadConfigurationOverridesFromFile() throws Exception {
        if (overridesFile == null) {
            return; // nothing to do
        }

        InputStream is = getFileInputStream(overridesFile);
        try {
            Properties props = new Properties();
            props.load(is);
            setConfigurationOverrides(props);
        } finally {
            is.close(); // if we got here, "is" will never be null
        }

        return;
    }

    /**
     * Loads a file either from file system, from URL or from classloader. If file
     * can't be found, exception is thrown.
     * @param file_name the file whose input stream is to be returned
     * @return input stream of the file - will never be null
     * @throws IOException if the file input stream cannot be obtained
     */
    private InputStream getFileInputStream(String file_name) throws IOException {
        // first see if the file was specified as a path on the local file system
        InputStream config_file_input_stream = null;
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
            config_file_input_stream = this.getClass().getClassLoader().getResourceAsStream(file_name);
        }

        if (config_file_input_stream == null) {
            throw new IOException("Cannot find config file for embedded agent: " + file_name);
        }
        return config_file_input_stream;
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

        classpathUrls.add(embeddedAgentDirectory.toURI().toURL()); // allows the agent to find resourcees in here, like log4j.xml

        for (File jarFile : jarFiles) {
            classpathUrls.add(jarFile.toURI().toURL());
        }

        // we need jboss-modules.jar because in AS7 it is used to override JAXP impl classes and we can't not use it
        URL jbossModules = null;
        String cp = System.getProperty("java.class.path");
        if (cp != null && cp.contains("jboss-modules.jar")) {
            for (String cpPath : cp.split(File.pathSeparator)) {
                if (cpPath.contains("jboss-modules.jar")) {
                    jbossModules = new File(cpPath).toURI().toURL();
                    classpathUrls.add(jbossModules);
                    break;
                }
            }
        }

        if (jbossModules == null) {
            File rootLocation = new File(System.getProperty("jboss.home.dir"), "jboss-modules.jar");
            if (rootLocation.exists()) {
                jbossModules = rootLocation.toURI().toURL();
                classpathUrls.add(jbossModules);
            } else {
                throw new IllegalStateException(
                    "Cannot find jboss-modules.jar - it is necessary for embedded agent to run.");
            }
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
            list.add(dir.toURI().toURL());

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
                value = replaceProperties(value);

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

        InputStream config_file_input_stream = getFileInputStream(file_name);

        // We need to clear out any previous configuration in case the current config file doesn't specify a preference
        // that already exists in the preferences node.  In this case, the configuration file wants to fall back on the
        // default value and if we don't clear the preferences, we aren't guaranteed the value stored in the backing
        // store is the default value.
        // But first we need to backup these original preferences in case the config file fails to load -
        // we'll restore the original values in that case.

        try {
            Preferences preferences_node = getPreferencesNode();
            ByteArrayOutputStream backup = new ByteArrayOutputStream();
            preferences_node.exportSubtree(backup);
            preferences_node.clear();

            // now load in the preferences
            try {
                ByteArrayOutputStream raw_config_file = new ByteArrayOutputStream();
                StreamUtil.copy(config_file_input_stream, raw_config_file, true);
                String new_config = replaceProperties(raw_config_file.toString());
                ByteArrayInputStream new_config_input_stream = new ByteArrayInputStream(new_config.getBytes());
                Preferences.importPreferences(new_config_input_stream);

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
        } finally {
            // we know this is not null; if it was, we would have thrown the IOException earlier.
            config_file_input_stream.close();
        }
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

    private String replaceProperties(String str) {
        if (str == null) {
            return null;
        }

        // keep replacing properties until no more ${} tokens are left that are replaceable
        String newValue = "";
        String oldValue = str;
        while (!newValue.equals(oldValue)) {
            oldValue = str;
            newValue = StringPropertyReplacer.replaceProperties(str);
            str = newValue;
        }
        return newValue;
    }
}