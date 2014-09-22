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

import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import javax.management.ObjectName;

import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.ObjectNameFactory;
import org.rhq.enterprise.communications.ServiceContainerMetricsMBean;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderMetrics;

/**
 * The agent's management interface. This is the interface used by the agent plugin to manage and monitor the agent
 * itself. This
 *
 * @author John Mazzitelli
 */
public interface AgentManagementMBean {
    /**
     * The domain name where this MBean will be placed. This is also the default domain name of the MBeanServer where
     * this MBean is registered.
     */
    String JMX_DOMAIN = "rhq.agent";

    /**
     * All agent management MBeans will have this as a key property whose property value will be the agent name.
     */
    String KEY_NAME = "name";

    /**
     * This is the object name that the agent will register as. Its just a base name - an additional key property
     * {@link #KEY_NAME} will be added to make it unique in the case when you embedded agents in the same VM.
     */
    ObjectName BASE_OBJECT_NAME = ObjectNameFactory.create(JMX_DOMAIN + ":type=agent");

    /**
     * Identifies the name of a plugin.
     */
    String PLUGIN_INFO_NAME = "name";

    /**
     * Identifies the display name of a plugin.
     */
    String PLUGIN_INFO_DISPLAY_NAME = "displayName";

    /**
     * Identifies the full path to a plugin.
     */
    String PLUGIN_INFO_PATH = "path";

    /**
     * Identifies the last modified date of a plugin.
     */
    String PLUGIN_INFO_TIMESTAMP = "timestamp";

    /**
     * Identifies the filesize of a plugin.
     */
    String PLUGIN_INFO_SIZE = "size";

    /**
     * Identifies whether the plugin is currently being used by the agent
     */
    String PLUGIN_INFO_ENABLED = "enabled";

    /**
     * Identifies the MD5 of a plugin.
     */
    String PLUGIN_INFO_MD5 = "md5";

    /**
     * Requests that the agent begin to update itself.
     */
    void updateAgent();

    /**
     * Switch the agent to talk to the given server.
     *
     * @param server server the agent should talk to
     */
    void switchToServer(String server);

    /**
     * This will perform an agent <i>hot-restart</i>. The agent will be {@link #shutdown()} and then immediately started
     * again. This is usually called after a client has
     * {@link #mergeIntoAgentConfiguration(Properties) changed some configuration settings}.
     *
     * <p>The actual restart is performed asynchronously. The caller has just a few seconds after this method returns
     * before it takes effect.</p>
     */
    void restart();

    /**
     * This will shutdown the agent's communications layer and the plugin container. If the agent is in daemon mode, the
     * agent's VM will die. Once this method is called, this management interface will no longer be available via JMX
     * and the agent will no longer be able to process incoming commands or send outgoing commands.
     *
     * <p>The actual shutdown is performed asynchronously. The caller has just a few seconds after this method returns
     * before it takes effect.</p>
     */
    void shutdown();

    /**
     * Tells the agent to download an updated server failover list. This will also check to make sure
     * the agent is pointing to its primary server as found in the new failover list and, if not, will
     * attempt to switch to the primary server now.
     */
    void downloadLatestFailoverList();

    /**
     * This will tell the agent to update its plugins. If the JON Server is up and the agent has detected it, this will
     * immediately pull down the updated plugins. If the JON Server is down, this will schedule the agent to pull down
     * the plugins as soon as the JON Server comes back up.
     *
     * <p>After the plugins are updated, the plugin container will immediately be
     * {@link #restartPluginContainer() restarted}.</p>
     *
     * <p>The actual PC restart is performed asynchronously. The caller has just a few seconds after this method returns
     * before it takes effect.</p>
     */
    void updatePlugins();

    /**
     * Returns information on all currently deployed plugins. The configuration will contain one {@link PropertyMap} for
     * each plugin. The name of the map will be the plugin name. Each map will have the key/value pairs where the keys
     * are PLUGIN_INFO_xxx.
     *
     * @return information on all deployed plugins
     */
    OperationResult retrieveAllPluginInfo();

    /**
     * Returns information on the given plugin. The configuration will contain {@link PropertySimple simple properties}
     * where the names are defined by PLUGIN_INFO_xxx.
     *
     * @param  pluginName the plugin whose information is to be returned
     *
     * @return the plugin information
     */
    OperationResult retrievePluginInfo(String pluginName);

    /**
     * This will shutdown then immediately restart the agent's internal plugin container. The plugin container manages
     * all plugins and their lifecycles. This is usually called after a client has
     * {@link #updatePlugins() updated the plugins}. Restarting the plugin container forces it to load in newly updated
     * plugins.
     *
     * <p>The actual restart is performed asynchronously. The caller has just a few seconds after this method returns
     * before it takes effect.</p>
     *
     * @see #updatePlugins()
     */
    void restartPluginContainer();

    /**
     * Asks the agent's plugin container to execute an availability scan and returns the results. See
     * {@link DiscoveryAgentService#executeAvailabilityScanImmediately(boolean)} for the semantics of this call.
     *
     * @param  changesOnly if <code>true</code>, only report those availabilities that have changed
     *
     * @return the report in an {@link OperationResult} object
     */
    public OperationResult executeAvailabilityScan(Boolean changesOnly);

    /**
     * Returns the agent's version string. This does not necessarily help identify the versions of the plugins, since
     * each plugin may have been updated from the JON Server since the initial installation of the agent.
     *
     * @return identifies the version of the agent.
     */
    String getVersion();

    /**
     * Returns the current time, as it is known to the agent. This can be used to determine if the agent's clock is
     * skewed from some other clock (e.g. the JON Server). The returned value is the number of milliseconds since
     * midnight, January 1, 1970 UTC (as per <code>System.currentTimeMillis</code>.
     *
     * @return current time, as it is known to the agent
     */
    long getCurrentTime();

    /**
     * Returns a string of the agent's current date/time, formatted with the given time zone.
     * If the given time zone is null or empty string, the agent's local time zone will be used.
     * If the given time zone is unknown, GMT will be the default.
     * <code>timeZone</code>can be either an abbreviation such as "PST", a full name such as
     * "America/Los_Angeles", or a custom ID such as "GMT-8:00". Note that the support of abbreviations is
     * for JDK 1.1.x compatibility only and full names should be used.
     *
     * @param timeZone the time zone to display the date/time in
     *
     * @return the agent's current date/time
     *
     * @see TimeZone#getTimeZone(String)
     */
    String retrieveCurrentDateTime(String timeZone);

    /**
     * Turns on or off debug mode, which makes the agent log more verbose with debug messages.
     * This will also be able to optionally turn on and off messaging trace, which
     * lets you debug the messaging between server and agent.
     * If the <code>enabled</code> flag is false, <code>traceMessaging</code> is ignored (i.e. all debug
     * will be disabled, including message tracing).
     *
     * @param enabled enable debug mode
     * @param traceMessaging if <code>true</code>, message tracing will be enabled unless <code>enabled</code> is <code>false</code>
     *
     * @throws ExecutionException if failed to change the debug mode
     */
    void setDebugMode(Boolean enabled, Boolean traceMessaging) throws ExecutionException;

    /**
     * Executes an agent prompt command.  The given <code>command</code> is the prompt command
     * plus any additional command arguments, separated by spaces (just as if you typed the command
     * in the console window). The results will be a string that contains the text that you would
     * have seen in the console output had the prompt command been executed from the console.
     *
     * @param command the command to execute along with any command line arguments.
     *
     * @return the output results of the executed prompt command
     *
     * @throws ExecutionException if the prompt command threw an exception, this method will throw an exception
     *         whose message is the output text that was written by the prompt command up until the error
     *         occurred. The cause of the thrown exception will be the actual exception thrown by
     *         the prompt command. This way you can see what the prompt command output was as well as
     *         the exception that occurred.
     */
    String executePromptCommand(String command) throws ExecutionException;

    /**
     * Returns the directory that is considered the "agent home" (i.e. the directory
     * where the agent is installed).
     *
     * @return agent home directory
     */
    String getAgentHomeDirectory();

    /**
     * Returns the number of times the agent has been restarted for the entire lifetime of
     * the agent's JVM.  Reasons for a restart can include the execution of the agent
     * plugin's "restart" operation or the VM Health Check detected a critical problem and
     * restarted the agent automatically. To find out why the agent was last restarted,
     * see {@link #getReasonForLastRestart()}.
     *
     * @return number of agent restarts that have occured
     */
    int getNumberAgentRestarts();

    /**
     * Returns the code that indicates why the agent was last restarted.
     *
     * @return restart code
     */
    String getReasonForLastRestart();

    /**
     * Returns the number of milliseconds this agent thinks its clock is ahead or behind from
     * its server's clock.  A positive value means the agent clock is ahead.
     *
     * @return time the agent-server clock difference
     */
    long getAgentServerClockDifference();

    /**
     * Returns the number of seconds the agent has been started - this resets everytime the agent is shutdown. This time
     * does not necessarily mean the total time the agent's VM has been running (since the agent may have been shutdown
     * and restarted without the VM ever coming down).
     *
     * @return number of seconds since the agent has been started
     */
    long getUptime();

    /**
     * @see ServiceContainerMetricsMBean#getNumberSuccessfulCommandsReceived()
     */
    long getNumberSuccessfulCommandsReceived();

    /**
     * @see ServiceContainerMetricsMBean#getNumberFailedCommandsReceived()
     */
    long getNumberFailedCommandsReceived();

    /**
     * @see ServiceContainerMetricsMBean#getNumberTotalCommandsReceived()
     */
    long getNumberTotalCommandsReceived();

    /**
     * @see ServiceContainerMetricsMBean#getAverageExecutionTime()
     */
    long getAverageExecutionTimeReceived();

    /**
     * @see ClientCommandSenderMetrics#getAverageExecutionTimeSent()
     */
    long getAverageExecutionTimeSent();

    /**
     * @see ClientCommandSenderMetrics#getNumberSuccessfulCommandsSent()
     */
    long getNumberSuccessfulCommandsSent();

    /**
     * @see ClientCommandSenderMetrics#getNumberFailedCommandsSent()
     */
    long getNumberFailedCommandsSent();

    /**
     * Combines the number of successful and failed commands sent.
     *
     * @see ClientCommandSenderMetrics#getNumberSuccessfulCommandsSent()
     * @see ClientCommandSenderMetrics#getNumberFailedCommandsSent()
     */
    long getNumberTotalCommandsSent();

    /**
     * @see ClientCommandSenderMetrics#getNumberCommandsActive()
     */
    long getNumberCommandsActiveSent();

    /**
     * @see ClientCommandSenderMetrics#getNumberCommandsInQueue()
     */
    long getNumberCommandsInQueue();

    /**
     * @see ClientCommandSenderMetrics#getNumberCommandsSpooled()
     */
    long getNumberCommandsSpooled();

    /**
     * @see ClientCommandSenderMetrics#isSending()
     */
    boolean isSending();

    /**
     * Returns the agent JVM's free memory as reported by <code>Runtime.getRuntime().freeMemory()</code>.
     *
     * @return free memory in bytes
     */
    long getJVMFreeMemory();

    /**
     * Returns the agent JVM's total memory as reported by <code>Runtime.getRuntime().totalMemory()</code>.
     *
     * @return total memory in bytes
     */
    long getJVMTotalMemory();

    /**
     * Returns the number of currently active threads in the agent's JVM.
     *
     * @return number of all active threads
     */
    int getJVMActiveThreads();

    /**
     * Returns the entire set of agent configuration preferences.
     *
     * @return agent configuration preferences
     */
    Properties getAgentConfiguration();

    /**
     * The given set of agent configuration preferences (in the form of name/value pairs in a Properties object) is
     * added to the current set of agent configuration preferences. Those preferences found in <code>config</code> that
     * already exist in the current agent configuration will override the old values in the current agent configuration.
     * Those preferences found in <code>config</code> but <i>do not</i> exist yet in the current agent configuration are
     * added to the agent configuration. If a preference exists in the current configuration but is not found in <code>
     * config</code>, then that preference is left as-is.
     *
     * <p>Changing the agent configuration usually requires the agent to be restarted in order for the new settings to
     * be picked up.</p>
     *
     * @param config new agent configuration preferences
     */
    void mergeIntoAgentConfiguration(Properties config);

    /**
     * Given the names of preferences, this will remove those preference settings from the agent configuration. This
     * will effectively force that preferences to fallback to their built-in defaults.
     *
     * @param preferenceNames the preferences to remove from the agent configuration
     */
    void removeFromAgentConfiguration(List<String> preferenceNames);
}