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
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
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

import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.server.core.CoreServerService;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.MessageDigestGenerator;
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

    public void switchToServer(String server) {
        m_agent.switchToServer(server);
    }

    public void restart() {
        // restarting the agent is a suicidal act - this MBean instance will
        // be unregistered after we shutdown.  Therefore, we must do this in a
        // separate thread so as to allow this method to return successfully
        // first. Therefore, this method must inherently do its thing asynchronously.

        // Another important fact is that if this method is called from the rhq-agent plugin
        // which is co-located in the same JVM, restarting the plugin container actually creates
        // a whole bunch of new classloaders that ALL inherit the access control context of the
        // agent plugin executing thread and classloader. The access control context contains
        // a reference to the agent plugin's classloader through its protection domain and thus,
        // by using the rhq-agent's restart, shutdown or restartPluginContainer operations we
        // create a classloader leak.
        //
        // The old rhq-agent's plugin classloader will never be released and will carry along with
        // it all the classes, slowly contributing to the eventual permgen depletion and OOMEs.
        //
        // To solve this problem, we run the restart, shutdown and restartPluginContainer methods with
        // the access control context of this class. This class is defined in the agent itself and thus
        // its access control context doesn't contain the "baggage" from the plugin classloaders.

        new Thread(new Runnable() {
            public void run() {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
                        try {
                            Thread.sleep(5000L); // give our restart() caller a chance to return and finish
                            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                            m_agent.shutdown();
                            m_agent.start();
                            m_agent.getAgentRestartCounter().restartedAgent(AgentRestartReason.OPERATION);
                        } catch (Exception e) {
                            e.printStackTrace(); // TODO what do to here?
                        } finally {
                            Thread.currentThread().setContextClassLoader(originalCL);
                        }

                        return null;
                    }
                });
            }
        }, "RHQ Agent Restart Thread").start();
    }

    public void shutdown() {
        // shutting down the agent is a suicidal act - this MBean instance will
        // be unregistered after we shutdown.  Therefore, we must do this in a
        // separate thread so as to allow this method to return successfully
        // first. Therefore, this method must inherently do its thing asynchronously.

        // see the explanation in the restart() method for why we're running this as
        // a privileged action

        new Thread(new Runnable() {
            public void run() {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
                        try {
                            Thread.sleep(5000L); // give our shutdown() caller a chance to return and finish
                            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                            m_agent.shutdown();
                        } catch (InterruptedException e) {
                            // exit the thread
                        } finally {
                            Thread.currentThread().setContextClassLoader(originalCL);
                        }

                        return null;
                    }
                });
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
        OperationResult info = new OperationResult();
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            PluginUpdate updater = getPluginUpdateObject();
            plugins = updater.getCurrentPluginFiles();
            PluginContainerConfiguration pcConfig = m_agent.getConfiguration().getPluginContainerConfiguration();
            List<String> enabledPlugins = pcConfig.getEnabledPlugins();
            List<String> disabledPlugins = pcConfig.getDisabledPlugins();
            
            PropertyList list = new PropertyList("plugins".intern());
            info.getComplexResults().put(list);

            if (plugins.size() > 0) {
                for (File plugin : plugins) {
                    String pluginName;
                    String pluginDisplayName;
                    try {
                        URL url = plugin.toURI().toURL();
                        PluginDescriptor descriptor = AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(url);
                        pluginName = descriptor.getName();
                        pluginDisplayName = descriptor.getDisplayName();
                    } catch (Exception t) {
                        pluginName = "?cannot-parse-descriptor?".intern();
                        pluginDisplayName = "?cannot-parse-descriptor?".intern();
                    }

                    PropertyMap map = new PropertyMap("plugin".intern());
                    map.put(new PropertySimple(PLUGIN_INFO_NAME, pluginName));
                    map.put(new PropertySimple(PLUGIN_INFO_DISPLAY_NAME, pluginDisplayName));
                    map.put(new PropertySimple(PLUGIN_INFO_PATH, plugin.getAbsoluteFile()));
                    map.put(new PropertySimple(PLUGIN_INFO_TIMESTAMP, new Date(plugin.lastModified())));
                    map.put(new PropertySimple(PLUGIN_INFO_SIZE, plugin.length()));
                    // plugin is either whitelisted or the white list is empty
                    boolean isEnabled = enabledPlugins.isEmpty() || enabledPlugins.contains(pluginName);
                    // ..and is not on the black list
                    isEnabled &= !disabledPlugins.contains(pluginName);
                    map.put(new PropertySimple(PLUGIN_INFO_ENABLED, isEnabled));

                    try {
                        map.put(new PropertySimple(PLUGIN_INFO_MD5, MessageDigestGenerator.getDigestString(plugin)));
                    } catch (IOException e) {
                        map.put(new PropertySimple(PLUGIN_INFO_MD5, e.toString()));
                    }

                    list.add(map);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
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

            if (plugins.size() > 0) {
                for (File plugin : plugins) {
                    String pluginDisplayName;
                    String pluginNameToReturn;
                    try {
                        URL url = plugin.toURI().toURL();
                        PluginDescriptor pluginDescriptor = AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(url);
                        pluginDisplayName = pluginDescriptor.getDisplayName();
                        pluginNameToReturn = pluginDescriptor.getName();
                    } catch (Exception t) {
                        continue;
                    }

                    if (pluginNameToReturn.toLowerCase().equals(pluginName.toLowerCase())) {
                        OperationResult opResults = new OperationResult();
                        Configuration info = opResults.getComplexResults();
                        info.put(new PropertySimple(PLUGIN_INFO_NAME, pluginNameToReturn));
                        info.put(new PropertySimple(PLUGIN_INFO_DISPLAY_NAME, pluginDisplayName));
                        info.put(new PropertySimple(PLUGIN_INFO_PATH, plugin.getAbsoluteFile()));
                        info.put(new PropertySimple(PLUGIN_INFO_TIMESTAMP, new Date(plugin.lastModified())));
                        info.put(new PropertySimple(PLUGIN_INFO_SIZE, plugin.length()));
                        try {
                            info.put(new PropertySimple(PLUGIN_INFO_MD5, MessageDigestGenerator.getDigestString(plugin)));
                        } catch (IOException e) {
                            info.put(new PropertySimple(PLUGIN_INFO_MD5, e.toString()));
                        }

                        return opResults;
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
        throw new IllegalArgumentException("There is no plugin named [" + pluginName + "]");
    }

    public void restartPluginContainer() {
        // see the explanation in the restart() method for why we're running this as
        // a privileged action

        new Thread(new Runnable() {
            public void run() {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
                        try {
                            Thread.sleep(5000L); // give our caller a chance to return and finish
                            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                            m_agent.shutdownPluginContainer();
                            m_agent.startPluginContainer(500L);
                        } catch (InterruptedException e) {
                            // exit the thread
                        } finally {
                            Thread.currentThread().setContextClassLoader(originalCL);
                        }

                        return null;
                    }
                });
            }
        }, "RHQ Agent Plugin Container Restart Thread").start();
    }

    public OperationResult executeAvailabilityScan(Boolean changesOnly) {
        boolean changes = (changesOnly != null) ? changesOnly.booleanValue() : false;

        AvailabilityReport report;
        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();

        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            report = inventoryManager.executeAvailabilityScanImmediately(changes);
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

            List<AvailabilityReport.Datum> avails = report.getResourceAvailability();

            if (avails.size() > 0) {
                for (AvailabilityReport.Datum avail : avails) {
                    boolean isUp = avail.getAvailabilityType() == AvailabilityType.UP;

                    // lookup the heavy-weight resource object
                    int resourceId = avail.getResourceId();
                    ResourceContainer resourceContainer = inventoryManager.getResourceContainer(resourceId);
                    Resource resource = resourceContainer.getResource();

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

    public String executePromptCommand(final String command) throws ExecutionException {
        // we don't know what command will get executed, so let's proactively run it in the privileged action
        // for why it is a good idea, see the comments in the restart() method

        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws Exception {
                    ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                        CharArrayWriter listener = new CharArrayWriter();
                        AgentPrintWriter apw = m_agent.getOut();
                        try {
                            apw.addListener(listener);
                            m_agent.executePromptCommand(
                                command); // TODO should we do something if false is returned? (i.e. kill agent?)
                        } catch (Exception e) {
                            throw new ExecutionException(listener.toString(),
                                e); // the message is the output, cause is the thrown exception
                        } finally {
                            apw.removeListener(listener);
                        }

                        String output = listener.toString();
                        return output;
                    } finally {
                        Thread.currentThread().setContextClassLoader(originalCL);
                    }
                }
            });
        } catch (PrivilegedActionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ExecutionException) {
                throw (ExecutionException) cause;
            } else {
                throw new ExecutionException(e);
            }
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
