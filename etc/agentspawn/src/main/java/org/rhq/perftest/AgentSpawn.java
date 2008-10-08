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
package org.rhq.perftest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.Preferences;

/**
 * This is a Java program that enables you to launch multiple agents within a single VM. Use this for performance
 * testing when you want to run alot of agents all hitting a single server.
 *
 * <p>See the PROP_XXX constants for the different system properties you can set to control the behavior and setup of
 * the spawned agents. They have "sensible" defaults, but you'll usually want to set one or more explicitly to define
 * things such as the number of agents you want to run ({@link #PROP_SPAWNCOUNT}), what the starting port is that you
 * want the agents to listen to ({@link #PROP_STARTPORT}) and where the agent distribution exists so the spawned agents
 * can find their dependency jars ({@link #PROP_AGENTDIST}).</p>
 *
 * <p>The spawned agents will all have the same base configuration defined by a single agent configuration .xml file.
 * This Java program is built with a default agent configuration that should ship within its jar so you typically don't
 * have to create your own agent configuration .xml file. If you do have a .xml file that you want to use as the agents'
 * base configurations, point the system property {@link #PROP_CONFIGFILE} to the file path where your .xml file
 * exists.</p>
 *
 * <p>You can override any of the base configuration's preferences by setting a system property with the same name as
 * the preference. This is useful, for example, if you do not want the agents to download and maintain their own copies
 * of plugins. You can copy all plugins in a single location and tell all spawned agents to share those via: <code>
 * -Drhq.agent.plugins.directory=/shared-plugins -Drhq.agent.update-plugins-at-startup=false</code>. In fact, that
 * specific use-case is so useful, setting the system property {@link #PROP_PLUGINSDIR} will set those two properties
 * for you appropriately.</p>
 *
 * @author John Mazzitelli
 */
public class AgentSpawn {
    // MAKE SURE THESE MATCH WHAT THE REAL AGENT EXPECTS
    private static final String PREFERENCE_NODE_PARENT = "rhq-agent";
    private static final String PREF_SCHEMA_VERSION = "rhq.agent.configuration-schema-version";
    private static final String PREF_AGENT_NAME = "rhq.agent.name";
    private static final String PREF_AGENT_BIND_ADDRESS = "rhq.communications.connector.bind-address";
    private static final String PREF_AGENT_BIND_PORT = "rhq.communications.connector.bind-port";
    private static final String PREF_PLUGINS_DIRECTORY = "rhq.agent.plugins.directory";
    private static final String PREF_COMM_DATA_DIRECTORY = "rhq.communications.data-directory";
    private static final String PREF_AGENT_DATA_DIRECTORY = "rhq.agent.data-directory";
    private static final String PREF_MBS_NAME = "rhq.communications.service-container.mbean-server-name";
    private static final String PREF_UPDATE_PLUGINS_AT_STARTUP = "rhq.agent.update-plugins-at-startup";

    // PERFTEST PROPERTIES THIS CLASS USES TO SPAWN OUR TEST AGENTS
    private static final String PROP_AGENTDIST = "perftest.agentdist";
    private static final String PROP_CONFIGFILE = "perftest.config";
    private static final String PROP_PREF = "perftest.pref";
    private static final String PROP_SPAWNDIR = "perftest.spawndir";
    private static final String PROP_SPAWNCOUNT = "perftest.spawncount";
    private static final String PROP_STARTPORT = "perftest.startport";
    private static final String PROP_BINDADDRESS = "perftest.bindaddress";
    private static final String PROP_CMDLINE = "perftest.cmdline";
    private static final String PROP_PLUGINSDIR = "perftest.plugins";
    private static final String PROP_STARTPAUSE = "perftest.startpause";
    private static final String PROP_PIDFILE = "perftest.pidfile";

    public static void main(String[] args) throws Exception {
        System.out.println("==AGENT SPAWN PERFTEST UTILITY==");
        System.out.println("Pid File/Pid: (" + PROP_PIDFILE + ")=" + generatePidFile());
        System.out.println("Agent Distribution (" + PROP_AGENTDIST + ")=" + getAgentDistribution());
        System.out.println("Configuration File (" + PROP_CONFIGFILE + ")=" + getConfigurationFile());
        System.out.println("Preferences Node Name (" + PROP_PREF + ")=" + getTemplatePreferencesNodeName());
        System.out.println("Spawned Agents Directory (" + PROP_SPAWNDIR + ")=" + getSpawnDirectory());
        System.out.println("# Agents To Be Created (" + PROP_SPAWNCOUNT + ")=" + getSpawnCount());
        System.out.println("Start Port Of Agent Listeners (" + PROP_STARTPORT + ")=" + getStartPort());
        System.out.println("Agents Command Line Arguments (" + PROP_CMDLINE + ")=" + System.getProperty(PROP_CMDLINE));
        System.out.println("Shared Plugins Directory (" + PROP_PLUGINSDIR + ")=" + getSharedPluginsDirectory());
        System.out.println("Pause (ms) Between Each Start (" + PROP_STARTPAUSE + ")=" + getStartPause());

        Map<String, String> overrideProps = new HashMap<String, String>();
        populateOverrideProps(overrideProps);
        for (Map.Entry<String, String> entry : overrideProps.entrySet()) {
            System.out.println("Override property: " + entry.getKey() + '=' + entry.getValue());
        }

        if (args.length == 0) {
            usage();
            return;
        }

        if (args[0].equals("clean")) {
            clean(args);
        } else if (args[0].equals("spawn")) {
            spawn();
        } else if (args[0].equals("start")) {
            spawn();
            start(args);
        } else {
            usage();
        }
    }

    private static void start(String[] args) throws Exception {
        // so we pick up our own log4j for the spawned agents but allow someone to override it
        if (System.getProperty("log4j.configuration") == null) {
            System.setProperty("log4j.configuration", "spawn-log4j.xml");
        }

        final int count = getSpawnCount();
        final int startPort = getStartPort();
        final long startPause = getStartPause();
        final boolean dryrun = ((args.length == 2) && args[1].equals("dryrun"));

        System.out.println("Starting [" + count + "] agents beginning at port [" + startPort + "]...");

        URL[] nativeDirs = getSpawnedAgentNativeLibraryDirectories();
        URL[] jniJars = getSpawnedAgentJNIClasspath();
        ClassLoader topClassloader = new SpawnedAgentClassLoader(jniJars, nativeDirs, null);

        for (int i = 0; i < count; i++) {
            System.out.println("Agent #" + i + ": " + (startPort + i));

            // We need to build the agent's classloader so it is completely isolated.
            URL[] jars = getSpawnedAgentClasspath();
            URL[] endorsedJars = getSpawnedAgentEndorsedClasspath();
            URL[] allJars = new URL[jars.length + endorsedJars.length];
            System.arraycopy(endorsedJars, 0, allJars, 0, endorsedJars.length);
            System.arraycopy(jars, 0, allJars, endorsedJars.length, jars.length);
            final ClassLoader agentClassLoader = new SpawnedAgentClassLoader(allJars, null, topClassloader);

            final int agentId = i;

            // we need to instantiate and start the agent in its own thread so it can have
            // a default context classloader that is our isolated classloader
            Runnable agentRunnable = new Runnable() {
                public void run() {
                    try {
                        Class<?> agentClass = agentClassLoader.loadClass("org.rhq.enterprise.agent.AgentMain");
                        Class<?>[] params = new Class[] { String[].class };
                        Object[] args = getAgentCommandLine(agentId);

                        Constructor<?> agentConstructor = agentClass.getConstructor(params);
                        Method startMethod = agentClass.getMethod("start", new Class[0]);

                        if (!dryrun) {
                            Object agentInstance = agentConstructor.newInstance(new Object[] { args });
                            startMethod.invoke(agentInstance, new Object[0]);
                        } else {
                            // this is a dry run - just output what we would have done
                            System.out.print("DRYRUN: ");
                            System.out.print(agentConstructor.getName());
                            System.out.println(" with args " + Arrays.toString(args));
                        }
                    } catch (Throwable t) {
                        System.out.println("Cannot start agent #" + agentId + " on port " + (startPort + agentId)
                            + ". Cause: " + t);
                        t.printStackTrace();
                    }
                }
            };

            // create our thread that starts the agent with the isolated class loader as its context
            Thread agentThread = new Thread(agentRunnable, "Starting Agent " + (startPort + i));
            agentThread.setDaemon(true);
            agentThread.setContextClassLoader(agentClassLoader);
            Thread.sleep(startPause);
            agentThread.start();

            if (dryrun) { // in a dryrun, the thread finishes fast, let's wait for it so we output the messages in order
                agentThread.join();
            }
        }

        int endPort = (startPort + count - 1);
        System.out.println("Started " + count + " agents running on ports " + startPort + "-" + endPort);

        // if this is not a dryrun, let's hang this main thread so we keep all agents alive
        // effectively, this means you need to forcibly kill this VM when not in a dry run
        if (!dryrun) {
            synchronized (AgentSpawn.class) {
                AgentSpawn.class.wait();
            }
        }

        return;
    }

    private static void spawn() throws Exception {
        System.out.println("Loading template configuration...");
        Preferences templatePrefs = loadConfigurationFile();

        System.out.println("Spawning agent configurations...");
        spawnAgentConfigurations(templatePrefs);
    }

    private static void usage() {
        System.out.println("Commands: clean | spawn | start");
        System.out.println("\tclean [all]: cleans spawned agents' files and preferences,\n"
            + "\t             specify 'all' and this will attempt to clean up\n"
            + "\t             everything it ever created");
        System.out.println("\tspawn: spawns the agents files and preferences, but does not start them");
        System.out.println("\tstart [dryrun]: spawns and starts the agents\n"
            + "\t                specify 'dryrun' to not really start them\n");
        return;
    }

    private static void clean(String[] args) throws Exception {
        System.out.println("Cleaning spawned agents...");
        Map<Integer, Preferences> allPrefs = getSpawnedAgentPreferencesNodes();
        for (Preferences doomedPrefs : allPrefs.values()) {
            // even if "all" was specified, let's do this just in case the
            // data and/or plugin directories were specified somewhere else
            // other than under the spawn directory (which "all" will scrub)
            String data = doomedPrefs.get(PREF_AGENT_DATA_DIRECTORY, null);
            if (data != null) {
                purge(new File(data), true);
            }

            String plugins = doomedPrefs.get(PREF_PLUGINS_DIRECTORY, null);
            if (plugins != null) {
                purge(new File(plugins), true);
            }

            doomedPrefs.removeNode();
        }

        if ((args.length == 2) && args[1].equals("all")) {
            System.out.println("Performing a full clean - will attempt to scrub everything...");
            Preferences topNode = Preferences.userRoot().node(PREFERENCE_NODE_PARENT);
            for (String childNode : topNode.childrenNames()) {
                if (childNode.startsWith("spawn")) {
                    topNode.node(childNode).removeNode();
                }
            }

            purge(getSpawnDirectory(), true);
        }

        return;
    }

    private static Map<Integer, Preferences> getSpawnedAgentPreferencesNodes() {
        Map<Integer, Preferences> allPrefs = new HashMap<Integer, Preferences>();

        int count = getSpawnCount();

        for (int i = 0; i < count; i++) {
            // keyed on "agent ID" which is just a number from 0 - (count-1)
            allPrefs.put(Integer.valueOf(i), getPreferencesNode(i));
        }

        return allPrefs;
    }

    private static void spawnAgentConfigurations(Preferences templatePrefs) throws Exception {
        // first collect our global overrides that may have been defined
        Map<String, String> overrideProps = new HashMap<String, String>();

        // if the agents are to share the same set of plugins, set the proper override properties to enable this
        String sharedPluginsDir = getSharedPluginsDirectory();
        if (sharedPluginsDir != null) {
            overrideProps.put(PREF_PLUGINS_DIRECTORY, sharedPluginsDir);
            overrideProps.put(PREF_UPDATE_PLUGINS_AT_STARTUP, "false");
        }

        populateOverrideProps(overrideProps);

        // create some things on the filesystem that are needed by the spawned agents
        File spawnDir = getSpawnDirectory();
        File data = new File(spawnDir, "data"); // this is the root directory of all our data directories
        File plugins = new File(spawnDir, "plugins"); // this is the root directory of all our plugins directories

        data.mkdirs();
        plugins.mkdirs();

        if (!data.isDirectory()) {
            throw new RuntimeException("Cannot create root data directory: " + data);
        }

        if (!plugins.isDirectory()) {
            throw new RuntimeException("Cannot create root plugins directory: " + plugins);
        }

        // create all the spawned agents' configurations now
        int count = getSpawnCount();
        int startPort = getStartPort();

        for (int i = 0; i < count; i++) {
            // get the spawned agent's config and clear it out of any old config that might still exist
            Preferences prefs = getPreferencesNode(i);
            prefs.clear();

            // start out by giving the spawned agent the template configuration
            for (String key : templatePrefs.keys()) {
                prefs.put(key, templatePrefs.get(key, "**SHOULD NEVER SEE THIS**"));
            }

            // now override some configuration settings so they don't conflict with other spawned agents
            int spawnPort = startPort + i; // this is the port our spawned agent will listen to

            prefs.put(PREF_AGENT_NAME, getPreferencesNodeName(i));
            prefs.put(PREF_AGENT_BIND_ADDRESS, getBindAddress());
            prefs.putInt(PREF_AGENT_BIND_PORT, spawnPort);
            prefs.put(PREF_AGENT_DATA_DIRECTORY, new File(data, Integer.toString(spawnPort)).getAbsolutePath());
            prefs.put(PREF_COMM_DATA_DIRECTORY, new File(data, Integer.toString(spawnPort)).getAbsolutePath());
            prefs.put(PREF_PLUGINS_DIRECTORY, new File(plugins, Integer.toString(spawnPort)).getAbsolutePath());
            prefs.put(PREF_MBS_NAME, getPreferencesNodeName(i));

            // if the user specified some overrides, set them now
            // this is useful if you do not want the agents to download and maintain
            // their own copies of plugins.  You can copy all plugins in a single location
            // and tell all spawned agents to share those via:
            //    -Drhq.agent.plugins.directory=/shared-plugins
            //    -Drhq.agent.update-plugins-at-startup=false
            for (Map.Entry<String, String> entry : overrideProps.entrySet()) {
                prefs.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Puts all relevent system properties in the given map.
     *
     * @param overrideProps
     */
    private static void populateOverrideProps(Map<String, String> overrideProps) {
        Properties sysProps = System.getProperties();
        for (Map.Entry<Object, Object> entry : sysProps.entrySet()) {
            if (entry.getKey().toString().startsWith("rhq.")) {
                overrideProps.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
    }

    /**
     * Returns the file path where an agent distribution can be found. This is used to location the jars and native
     * libraries required to run an agent.
     *
     * @return the location of the agent distro where all dependency jars can be found
     *
     * @throws RuntimeException
     */
    private static String getAgentDistribution() {
        // default uses the maven build layout and assume we are running out of the perftest target dir
        String distString = System.getProperty(PROP_AGENTDIST,
            "../../../modules/enterprise/agent/target/rhq-agent-1.2.0-SNAPSHOT");
        File dist = new File(distString);
        if (!dist.isDirectory()) {
            throw new RuntimeException("Missing the agent distribution at : " + distString + ". Please set ["
                + PROP_AGENTDIST + "] to point to the exploded agent distribution (not a zip/tar file).");
        }

        return distString;
    }

    /**
     * The name of the configuration file. This is the template of all spawned agents. A spawned agent will start out
     * with the configuration found in this file, with certain preferences overridden so they don't conflict with other
     * agents. This will either be a file path, URL or resource path found on the classpath.
     *
     * @return location of the template agent configuration
     */
    private static String getConfigurationFile() {
        return System.getProperty(PROP_CONFIGFILE, "template-configuration.xml");
    }

    /**
     * This is the preferences node name of the configuration file. The true name of the spawned agents will be a
     * derivitive of this name.
     *
     * @return configuration file's preferences node name
     */
    private static String getTemplatePreferencesNodeName() {
        return System.getProperty(PROP_PREF, "spawn");
    }

    /**
     * The name of the preferences node for the identified spawned agent. This will be a derivitive of
     * {@link #getTemplatePreferencesNodeName()}.
     *
     * @param  agentId ID of the spawned agent whose preferences node name is to be returned
     *
     * @return the preferences node name of the spawned agent identified by the given ID
     */
    private static String getPreferencesNodeName(int agentId) {
        return getTemplatePreferencesNodeName() + '_' + (getStartPort() + agentId);
    }

    /**
     * Gets the preferences node of the template configuration as defined in the {@link #getConfigurationFile()}. This
     * is the base configuration of all spawned agents. Some of these preferences will be overridden so they do not
     * conflict with other agents.
     *
     * @return template configuration preferences
     */
    private static Preferences getTemplatePreferencesNode() {
        Preferences topNode = Preferences.userRoot().node(PREFERENCE_NODE_PARENT);
        Preferences preferencesNode = topNode.node(getTemplatePreferencesNodeName());

        return preferencesNode;
    }

    /**
     * Gets the preferences node of the identified spawned agent. This consists of the
     * {@link #getTemplatePreferencesNode() template configuration} with some preferences overridden so they do not
     * conflict with other agents.
     *
     * @param  agentId ID of the spawned agent whose preferences are to be returned
     *
     * @return spawned agent's preferences
     */
    private static Preferences getPreferencesNode(int agentId) {
        Preferences topNode = Preferences.userRoot().node(PREFERENCE_NODE_PARENT);
        Preferences preferencesNode = topNode.node(getPreferencesNodeName(agentId));

        return preferencesNode;
    }

    private static File getSpawnDirectory() {
        File dir;

        String dirString = System.getProperty(PROP_SPAWNDIR);
        if ((dirString == null) || (dirString.trim().length() == 0)) {
            String root = null; // System.getProperty("java.io.tmpdir")
            dir = new File(root, "spawn");
        } else {
            dir = new File(dirString.trim());
        }

        if (!dir.isDirectory()) {
            dir.mkdirs();
            if (!dir.isDirectory()) {
                throw new RuntimeException("Invalid " + PROP_SPAWNDIR + " : " + dirString);
            }
        }

        return dir;
    }

    private static int getSpawnCount() {
        String countString = System.getProperty(PROP_SPAWNCOUNT, "2");
        int count = Integer.parseInt(countString);
        if (count < 1) {
            throw new RuntimeException("Invalid " + PROP_SPAWNCOUNT + " : " + countString);
        }

        return count;
    }

    private static int getStartPort() {
        String portString = System.getProperty(PROP_STARTPORT, "36163");
        int port = Integer.parseInt(portString);
        if ((port < 1) || (port > 65535)) {
            throw new RuntimeException("Invalid " + PROP_STARTPORT + " : " + portString);
        }

        return port;
    }

    /**
     * This will return the path of the shared plugins directory, if defined. If the spawned agents will not share a set
     * of plugins, this will return <code>null</code>. If a shared plugins directory was specified, but that directory
     * does not exist or is invalid, an exception is thrown.
     *
     * @return the location where the shared plugins exist (may be <code>null</code> if not defined)
     */
    private static String getSharedPluginsDirectory() {
        String dir = System.getProperty(PROP_PLUGINSDIR);

        if ((dir != null) && (dir.trim().length() > 0)) {
            if (!new File(dir.trim()).isDirectory()) {
                throw new RuntimeException("Specified plugins directory does not exist: " + dir);
            }
        } else {
            dir = null;
        }

        return dir;
    }

    /**
     * The number of milliseconds that should pass before starting an agent.
     *
     * @return pause time in milliseconds
     */
    private static long getStartPause() {
        String timeString = System.getProperty(PROP_STARTPAUSE, "1000");
        long time = Long.parseLong(timeString);
        if (time < 0) {
            throw new RuntimeException("Invalid " + PROP_STARTPAUSE + " : " + timeString);
        }

        return time;
    }

    /**
     * If this AgentSpawn process was told to generate a pid file, it does it now and returns
     * a string with the name of the pid file and the PID.
     * 
     * @return string identifying the pid file and the pid of the process
     */
    private static String generatePidFile() {
        String pidfile = System.getProperty(PROP_PIDFILE);
        if (pidfile == null) {
            return "<not generated/unknown pid>";
        }

        // here we assume the MBean name has the pid in it - cross your fingers
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            int endIndex = name.indexOf("@");
            int pid = Integer.parseInt(name.substring(0, endIndex));
            FileOutputStream fos = new FileOutputStream(new File(pidfile));
            fos.write(Integer.toString(pid).getBytes());
            fos.close();
            return pidfile + " (" + pid + ")";
        } catch (Throwable t) {
            return "<cannot determine pid: " + t + ">";
        }
    }

    /**
     * Note that the command line arguments should be separated by a comma.
     *
     * @param  agentId
     *
     * @return the command line that should be used to pass to the identified agent's main method
     */
    private static String[] getAgentCommandLine(int agentId) {
        String cmdline = System.getProperty(PROP_CMDLINE);
        String[] arguments;

        if ((cmdline == null) || (cmdline.trim().length() == 0)) {
            arguments = new String[0];
        } else {
            arguments = cmdline.trim().split(",");
        }

        ArrayList<String> argsList = new ArrayList<String>();

        for (String argument : arguments) {
            argsList.add(argument);
        }

        String prefs = getPreferencesNodeName(agentId);

        argsList.add("--daemon");
        argsList.add("--pref");
        argsList.add(prefs);
        argsList.add("--output");
        argsList.add(new File(getSpawnDirectory(), "logs/" + prefs + ".out").getAbsolutePath());

        return argsList.toArray(new String[argsList.size()]);
    }

    private static String getBindAddress() {
        String bindAddress = System.getProperty(PROP_BINDADDRESS);

        if ((bindAddress == null) || (bindAddress.trim().length() == 0)) {
            try {
                bindAddress = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                bindAddress = "127.0.0.1";
            }
        }

        return bindAddress.trim();
    }

    /**
     * Loads the {@link #getConfigurationFile() configuration file}. The file location will first be checked for
     * existence on the file system and then as a URL. If it cannot be found, it will be assumed the file location
     * specifies the file as found in the current class loader and the file will be searched there. An exception is
     * thrown if the file cannot be found anywhere.
     *
     * @return the configuration that was loaded
     *
     * @throws Exception if can't load
     */
    private static Preferences loadConfigurationFile() throws Exception {
        String fileName = getConfigurationFile();
        String preferencesNodeName = getTemplatePreferencesNodeName();
        InputStream configFileInputStream = null;

        // first see if the file was specified as a path on the local file system
        try {
            File configFile = new File(fileName);

            if (configFile.exists()) {
                configFileInputStream = new FileInputStream(configFile);
            }
        } catch (Exception e) {
            // isn't really an error - this just isn't a file on the local file system
        }

        // see if the file was specified as a URL
        if (configFileInputStream == null) {
            try {
                URL configFile = new URL(fileName);

                configFileInputStream = configFile.openStream();
            } catch (Exception e) {
                // isn't really an error - this just isn't a URL
            }
        }

        // if neither a file path or URL, assume the config file can be found in the classloader
        if (configFileInputStream == null) {
            configFileInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
        }

        if (configFileInputStream == null) {
            throw new Exception("Bad config file: " + fileName);
        }

        // We need to clear out any previous configuration in case the current config file doesn't specify a preference
        // that already exists in the preferences node. In this case, the configuration file wants to fall back on the
        // default value and if we don't clear the preferences, we aren't guaranteed the value stored in the backing
        // store is the default value.
        // But first we need to backup these original preferences in case the config file fails to load -
        // we'll restore the original values in that case.

        Preferences preferencesNode = getTemplatePreferencesNode();
        ByteArrayOutputStream backup = new ByteArrayOutputStream();
        preferencesNode.exportSubtree(backup);
        preferencesNode.clear();

        // now load in the preferences
        try {
            Preferences.importPreferences(configFileInputStream);

            if (preferencesNode.getInt(PREF_SCHEMA_VERSION, 0) == 0) {
                throw new IllegalArgumentException("Bad node name: " + preferencesNodeName);
            }
        } catch (Exception e) {
            // a problem occurred importing the config file; let's restore our original values
            try {
                Preferences.importPreferences(new ByteArrayInputStream(backup.toByteArray()));
            } catch (Exception e1) {
                // its conceivable the same problem occurred here as with the original exception (backing store
                // problem?) let's throw the original exception, not this one
            }

            throw e;
        }

        return preferencesNode;
    }

    private static void purge(File dir, boolean deleteIt) {
        if (dir != null) {
            if (dir.isDirectory()) {
                File[] doomedFiles = dir.listFiles();
                if (doomedFiles != null) {
                    for (File doomedFile : doomedFiles) {
                        purge(doomedFile, true); // call this method recursively
                    }
                }
            }

            if (deleteIt) {
                dir.delete();
            }
        }

        return;
    }

    /**
     * This will return URLs to all of the jars and resources that the spawned agents will have access to in their
     * isolated classloader. Only those classes within these jars will be accessible to the agent.
     *
     * <p>The returns array will <b>not</b> include any JNI jars - they should be in a separate classpath which will be
     * in an upper classloader.</p>
     *
     * @return URLs to embedded agent jars that will be in the embedded agent's classloader
     *
     * @throws Exception if failed to find the jars
     */
    private static URL[] getSpawnedAgentClasspath() throws Exception {
        File dist = new File(getAgentDistribution());
        File lib = new File(dist, "lib");

        if (!lib.isDirectory()) {
            throw new Exception("There is no lib directory under [" + dist + "]; cannot get agent jars");
        }

        File[] jarFiles = lib.listFiles();
        ArrayList<URL> classpathUrls = new ArrayList<URL>(jarFiles.length + 1);

        classpathUrls.add(dist.toURI().toURL()); // allows the agent to find resources in here, like log4j.xml

        for (File jarFile : jarFiles) {
            if (!jarFile.isDirectory()) {
                if (!isJNIJar(jarFile)) {
                    classpathUrls.add(jarFile.toURI().toURL());
                }
            }
        }

        return classpathUrls.toArray(new URL[classpathUrls.size()]);
    }

    /**
     * This will return URLs to all of the endorsed jars that the spawned agents will have access to in their
     * isolated classloader.
     *
     * <p>The returns array will <b>not</b> include any JNI jars - they should be in a separate classpath which will be
     * in an upper classloader.</p>
     *
     * @return URLs to embedded agent jars that will be in the embedded agent's classloader
     *
     * @throws Exception if failed to find the jars
     */
    private static URL[] getSpawnedAgentEndorsedClasspath() throws Exception {
        File dist = new File(getAgentDistribution());
        File lib = new File(dist, "lib/endorsed");

        if (!lib.isDirectory()) {
            throw new Exception("There is no lib/endorsed directory under [" + dist + "]; cannot get endorsed jars");
        }

        File[] jarFiles = lib.listFiles();
        ArrayList<URL> endorsedUrls = new ArrayList<URL>(jarFiles.length + 1);

        //classpathUrls.add(dist.toURI().toURL()); // allows the agent to find resources in here, like log4j.xml

        for (File jarFile : jarFiles) {
            if (!jarFile.isDirectory()) {
                if (!isJNIJar(jarFile)) {
                    endorsedUrls.add(jarFile.toURI().toURL());
                }
            }
        }

        return endorsedUrls.toArray(new URL[endorsedUrls.size()]);
    }

    /**
     * This will return URLs to all of the jars containing our JNI classes.
     *
     * @return URLs to jars with our JNI classes
     *
     * @throws Exception if failed to find the jars
     */
    private static URL[] getSpawnedAgentJNIClasspath() throws Exception {
        File dist = new File(getAgentDistribution());
        File lib = new File(dist, "lib");

        if (!lib.isDirectory()) {
            throw new Exception("There is no lib directory under [" + dist + "]; cannot get JNI jars");
        }

        File[] jarFiles = lib.listFiles();
        ArrayList<URL> classpathUrls = new ArrayList<URL>(jarFiles.length + 1);

        for (File jarFile : jarFiles) {
            if (isJNIJar(jarFile)) {
                classpathUrls.add(jarFile.toURI().toURL());
            }
        }

        return classpathUrls.toArray(new URL[classpathUrls.size()]);
    }

    private static boolean isJNIJar(File jarFile) {
        String name = jarFile.getName();
        if (name.startsWith("sigar") && name.endsWith(".jar")) {
            return true;
        }

        return false;
    }

    private static URL[] getSpawnedAgentNativeLibraryDirectories() throws Exception {
        String dist = getAgentDistribution();

        ArrayList<URL> nativeLibraryDirUrls = new ArrayList<URL>();

        // we will assume our native libraries are under the same lib directory where the jars are
        // we will also look for native libraries in subdirectories under lib
        File lib = new File(dist, "lib");

        if (!lib.isDirectory()) {
            throw new Exception("There is no lib directory under [" + dist + "]; cannot get native library directories");
        }

        addDirectories(lib, nativeLibraryDirUrls);

        return nativeLibraryDirUrls.toArray(new URL[nativeLibraryDirUrls.size()]);
    }

    private static void addDirectories(File dir, ArrayList<URL> list) throws Exception {
        if (dir.isDirectory()) {
            // add the given directory to the list and add all its subdirectories to the list
            list.add(dir.toURI().toURL());

            for (File dirEntry : dir.listFiles()) {
                addDirectories(dirEntry, list);
            }
        }

        return;
    }
}