package org.rhq.embeddedagent.extension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.Preferences;

import org.apache.log4j.LogManager;
import org.apache.log4j.xml.DOMConfigurator;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.logging.Logger;
import org.jboss.modules.Resource;
import org.jboss.util.StringPropertyReplacer;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.agent.AgentConfiguration;
import org.rhq.enterprise.agent.AgentConfigurationConstants;
import org.rhq.enterprise.agent.AgentConfigurationUpgrade;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;

public class AgentConfigurationSetup {

    private final Logger log = Logger.getLogger(AgentConfigurationSetup.class);

    private static final String DATA_DIRECTORY_NAME = "embeddedagent";

    /**
     * The location of the configuration file - can be a file path or path within classloader.
     */
    private final Resource configFile;

    /**
     * Properties that will be used to override preferences found in the preferences node and the configuration
     * preferences file.
     */
    private final Properties configurationOverrides;

    /**
     * If <code>true</code>, will revert the agent's configuration back to the original configuration file.
     * Otherwise, the configuration will be that which is currently persisted in the preferences store.
     */
    private final boolean resetConfigurationAtStartup;

    /**
     * The preferences node name that identifies the configuration set used to configure the services.
     */
    private final String preferencesNodeName;

    /**
     * Provides environment information about the server in which we are embedded.
     */
    private final ServerEnvironment serverEnvironment;

    public AgentConfigurationSetup(Resource configFile, boolean resetConfigurationAtStartup,
        Properties configurationOverrides, ServerEnvironment serverEnv) {

        this.configFile = configFile;
        this.resetConfigurationAtStartup = resetConfigurationAtStartup;
        this.serverEnvironment = serverEnv;
        this.configurationOverrides = prepareConfigurationOverrides(configurationOverrides);

        String agentName = configurationOverrides.getProperty(AgentConfigurationConstants.NAME, "embeddedagent");
        preferencesNodeName = agentName;
        System.setProperty("rhq.agent.preferences-node", preferencesNodeName);
    }

    public String getPreferencesNodeName() {
        return this.preferencesNodeName;
    }

    private Properties prepareConfigurationOverrides(Properties overrides) {
        // perform some checking to setup defaults if need be
        String agentName = overrides.getProperty(AgentConfigurationConstants.NAME, "");
        if (agentName.trim().length() == 0 || "-".equals(agentName)) {
            agentName = "embeddedagent-" + serverEnvironment.getNodeName();
        }

        agentName = StringPropertyReplacer.replaceProperties(agentName);
        overrides.put(AgentConfigurationConstants.NAME, agentName);

        File dataDir = getAgentDataDirectory();
        File pluginsDir = new File(serverEnvironment.getServerDataDir(), "embeddedagent-plugins");
        overrides.put(AgentConfigurationConstants.DATA_DIRECTORY, dataDir.getAbsolutePath());
        overrides.put(AgentConfigurationConstants.PLUGINS_DIRECTORY, pluginsDir.getAbsolutePath());
        overrides.put(ServiceContainerConfigurationConstants.DATA_DIRECTORY, dataDir.getAbsolutePath());

        return overrides;
    }

    private File getAgentDataDirectory() {
        File dir = new File(serverEnvironment.getServerDataDir(), DATA_DIRECTORY_NAME);
        dir.mkdirs();
        return dir;
    }

    public void preConfigureAgent() throws Exception {

        // we need to store the preferences prior to starting the agent
        if (resetConfigurationAtStartup) {
            log.debug("Resetting the embedded agent's configuration back to its original settings");
            reloadAgentConfiguration();
            cleanDataDirectory();
        } else {
            log.debug("Loading the embedded agent's pre-existing configuration from preferences");
            prepareConfigurationPreferences();
        }

        return;
    }

    /**
     * Prepares the log config file so it writes the logs to the server's log directory.
     * This is needed if we call or use any agent class because it wants to use log4j.
     * This MUST be called prior to using any class that logs via log4j.
     *
     * @param logConfigFile the agent's out-of-box log config file
     * @return the new log config file that the agent should use
     * @throws Exception
     */
    public void prepareLogConfigFile(Resource logConfigFile) throws Exception {
        try {
            File logDir = this.serverEnvironment.getServerLogDir();
            String agentLogFile = new File(logDir, "embedded-agent.log").getAbsolutePath();
            String cmdTraceLogFile = new File(logDir, "embedded-agent-command-trace.log").getAbsolutePath();

            String logConfig = new String(StreamUtil.slurp(logConfigFile.openStream()));
            logConfig = logConfig.replace("\"logs/agent.log\"", "\"" + agentLogFile + "\"");
            logConfig = logConfig.replace("\"logs/command-trace.log\"", "\"" + cmdTraceLogFile + "\"");
            for (String app : new String[] { "ref=\"FILE\"", "ref=\"COMMANDTRACE\"" }) {
                logConfig = logConfig.replace("<!-- <appender-ref " + app + "/> -->", "<appender-ref " + app + "/>");
            }

            File runtimeLogConfigFile = new File(getAgentDataDirectory(), "/log4j.xml");
            ByteArrayInputStream in = new ByteArrayInputStream(logConfig.getBytes());
            StreamUtil.copy(in, new FileOutputStream(runtimeLogConfigFile));

            // this hot deploys the log4j.xml into log4j which is what the agent wants to use
            LogManager.resetConfiguration();
            DOMConfigurator.configure(runtimeLogConfigFile.toURI().toURL());
        } catch (Exception e) {
            log.error("Cannot tell the agent to put its logs in the logs directory - look elsewhere for the log files");
        }
    }

    private Properties getAgentConfigurationProperties() {
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

    private void reloadAgentConfiguration() throws Exception {
        getPreferencesNode().clear();
        prepareConfigurationPreferences();
    }

    private void cleanDataDirectory() {
        AgentConfiguration config = new AgentConfiguration(getPreferencesNode());
        File dataDir = config.getDataDirectory();

        cleanDataFile(dataDir);

        // it is conceivable the comm services data directory was configured in a different
        // place than where the agent's data directory is - make sure we clean out that other data dir
        File commDataDir = config.getServiceContainerPreferences().getDataDirectory();
        if (!commDataDir.getAbsolutePath().equals(dataDir.getAbsolutePath())) {
            cleanDataFile(commDataDir);
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
        Preferences prefNode = getPreferencesNode();
        AgentConfiguration config = new AgentConfiguration(prefNode);

        if (config.getAgentConfigurationVersion() == 0) {
            config = loadConfigurationFile();
        }

        // now that the configuration preferences are loaded, we need to override them with any bootstrap override properties
        Properties overrides = configurationOverrides;
        if (overrides != null) {
            for (Map.Entry<Object, Object> entry : overrides.entrySet()) {
                String key = entry.getKey().toString();
                String value = entry.getValue().toString();

                // allow ${var} notation in the values so we can provide variable replacements in the values
                value = StringPropertyReplacer.replaceProperties(value);

                prefNode.put(key, value);
            }
        }

        // let's make sure our configuration is upgraded to the latest schema
        AgentConfigurationUpgrade.upgradeToLatest(config.getPreferences());

        return config;
    }

    /**
     * Loads the configuration file.
     *
     * @return the configuration that was loaded
     *
     * @throws Exception on failure
     */
    private AgentConfiguration loadConfigurationFile() throws Exception {
        // We need to clear out any previous configuration in case the current config file doesn't specify a preference
        // that already exists in the preferences node.  In this case, the configuration file wants to fall back on the
        // default value and if we don't clear the preferences, we aren't guaranteed the value stored in the backing
        // store is the default value.
        // But first we need to backup these original preferences in case the config file fails to load -
        // we'll restore the original values in that case.

        Preferences prefNode = getPreferencesNode();
        ByteArrayOutputStream backup = new ByteArrayOutputStream();
        prefNode.exportSubtree(backup);
        prefNode.clear();

        // now load in the preferences
        try {
            ByteArrayOutputStream rawConfigFile = new ByteArrayOutputStream();
            InputStream rawConfigInputStream = configFile.openStream();
            StreamUtil.copy(rawConfigInputStream, rawConfigFile, true);
            String newConfig = StringPropertyReplacer.replaceProperties(rawConfigFile.toString());
            ByteArrayInputStream newConfigInputStream = new ByteArrayInputStream(newConfig.getBytes());
            Preferences.importPreferences(newConfigInputStream);

            if (new AgentConfiguration(prefNode).getAgentConfigurationVersion() == 0) {
                throw new IllegalArgumentException("Bad preferences node");
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

        AgentConfiguration agentConfig = new AgentConfiguration(prefNode);
        return agentConfig;
    }

    /**
     * Returns the preferences for this agent. The node returned is where all preferences are to be stored.
     *
     * @return the agent preferences
     */
    private Preferences getPreferencesNode() {
        Preferences topNode = Preferences.userRoot().node(AgentConfigurationConstants.PREFERENCE_NODE_PARENT);
        Preferences prefNode = topNode.node(preferencesNodeName);
        return prefNode;
    }

    /**
     * This will delete the given file and if its a directory, will recursively delete its contents and its
     * subdirectories.
     *
     * @param file the file/directory to delete
     */
    private void cleanDataFile(File file) {
        boolean deleted;

        File[] doomedFiles = file.listFiles();
        if (doomedFiles != null) {
            for (File doomedFile : doomedFiles) {
                cleanDataFile(doomedFile); // call this method recursively
            }
        }

        deleted = file.delete();

        if (!deleted) {
            log.warn("Cannot clean data file [" + file + "]");
        }

        return;
    }
}