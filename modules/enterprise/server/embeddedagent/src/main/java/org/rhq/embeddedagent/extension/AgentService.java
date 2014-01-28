package org.rhq.embeddedagent.extension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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

public class AgentService implements Service<AgentService> {

    public static final ServiceName SERVICE_NAME = ServiceName.of("org.rhq").append(
        AgentSubsystemExtension.SUBSYSTEM_NAME);

    private final Logger log = Logger.getLogger(AgentService.class);

    /**
     * Our subsystem add-step handler will inject this as a dependency for us.
     * This service gives us information about the server, like the install directory, data directory, etc.
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
     * Provides the status flag of the embedded agent itself (not of this service).
     */
    private AtomicBoolean agentStarted = new AtomicBoolean(false);

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

    protected boolean isAgentStarted() {
        return agentStarted.get();
    }

    protected void startAgent() {
        log.info("Starting the embedded agent now");
        agentStarted.set(true);
    }

    protected void stopAgent() {
        log.info("Stopping the embedded agent now");
        agentStarted.set(false);
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
