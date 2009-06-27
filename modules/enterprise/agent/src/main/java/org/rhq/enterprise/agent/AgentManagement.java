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
package org.rhq.enterprise.agent;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.rhq.core.clientapi.server.core.CoreServerService;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.MD5Generator;
import org.rhq.enterprise.agent.AgentRestartCounter.AgentRestartReason;
import org.rhq.enterprise.communications.ServiceContainerMetricsMBean;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderMetrics;
import org.rhq.enterprise.communications.command.client.ClientRemotePojoFactory;

/**
 * This is the management layer for the agent. This is the MBean that is used to manage the agent itself. It emits the
 * agent's metric data.
 *
 * @author John Mazzitelli
 */
public class AgentManagement implements AgentManagementMBean, MBeanRegistration {
    /**
     * The agent being monitored.
     */
    private AgentMain m_agent;

    /**
     * Where this MBean is registered.
     */
    private MBeanServer m_mbs;

    /**
     * The name this MBean instance is registered under.
     */
    private ObjectName m_objectName;

    /**
     * Constructor for {@link AgentManagement}.
     *
     * @param agent the agent to be monitored
     */
    public AgentManagement(AgentMain agent) {
        m_agent = agent;
    }

    public void restart() {
        // restarting the agent is a suicidal act - this MBean instance will
        // be unregistered after we shutdown.  Therefore, we must do this in a
        // separate thread so as to allow this method to return successfully
        // first. Therefore, this method must inheritently do its thing asynchronously.
        new Thread(new Runnable() {
            public void run() {
                ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
                try {
                    sleep(5000L); // give our restart() caller a chance to return and finish
                    Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                    m_agent.shutdown();
                    m_agent.start();
                    m_agent.getAgentRestartCounter().restartedAgent(AgentRestartReason.OPERATION);
                } catch (Exception e) {
                    e.printStackTrace(); // TODO what do to here?
                } finally {
                    Thread.currentThread().setContextClassLoader(originalCL);
                }
            }
        }, "RHQ Agent Restart Thread").start();
    }

    public void shutdown() {
        // shutting down the agent is a suicidal act - this MBean instance will
        // be unregistered after we shutdown.  Therefore, we must do this in a
        // separate thread so as to allow this method to return successfully
        // first. Therefore, this method must inheritently do its thing asynchronously.
        new Thread(new Runnable() {
            public void run() {
                ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
                try {
                    sleep(5000L); // give our shutdown() caller a chance to return and finish
                    Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                    m_agent.shutdown();
                } finally {
                    Thread.currentThread().setContextClassLoader(originalCL);
                }
            }
        }, "RHQ Agent Shutdown Thread").start();
    }

    public void downloadLatestFailoverList() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            m_agent.performPrimaryServerSwitchoverCheck();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public void updatePlugins() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            m_agent.updatePlugins();
            restartPluginContainer();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public OperationResult retrieveAllPluginInfo() {
        List<File> plugins;

        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            PluginUpdate updater = getPluginUpdateObject();
            plugins = updater.getCurrentPluginFiles();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }

        OperationResult info = new OperationResult();
        PropertyList list = new PropertyList("plugins");
        info.getComplexResults().put(list);

        if (plugins.size() > 0) {
            for (File plugin : plugins) {
                PropertyMap map = new PropertyMap("plugin");
                map.put(new PropertySimple(PLUGIN_INFO_NAME, plugin.getName()));
                map.put(new PropertySimple(PLUGIN_INFO_PATH, plugin.getAbsoluteFile()));
                map.put(new PropertySimple(PLUGIN_INFO_TIMESTAMP, new Date(plugin.lastModified())));
                map.put(new PropertySimple(PLUGIN_INFO_SIZE, plugin.length()));

                try {
                    map.put(new PropertySimple(PLUGIN_INFO_MD5, MD5Generator.getDigestString(plugin)));
                } catch (IOException e) {
                    map.put(new PropertySimple(PLUGIN_INFO_MD5, e.toString()));
                }

                list.add(map);
            }
        }

        return info;
    }

    public OperationResult retrievePluginInfo(String pluginName) {
        List<File> plugins;

        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            PluginUpdate updater = getPluginUpdateObject();
            plugins = updater.getCurrentPluginFiles();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }

        if (plugins.size() > 0) {
            for (File plugin : plugins) {
                if (plugin.getName().equals(pluginName)) {
                    OperationResult opResults = new OperationResult();
                    Configuration info = opResults.getComplexResults();
                    info.put(new PropertySimple(PLUGIN_INFO_NAME, plugin.getName()));
                    info.put(new PropertySimple(PLUGIN_INFO_PATH, plugin.getAbsoluteFile()));
                    info.put(new PropertySimple(PLUGIN_INFO_TIMESTAMP, new Date(plugin.lastModified())));
                    info.put(new PropertySimple(PLUGIN_INFO_SIZE, plugin.length()));

                    try {
                        info.put(new PropertySimple(PLUGIN_INFO_MD5, MD5Generator.getDigestString(plugin)));
                    } catch (IOException e) {
                        info.put(new PropertySimple(PLUGIN_INFO_MD5, e.toString()));
                    }

                    return opResults;
                }
            }
        }

        throw new IllegalArgumentException("There is no plugin named [" + pluginName + "]");
    }

    public void restartPluginContainer() {
        new Thread(new Runnable() {
            public void run() {
                sleep(5000L); // give our caller a chance to return and finish
                ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                    m_agent.shutdownPluginContainer();
                    m_agent.startPluginContainer(500L);
                } finally {
                    Thread.currentThread().setContextClassLoader(originalCL);
                }
            }
        }, "RHQ Agent Plugin Container Restart Thread").start();
    }

    public OperationResult executeAvailabilityScan(Boolean changesOnly) {
        boolean changes = (changesOnly != null) ? changesOnly.booleanValue() : false;

        AvailabilityReport report;

        // ask for the report and tell the inventory manager to handle it.  We must hand it off to IM
        // because we need to send the report to the server - otherwise, the "real" availability executor
        // will not send changed resources thinking someone else did.
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
            report = inventoryManager.executeAvailabilityScanImmediately(changes);
            inventoryManager.handleReport(report);
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }

        OperationResult opResult = new OperationResult();
        Configuration complexResults = opResult.getComplexResults();

        PropertyList list = new PropertyList("resourceAvailabilities");
        complexResults.put(list);

        String agentName;
        Boolean changesOnlyFromReport;

        if (report != null) {
            agentName = report.getAgentName();
            changesOnlyFromReport = Boolean.valueOf(report.isChangesOnlyReport());

            List<Availability> avails = report.getResourceAvailability();

            if ((avails != null) && (avails.size() > 0)) {
                for (Availability avail : avails) {
                    Resource resource = avail.getResource();
                    boolean isUp = avail.getAvailabilityType() == AvailabilityType.UP;

                    PropertyMap map = new PropertyMap("resourceAvailability");
                    map.put(new PropertySimple("resourceId", Integer.valueOf(resource.getId())));
                    map.put(new PropertySimple("resourceName", resource.getName()));
                    map.put(new PropertySimple("isAvailable", Boolean.valueOf(isUp)));
                    list.add(map);
                }
            }
        } else {
            // report was null - this means there are no committed resources in inventory
            agentName = m_agent.getConfiguration().getAgentName();
            changesOnlyFromReport = changesOnly;
        }

        complexResults.put(new PropertySimple("agentName", agentName));
        complexResults.put(new PropertySimple("isChangesOnly", changesOnlyFromReport));

        return opResult;
    }

    public String getVersion() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            return Version.getProductVersion();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public String retrieveCurrentDateTime(String timeZone) {
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.FULL);

        if (timeZone == null || timeZone.length() == 0) {
            df.setTimeZone(TimeZone.getDefault());
        } else {
            df.setTimeZone(TimeZone.getTimeZone(timeZone));
        }

        return df.format(new Date());
    }

    public void setDebugMode(Boolean enabled, Boolean traceMessaging) throws ExecutionException {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            if (enabled != null && enabled.booleanValue()) {
                executePromptCommand("debug -f log4j-debug.xml");
                if (traceMessaging != null && traceMessaging.booleanValue()) {
                    executePromptCommand("debug -c true");
                } else {
                    executePromptCommand("debug -c false");
                }
            } else {
                executePromptCommand("debug -f log4j.xml");
                executePromptCommand("debug -c false");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
        return;
    }

    public String executePromptCommand(String command) throws ExecutionException {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            CharArrayWriter listener = new CharArrayWriter();
            AgentPrintWriter apw = m_agent.getOut();
            try {
                apw.addListener(listener);
                m_agent.executePromptCommand(command); // TODO should we do something if false is returned? (i.e. kill agent?)
            } catch (Exception e) {
                throw new ExecutionException(listener.toString(), e); // the message is the output, cause is the thrown exception
            } finally {
                apw.removeListener(listener);
            }

            String output = listener.toString();
            return output;
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public String getAgentHomeDirectory() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            return m_agent.getAgentHomeDirectory();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public int getNumberAgentRestarts() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            return m_agent.getAgentRestartCounter().getNumberOfRestarts();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public String getReasonForLastRestart() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            return m_agent.getAgentRestartCounter().getLastAgentRestartReason().toString();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public long getAgentServerClockDifference() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            return m_agent.getAgentServerClockDifference();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public long getUptime() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            long start_time = m_agent.getStartTime();

            if (start_time > 0) {
                return (System.currentTimeMillis() - start_time) / 1000L; // we want units in seconds
            }

            return 0L;
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public long getNumberSuccessfulCommandsReceived() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            return getServerSideMetrics().getNumberSuccessfulCommandsReceived();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public long getNumberFailedCommandsReceived() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            return getServerSideMetrics().getNumberFailedCommandsReceived();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public long getNumberTotalCommandsReceived() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            return getServerSideMetrics().getNumberTotalCommandsReceived();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public long getAverageExecutionTimeReceived() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            return getServerSideMetrics().getAverageExecutionTimeReceived();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public long getAverageExecutionTimeSent() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            return getClientSideMetrics().getAverageExecutionTimeSent();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public long getNumberSuccessfulCommandsSent() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            return getClientSideMetrics().getNumberSuccessfulCommandsSent();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public long getNumberFailedCommandsSent() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            return getClientSideMetrics().getNumberFailedCommandsSent();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public long getNumberTotalCommandsSent() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            ClientCommandSenderMetrics metrics = getClientSideMetrics();
            return metrics.getNumberSuccessfulCommandsSent() + metrics.getNumberFailedCommandsSent();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public long getNumberCommandsActiveSent() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            return getClientSideMetrics().getNumberCommandsActive();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public long getNumberCommandsInQueue() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            return getClientSideMetrics().getNumberCommandsInQueue();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public long getNumberCommandsSpooled() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            return getClientSideMetrics().getNumberCommandsSpooled();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public boolean isSending() {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            ClientCommandSenderMetrics metrics = getClientSideMetrics();
            return metrics.isSending();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    public long getJVMFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    public long getJVMTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    public int getJVMActiveThreads() {
        return ManagementFactory.getThreadMXBean().getThreadCount();
    }

    public Properties getAgentConfiguration() {
        Properties properties = new Properties();

        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            try {
                Preferences prefs = m_agent.getConfiguration().getPreferences();
                String[] keys = prefs.keys();
                for (String key : keys) {
                    properties.setProperty(key, prefs.get(key, "<error>"));
                }
            } catch (Exception e) {
                properties.setProperty("ERROR", e.getMessage()); // this should really never happen
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }

        return properties;
    }

    public void mergeIntoAgentConfiguration(Properties config) {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            if ((config != null) && (config.size() > 0)) {
                Preferences prefs = m_agent.getConfiguration().getPreferences();

                Set<Object> names = config.keySet();
                for (Object name : names) {
                    Object value = config.get(name);
                    prefs.put(name.toString(), value.toString()); // this persists the new config setting
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }

        return;
    }

    public void removeFromAgentConfiguration(List<String> preferenceNames) {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            if ((preferenceNames != null) && (preferenceNames.size() > 0)) {
                Preferences prefs = m_agent.getConfiguration().getPreferences();

                for (String doomedPreferenceName : preferenceNames) {
                    prefs.remove(doomedPreferenceName);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }

        return;
    }

    /**
     * Returns the MBeanServer where this MBean is registered; <code>null</code> if this MBean is not registered.
     *
     * @return the hosting MBeanServer
     */
    public MBeanServer getMBeanServer() {
        return m_mbs;
    }

    /**
     * This is the name that the MBean is registered under.
     *
     * @return mbean name
     */
    public ObjectName getObjectName() {
        return m_objectName;
    }

    /**
     * @see javax.management.MBeanRegistration#preRegister(MBeanServer, ObjectName)
     */
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        singletonObjectName = name; // see the comments on this var's declaration statement for why this is needed

        m_mbs = server;
        m_objectName = name;
        return name;
    }

    /**
     * @see javax.management.MBeanRegistration#postRegister(Boolean)
     */
    public void postRegister(Boolean registrationDone) {
        // NO-OP
    }

    /**
     * @see javax.management.MBeanRegistration#postDeregister()
     */
    public void postDeregister() {
        m_mbs = null;
        m_objectName = null;
    }

    /**
     * @see javax.management.MBeanRegistration#preDeregister()
     */
    public void preDeregister() throws Exception {
        // NO-OP
    }

    /**
     * Returns the client-side metrics from the agent's sender.
     *
     * @return client side metrics
     */
    private ClientCommandSenderMetrics getClientSideMetrics() {
        ClientCommandSender sender = m_agent.getClientCommandSender();
        ClientCommandSenderMetrics metrics;

        if (sender != null) {
            metrics = sender.getMetrics();
        } else {
            metrics = new ClientCommandSenderMetrics(); // simulate an empty and idle sender (all metrics are zeroed out)
        }

        return metrics;
    }

    /**
     * Obtains a proxy the MBean that emits the server-side (i.e. incoming commands) metrics.
     *
     * @return server side metric mbean proxy
     *
     * @throws IllegalStateException if for some reason the MBean is not available or its proxy could not be created
     */
    private ServiceContainerMetricsMBean getServerSideMetrics() {
        try {
            MBeanServer mbs = m_agent.getServiceContainer().getMBeanServer();
            Object mbean = MBeanServerInvocationHandler.newProxyInstance(mbs,
                ServiceContainerMetricsMBean.OBJECTNAME_METRICS, ServiceContainerMetricsMBean.class, false);
            return (ServiceContainerMetricsMBean) mbean;
        } catch (Exception e) {
            throw new IllegalStateException(e); // should never happen
        }
    }

    /**
     * Builds a {@link PluginUpdate} object that can be used to update the plugins or get information about the plugins.
     *
     * @return plugin updater
     */
    private PluginUpdate getPluginUpdateObject() {
        ClientCommandSender sender = m_agent.getClientCommandSender();
        CoreServerService server = null;

        if (sender != null) {
            ClientRemotePojoFactory factory = sender.getClientRemotePojoFactory();
            server = factory.getRemotePojo(CoreServerService.class);
        }

        PluginContainerConfiguration pc_config = m_agent.getConfiguration().getPluginContainerConfiguration();
        PluginUpdate plugin_update = new PluginUpdate(server, pc_config);

        return plugin_update;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    // this is here to support the agent plugin - the agent plugin will
    // get the singleton object name to determine which MBean it needs to
    // use.  This was needed because under testing, we like to embed multiple
    // agents in the same VM, but in different classloaders.  This enables
    // the agent plugin to be able to run in both the "normal" mode
    // and in the perf testing mode. We set it to a default name just because
    // we want it a non-null (avoid possible NPEs later) but the default
    // name is one that never exists (its missing the name key property).
    public static ObjectName singletonObjectName = AgentManagementMBean.BASE_OBJECT_NAME;
}