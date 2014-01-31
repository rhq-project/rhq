package org.rhq.embeddedagent.extension;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.Resource;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import org.rhq.enterprise.agent.AgentMain;

public class AgentService implements Service<AgentService> {

    public static final ServiceName SERVICE_NAME = ServiceName.of("org.rhq").append(
        AgentSubsystemExtension.SUBSYSTEM_NAME);

    private final Logger log = Logger.getLogger(AgentService.class);

    /**
     * Our subsystem add-step handler will inject this as a dependency for us.
     * This service gives us information about the server, like the install directory, data directory, etc.
     * Package-scoped so the add-step handler can access this.
     */
    InjectedValue<ServerEnvironment> envServiceValue = new InjectedValue<ServerEnvironment>();

    /**
     * This service can be configured to be told explicitly about certain plugins to be
     * enabled or disabled. This map holds that configuration. These aren't necessarily
     * all the plugins that will be loaded, but they are those plugins this service was
     * explicitly told about and indicates if they should be enabled or disabled.
     * TODO: For any plugin not specified will, by default, be WHAT? Disabled???
     */
    private Map<String, Boolean> plugins = Collections.synchronizedMap(new HashMap<String, Boolean>());

    /**
     * Configuration settings that override the out-of-box configuration file. These are settings
     * that the user set in the subsystem (e.g. standalone.xml or via AS CLI).
     */
    private Map<String, String> configOverrides = Collections.synchronizedMap(new HashMap<String, String>());

    /**
     * This is the actual embedded agent. This is what handles the plugin container lifecycle
     * and communication to/from the server.
     */
    private AtomicReference<AgentMain> theAgent = new AtomicReference<AgentMain>();

    /**
     * This is the daemon thread running the agent.
     */
    private Thread agentThread;

    public AgentService() {
    }

    @Override
    public AgentService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        log.info("Embedded agent service starting");
        startAgent();
    }

    @Override
    public void stop(StopContext context) {
        log.info("Embedded agent service stopping");
        stopAgent();
    }

    /**
     * Returns the set of plugins the service knows about and whether
     * or not those plugins are to be enabled or disabled.
     * You get back a copy, not the actual map.
     *
     * @return plugins and their enable-flag
     */
    protected Map<String, Boolean> getPlugins() {
        synchronized (plugins) {
            return new HashMap<String, Boolean>(plugins);
        }
    }

    /**
     * Sets the enable flags for plugins.
     *
     * @return plugins and their enable-flag (if <code>null</code>, assumes an empty map)
     */
    protected void setPlugins(Map<String, Boolean> pluginsWithEnableFlag) {
        synchronized (plugins) {
            plugins.clear();
            if (pluginsWithEnableFlag != null) {
                plugins.putAll(pluginsWithEnableFlag);
            }
        }

        log.info("New plugin definitions: " + pluginsWithEnableFlag);
    }

    protected void setConfigurationOverrides(Map<String, String> overrides) {
        synchronized (configOverrides) {
            configOverrides.clear();
            if (overrides != null) {
                configOverrides.putAll(overrides);
            }
        }
    }

    protected boolean isAgentStarted() {
        AgentMain agent = theAgent.get();
        return (agent != null && agent.isStarted());
    }

    protected void startAgent()  throws StartException {
        if (isAgentStarted()) {
            log.info("Embedded agent is already started.");
            return;
        }

        log.info("Starting the embedded agent now");
        try {
            // make sure we pre-configure the agent with some settings taken from our runtime environment
            ServerEnvironment env = envServiceValue.getValue();
            boolean resetConfigurationAtStartup = true;
            AgentConfigurationSetup configSetup = new AgentConfigurationSetup(
                getExportedResource("conf/agent-configuration.xml"), resetConfigurationAtStartup, configOverrides, env);
            // prepare the agent logging first thing so the agent logs messages using this config
            configSetup.prepareLogConfigFile(getExportedResource("conf/log4j.xml"));
            configSetup.preConfigureAgent();

            // build the startup command line arguments to pass to the agent
            String[] args = new String[3];
            args[0] = "--daemon";
            args[1] = "--pref=" + configSetup.getPreferencesNodeName();
            args[2] = "--output=" + new File(env.getServerLogDir(), "embedded-agent.out").getAbsolutePath();

            theAgent.set(new AgentMain(args));

            agentThread = new Thread("Embedded Agent Start Thread") {
                public void run() {
                    try {
                        theAgent.get().start();
                    } catch (InterruptedException e) {
                        // agent just exited due to being shutdown, die quietly
                        log.debug("Embedded agent has exited.");
                    } catch (Throwable t) {
                        log.error("Embedded agent aborted with exception.", t);
                    }
                };
            };
            agentThread.setDaemon(true);
            agentThread.start();
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    protected void stopAgent() {
        try {
            if (!isAgentStarted()) {
                log.info("Embedded agent is already stopped.");
            } else {
                log.info("Stopping the embedded agent now");
                theAgent.get().shutdown();
            }
        } finally {
            if (agentThread != null) {
                agentThread.interrupt();
            }
        }
        theAgent.set(null);
    }

    /**
     * Gets information about a file that is inside our module. Use this to
     * obtain files locationed in the embedded agent, for example, pass in
     * "conf/agent-configuration.xml" to get the config file.
     *
     * @param name name of the agent file
     * @return object referencing the file from our module
     */
    private Resource getExportedResource(String name) {
        Module module = Module.forClass(getClass());
        Resource r = module.getExportedResource("rhq-agent", name);
        return r;
    }
}
