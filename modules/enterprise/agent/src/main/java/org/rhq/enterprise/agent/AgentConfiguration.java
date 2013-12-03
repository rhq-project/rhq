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

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import mazz.i18n.Logger;

import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.ServiceContainerConfiguration;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderConfiguration;
import org.rhq.enterprise.communications.command.client.PersistentFifo;
import org.rhq.enterprise.communications.util.SecurityUtil;

/**
 * Just provides some convienence methods to extract agent configuration properties.
 *
 * @author John Mazzitelli
 */
public class AgentConfiguration {
    /**
     * Logger
     */
    private static final Logger LOG = AgentI18NFactory.getLogger(AgentConfiguration.class);

    /**
     * This is a static utility method that builds a full remote endpoint string that combines
     * the given transport, server bind address, server bind port and server transport params.
     * This is used to get the {@link #getServerLocatorUri() full server URI} but can be used to
     * any other caller if they happen to have those four pieces of data.
     *
     * @param transport
     * @param bind_address
     * @param bind_port
     * @param transport_params
     *
     * @return a locator URI that can be used to try to communicate with an endpoint
     */
    public static String buildServerLocatorUri(String transport, String bind_address, int bind_port,
        String transport_params) {

        transport_params = transport_params.trim(); // just for my sanity

        String locator_uri = transport + "://" + bind_address + ":" + bind_port;

        if (transport_params.length() > 0) {
            // For some transports (e.g. servlet) the params will actually be the rest of the URL with optional query string.
            // To denote that we don't want to start the query string immediately after host:port, the params can start with /
            if (transport_params.startsWith("/")) {
                locator_uri += transport_params;
            } else {
                locator_uri += "/?" + transport_params;
            }
        }

        return locator_uri;
    }

    /**
     * The agent configuration properties this object wraps. This should be the agent preferences node.
     */
    private final Preferences m_preferences;

    /**
     * Wraps a preferences object in this instance.
     *
     * @param  prefs the agent configuration preferences
     *
     * @throws IllegalArgumentException if props is <code>null</code>
     */
    public AgentConfiguration(Preferences prefs) {
        if (prefs == null) {
            throw new IllegalArgumentException(LOG.getMsgString(AgentI18NResourceKeys.PREFS_MUST_NOT_BE_NULL));
        }

        m_preferences = prefs;
    }

    /**
     * Returns the raw preferences containing the agent configuration.
     *
     * @return the agent configuration preferences
     */
    public Preferences getPreferences() {
        return m_preferences;
    }

    /**
     * Returns the service container configuration object that provides strongly typed methods to retrieve the
     * server-side communications preferences as configured by the agent.
     *
     * @return server-side communications preferences
     */
    public ServiceContainerConfiguration getServiceContainerPreferences() {
        return new ServiceContainerConfiguration(m_preferences);
    }

    /**
     * Returns the version of the configuration schema.
     *
     * @return configuration version; if the configuration isn't versioned, 0 is returned
     */
    public int getAgentConfigurationVersion() {
        int value = m_preferences.getInt(AgentConfigurationConstants.CONFIG_SCHEMA_VERSION, 0);

        return value;
    }

    /**
     * This tags the existing preferences by setting the configuration schema version preference appropriately.
     */
    public void tagWithAgentConfigurationVersion() {
        m_preferences.putInt(AgentConfigurationConstants.CONFIG_SCHEMA_VERSION,
            AgentConfigurationConstants.CURRENT_CONFIG_SCHEMA_VERSION);
        flush(AgentConfigurationConstants.CONFIG_SCHEMA_VERSION);
    }

    /**
     * This returns <code>true</code> if the agent has already been setup - meaning the user has properly configured the
     * agent with the appropriate preferences. This is done either through custom configuration file or the setup prompt
     * command.
     *
     * @return <code>true</code> if the agent configuration was already setup, <code>false</code> if it has not been
     *         setup
     */
    public boolean isAgentConfigurationSetup() {
        boolean value = m_preferences.getBoolean(AgentConfigurationConstants.CONFIG_SETUP, false);
        return value;
    }

    /**
     * Sets the flag to indicate if the agent configuration has already been setup or not. <code>true</code> means the
     * agent has already been setup with the user-provided configuration preference values. <code>false</code> means the
     * user has not setup the agent.
     *
     * @param flag
     */
    public void setAgentConfigurationSetup(boolean flag) {
        m_preferences.putBoolean(AgentConfigurationConstants.CONFIG_SETUP, flag);
        flush(AgentConfigurationConstants.CONFIG_SETUP);
    }

    /**
     * Returns the unique name that this agent will be known as. This name is unique among all other agents in the
     * system. This value is usually, but doesn't have to be, the agent platform's fully qualified domain name. If the
     * agent configuration doesn't explicitly define the agent's name <code>null</code> will be returned, which is
     * usually a bad condition. An agent should never be started unless it had an agent name defined.
     *
     * @return the agent's name
     */
    public String getAgentName() {
        String name = m_preferences.get(AgentConfigurationConstants.NAME, null);
        return name;
    }

    /**
     * Returns the RHQ Server's transport type (socket, http, etc).
     *
     * @return the server transport type
     */
    public String getServerTransport() {
        String value = m_preferences.get(AgentConfigurationConstants.SERVER_TRANSPORT,
            AgentConfigurationConstants.DEFAULT_SERVER_TRANSPORT);
        return value;
    }

    /**
     * Returns the RHQ Server's bind address on which it is listening for incoming commands.
     * If the preference value isn't set, this will look up a default hostname for the
     * default server address to use. If that isn't available or defined, the local host
     * address will be used.
     *
     * @return the RHQ Server bind address
     */
    public String getServerBindAddress() {
        String address = m_preferences.get(AgentConfigurationConstants.SERVER_BIND_ADDRESS, null);
        if (address == null) {
            String alias = m_preferences.get(AgentConfigurationConstants.SERVER_ALIAS, null);
            if (alias != null) {
                try {
                    address = InetAddress.getByName(alias).getCanonicalHostName();
                } catch (Exception e1) {
                    LOG.debug(AgentI18NResourceKeys.SERVER_ALIAS_UNKNOWN, alias, e1);
                    address = null;
                }
            }

            if (address == null) {
                try {
                    address = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e2) {
                    address = "127.0.0.1";
                }
            }
        }
        return address;
    }

    /**
     * Returns the RHQ Server's bind port on which it is listening for incoming commands.
     *
     * @return the RHQ Server's bind port
     */
    public int getServerBindPort() {
        int value = m_preferences.getInt(AgentConfigurationConstants.SERVER_BIND_PORT,
            AgentConfigurationConstants.DEFAULT_SERVER_BIND_PORT);
        return value;
    }

    /**
     * Returns the RHQ Server's transport parameters which are used to further customize the RHQ Server and its
     * transport subsystem.
     *
     * @return the RHQ Server transport parameters
     */
    public String getServerTransportParams() {
        String value = m_preferences.get(AgentConfigurationConstants.SERVER_TRANSPORT_PARAMS,
            AgentConfigurationConstants.DEFAULT_SERVER_TRANSPORT_PARAMS);
        return value;
    }

    /**
     * This is a convienence method that builds a full remote endpoint string that combines
     * {@link #getServerTransport()}, {@link #getServerBindAddress()}, {@link #getServerBindPort()}, and
     * {@link #getServerTransportParams()}.
     *
     * @return the locator URI that should be used to try to communicate with the RHQ Server.
     *
     * @see #buildServerLocatorUri(String, String, int, String)
     */
    public String getServerLocatorUri() {
        String transport = getServerTransport();
        String bind_address = getServerBindAddress();
        int bind_port = getServerBindPort();
        String transport_params = getServerTransportParams();

        return buildServerLocatorUri(transport, bind_address, bind_port, transport_params);
    }

    /**
     * Convienence method that sets the transport, bind address, bind port and transport parameters
     * for a new server endpoint.  This should be used only when the agent needs to switch to a
     * new server.
     *
     * @param transport see {@link #getServerTransport()}
     * @param bindAddress see {@link #getServerBindAddress()}
     * @param bindPort see {@link #getServerBindPort()}
     * @param transportParams see {@link #getServerTransportParams()}
     */
    public void setServerLocatorUri(String transport, String bindAddress, int bindPort, String transportParams) {
        m_preferences.put(AgentConfigurationConstants.SERVER_TRANSPORT, transport);
        m_preferences.put(AgentConfigurationConstants.SERVER_BIND_ADDRESS, bindAddress);
        m_preferences.putInt(AgentConfigurationConstants.SERVER_BIND_PORT, bindPort);
        m_preferences.put(AgentConfigurationConstants.SERVER_TRANSPORT_PARAMS, transportParams);
        flush("ServerLocatorUri");
        return;
    }

    /**
     * Returns the auto-detection flag that, if true, tells the agent to attempt to auto-detect the RHQ Server coming
     * online and going offline.
     *
     * @return <code>true</code> if the agent should listen for the server coming online and going offline; <code>
     *         false</code> if the agent won't attempt to auto-discover the server (which means some other mechanism
     *         must be used to know when the client command sender should start and stop sending messages)
     */
    public boolean isServerAutoDetectionEnabled() {
        boolean flag = m_preferences.getBoolean(AgentConfigurationConstants.SERVER_AUTO_DETECTION,
            AgentConfigurationConstants.DEFAULT_SERVER_AUTO_DETECTION);

        return flag;
    }

    /**
     * Returns <code>true</code> if the agent should register itself with the RHQ Server when the agent starts up. If
     * <code>false</code>, the agent will not automatically attempt to register itself at startup - in which case the
     * agent will assume it is either already registered or it will get registered by some other mechanism.
     *
     * @return <code>true</code> if the agent should try to register itself when it starts up
     */
    public boolean isRegisterWithServerAtStartupEnabled() {
        boolean flag = m_preferences.getBoolean(AgentConfigurationConstants.REGISTER_WITH_SERVER_AT_STARTUP,
            AgentConfigurationConstants.DEFAULT_REGISTER_WITH_SERVER_AT_STARTUP);

        return flag;
    }

    /**
     * This defines how many milliseconds the agent should wait at startup for the RHQ Server to be detected. If the RHQ
     * Server has not started up in the given amount of time, the agent will continue initializing and expect the server
     * to come up later. If this is 0, the agent will not wait at all.
     *
     * @return wait time in milliseconds
     *
     * @see    #getClientCommandSenderConfiguration()
     */
    public long getWaitForServerAtStartupMsecs() {
        long value = m_preferences.getLong(AgentConfigurationConstants.WAIT_FOR_SERVER_AT_STARTUP_MSECS,
            AgentConfigurationConstants.DEFAULT_WAIT_FOR_SERVER_AT_STARTUP_MSECS);
        return value;
    }

    /**
     * Returns <code>true</code> if the agent is allowed to apply updates to itself. This means that
     * the agent will be enabled to process agent update binaries, effectively upgrading the agent
     * to a newer version.
     *
     * @return <code>true</code> if the agent is allowed to update itself
     */
    public boolean isAgentUpdateEnabled() {
        boolean flag = m_preferences.getBoolean(AgentConfigurationConstants.AGENT_UPDATE_ENABLED,
            AgentConfigurationConstants.DEFAULT_AGENT_UPDATE_ENABLED);
        return flag;
    }

    /**
     * This will return the URL that the agent should use when it needs to find out
     * the version information of the latest agent update binary.
     *
     * @return version URL if defined, <code>null</code> if not defined
     */
    public String getAgentUpdateVersionUrlIfDefined() {
        String str = m_preferences.get(AgentConfigurationConstants.AGENT_UPDATE_VERSION_URL, null);

        return str;
    }

    /**
     * This will return the URL that the agent should use when it needs to find out
     * the version information of the latest agent update binary.
     * <p>
     * If the URL is not defined, this will return a default URL that points to this agent's server.
     * If the {@link #getServerTransport() server transport} is secure, the URL returned
     * will be over "https", otherwise, it will go over "http". The
     * {@link #getServerBindAddress() server address} and {@link #getServerBindPort() server port}
     * will be the same regardless of the security transport.
     * </p>
     *
     * @return version URL
     */
    public String getAgentUpdateVersionUrl() {
        String str = m_preferences.get(AgentConfigurationConstants.AGENT_UPDATE_VERSION_URL, null);

        if (str == null) {
            String transport = SecurityUtil.isTransportSecure(getServerTransport()) ? "https" : "http";
            String address = getServerBindAddress();
            int port = getServerBindPort();
            str = transport + "://" + address + ":" + port + "/agentupdate/version";
        }

        return str;
    }

    /**
     * This will return the URL that the agent should use when it needs to download
     * the latest agent update binary.
     *
     * @return download URL if defined, <code>null</code> if not defined
     */
    public String getAgentUpdateDownloadUrlIfDefined() {
        String str = m_preferences.get(AgentConfigurationConstants.AGENT_UPDATE_DOWNLOAD_URL, null);

        return str;
    }

    /**
     * This will return the URL that the agent should use when it needs to download
     * the latest agent update binary.
     * <p>
     * If the URL is not defined, this will return a default URL that points to this agent's server.
     * If the {@link #getServerTransport() server transport} is secure, the URL returned
     * will be over "https", otherwise, it will go over "http". The
     * {@link #getServerBindAddress() server address} and {@link #getServerBindPort() server port}
     * will be the same regardless of the security transport.
     * </p>
     *
     * @return version URL
     */
    public String getAgentUpdateDownloadUrl() {
        String str = m_preferences.get(AgentConfigurationConstants.AGENT_UPDATE_DOWNLOAD_URL, null);

        if (str == null) {
            String transport = SecurityUtil.isTransportSecure(getServerTransport()) ? "https" : "http";
            String address = getServerBindAddress();
            int port = getServerBindPort();
            str = transport + "://" + address + ":" + port + "/agentupdate/download";
        }

        return str;
    }

    /**
     * This defines how many milliseconds the agent should wait between each check that ensures it is connected
     * to the primary server, as opposed to one of its failure servers.
     *
     * @return check interval time in milliseconds
     */
    public long getPrimaryServerSwitchoverCheckIntervalMsecs() {
        long value = m_preferences.getLong(AgentConfigurationConstants.PRIMARY_SERVER_SWITCHOVER_CHECK_INTERVAL_MSECS,
            AgentConfigurationConstants.DEFAULT_PRIMARY_SERVER_SWITCHOVER_CHECK_INTERVAL_MSECS);
        return value;
    }

    /**
     * This defines how many milliseconds the agent should wait between each check that determines
     * if the VM is healthy or not (such as if the VM is critically low on memory).
     *
     * @return check interval time in milliseconds
     */
    public long getVMHealthCheckIntervalMsecs() {
        long value = m_preferences.getLong(AgentConfigurationConstants.VM_HEALTH_CHECK_INTERVAL_MSECS,
            AgentConfigurationConstants.DEFAULT_VM_HEALTH_CHECK_INTERVAL_MSECS);
        return value;
    }

    /**
     * This defines when the VM health check will consider the heap memory to be critically low.
     * This is a percentage of used heap memory to max heap - when used heap is larger than this
     * percentage of max, the VM will be considered critically low on heap memory.
     *
     * @return threshold percentage, as a float
     */
    public float getVMHealthCheckLowHeapMemThreshold() {
        float value = m_preferences.getFloat(AgentConfigurationConstants.VM_HEALTH_CHECK_LOW_HEAP_MEM_THRESHOLD,
            AgentConfigurationConstants.DEFAULT_VM_HEALTH_CHECK_LOW_HEAP_MEM_THRESHOLD);
        return value;
    }

    /**
     * This defines when the VM health check will consider the nonheap memory to be critically low.
     * This is a percentage of used nonheap memory to max nonheap - when used nonheap is larger than this
     * percentage of max, the VM will be considered critically low on nonheap memory.
     *
     * @return threshold percentage, as a float
     */
    public float getVMHealthCheckLowNonHeapMemThreshold() {
        float value = m_preferences.getFloat(AgentConfigurationConstants.VM_HEALTH_CHECK_LOW_NONHEAP_MEM_THRESHOLD,
            AgentConfigurationConstants.DEFAULT_VM_HEALTH_CHECK_LOW_NONHEAP_MEM_THRESHOLD);
        return value;
    }

    /**
     * Returns <code>true</code> if the agent should update its plugins when the agent starts up. If <code>false</code>,
     * the agent will not automatically update the plugins and will use it current plugins. Note that the side effect of
     * this being <code>true</code> is that the agent will block waiting for at least one plugin to be available before
     * the plugin container starts. In other words, if this flag is <code>true</code>, a newly installed agent will not
     * fully complete its startup until it has downloaded its plugins.
     *
     * @return <code>true</code> if the agent should try to update its plugins at startup
     */
    public boolean isUpdatePluginsAtStartupEnabled() {
        boolean flag = m_preferences.getBoolean(AgentConfigurationConstants.UPDATE_PLUGINS_AT_STARTUP,
            AgentConfigurationConstants.DEFAULT_UPDATE_PLUGINS_AT_STARTUP);

        return flag;
    }

    /**
     * Returns <code>true</code> if the agent should test connectivity to all servers in
     * its failover list. Warning messages will be logged if one or more servers cannot be
     * connected to.
     *
     * @return <code>true</code> if the agent should try to test connectivity to all servers in the failover list.
     */
    public boolean isTestFailoverListAtStartupEnabled() {
        boolean flag = m_preferences.getBoolean(AgentConfigurationConstants.TEST_FAILOVER_LIST_AT_STARTUP,
            AgentConfigurationConstants.DEFAULT_TEST_FAILOVER_LIST_AT_STARTUP);

        return flag;
    }

    /**
     * Returns the data directory where all internally persisted data can be written to. If the data directory does not
     * exist, it will be created.
     *
     * <p>Because this does alittle extra work, it may not be suitable to call if you just want to get the value of the
     * data directory or if you just want to know if its defined or not. In those instances, use
     * {@link #getDataDirectoryIfDefined()} instead.</p>
     *
     * @return the data directory
     */
    public File getDataDirectory() {
        String dir_str = m_preferences.get(AgentConfigurationConstants.DATA_DIRECTORY,
            AgentConfigurationConstants.DEFAULT_DATA_DIRECTORY);

        File dir = new File(dir_str);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        return dir;
    }

    /**
     * This will return the data directory string as found in the preferences. If the data directory is not defined in
     * the preferences, <code>null</code> is returned.
     *
     * @return the data directory string as defined in the preferences or <code>null</code> if it is not defined
     *
     * @see    #getDataDirectory()
     */
    public String getDataDirectoryIfDefined() {
        String dir_str = m_preferences.get(AgentConfigurationConstants.DATA_DIRECTORY, null);

        return dir_str;
    }

    /**
     * Returns <code>true</code> if the native system should be disabled and not used by the agent or its plugin
     * container. <code>false</code> means the agent/plugin container is allowed to load in and use the native
     * libraries.
     *
     * @return <code>true</code> if the native system should not be loaded or used
     */
    public boolean isNativeSystemDisabled() {
        boolean flag = m_preferences.getBoolean(AgentConfigurationConstants.DISABLE_NATIVE_SYSTEM,
            AgentConfigurationConstants.DEFAULT_DISABLE_NATIVE_SYSTEM);
        return flag;
    }

    /**
     * Returns the client sender queue size which determines how many commands can be queued up for sending. If this is
     * 0 or less, it means the queue is unbounded.
     *
     * @return queue size
     *
     * @see    #getClientCommandSenderConfiguration()
     */
    public int getClientSenderQueueSize() {
        int value = m_preferences.getInt(AgentConfigurationConstants.CLIENT_SENDER_QUEUE_SIZE,
            AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_QUEUE_SIZE);

        return value;
    }

    /**
     * Returns the maximum number of concurrent commands that the client sender will send at any one time.
     *
     * @return max concurrent value
     *
     * @see    #getClientCommandSenderConfiguration()
     */
    public int getClientSenderMaxConcurrent() {
        int value = m_preferences.getInt(AgentConfigurationConstants.CLIENT_SENDER_MAX_CONCURRENT,
            AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_MAX_CONCURRENT);

        if (value < 1) {
            LOG.warn(AgentI18NResourceKeys.PREF_MUST_BE_GREATER_THAN_0,
                AgentConfigurationConstants.CLIENT_SENDER_MAX_CONCURRENT, value,
                AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_MAX_CONCURRENT);

            value = AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_MAX_CONCURRENT;
        }

        return value;
    }

    /**
     * Returns the default timeout that the client sender will wait for a command to be processed by the server. The
     * timeout may be less than or equal to zero in which case the default will be to never timeout commands.
     *
     * @return timeout in milliseconds
     *
     * @see    #getClientCommandSenderConfiguration()
     */
    public long getClientSenderCommandTimeout() {
        long value = m_preferences.getLong(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_TIMEOUT,
            AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_COMMAND_TIMEOUT);
        return value;
    }

    /**
     * Returns the interval the client sender should wait in between polling the server. If this value is 0 or less,
     * server polling should be disabled.
     *
     * @return server polling interval in milliseconds
     *
     * @see    #getClientCommandSenderConfiguration()
     */
    public long getClientSenderServerPollingInterval() {
        long value = m_preferences.getLong(AgentConfigurationConstants.CLIENT_SENDER_SERVER_POLLING_INTERVAL,
            AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SERVER_POLLING_INTERVAL);

        return value;
    }

    /**
     * Returns the time in milliseconds the client sender should wait in between retries of commands that have failed.
     * This is a minimum but by no means is the limit before the command must be retried. The command may, in fact, be
     * retried any amount of time after this retry interval.
     *
     * @return retry interval in milliseconds
     *
     * @see    #getClientCommandSenderConfiguration()
     */
    public long getClientSenderRetryInterval() {
        long value = m_preferences.getLong(AgentConfigurationConstants.CLIENT_SENDER_RETRY_INTERVAL,
            AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_RETRY_INTERVAL);

        return value;
    }

    /**
     * Returns the number of times a guaranteed message is retried, if it fails for a reason other than a "cannot
     * connect" to server.
     *
     * @return maximum number of retry attempts that will be made to send a guaranteed delivery message when it fails
     *         for some reason other than being unable to communicate with the server.
     *
     * @see    #getClientCommandSenderConfiguration()
     */
    public int getClientSenderMaxRetries() {
        int value = m_preferences.getInt(AgentConfigurationConstants.CLIENT_SENDER_MAX_RETRIES,
            AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_MAX_RETRIES);

        return value;
    }

    /**
     * This will return the name of the command spool file (to be located in the
     * {@link #getDataDirectory() data directory}). If this is not defined, <code>null</code> is returned to indicate
     * that commands should not be spooled to disk (thus implicitly disabling guaranteed delivery).
     *
     * @return the command spool file name or <code>null</code> if it is not defined
     */
    public String getClientSenderCommandSpoolFileName() {
        String dir_str = m_preferences.get(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_SPOOL_FILE_NAME,
            AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_COMMAND_SPOOL_FILE_NAME);

        return dir_str;
    }

    /**
     * Returns an array of command spool file parameters. The first element of the array is the maximum file size
     * threshold. The second element is the purge percentage. See {@link PersistentFifo} for the meanings of these
     * settings.
     *
     * <p>Because this is a weakly typed method (i.e. you have to know what the elements in the returned array
     * represent), it is recommended that you call {@link #getClientCommandSenderConfiguration()} because it will return
     * all the configuration, including the spool file parameters, in a more strongly typed data object.</p>
     *
     * @return array of command spool file parameters
     *
     * @see    #getClientCommandSenderConfiguration()
     */
    public long[] getClientSenderCommandSpoolFileParams() {
        String value = m_preferences.get(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_SPOOL_FILE_PARAMS,
            AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_COMMAND_SPOOL_FILE_PARAMS);

        long[] ret_params = isClientSenderCommandSpoolFileParamsValueValid(value);

        // If the config was invalid, immediately fall back to our default just so we don't bomb out with a NPE later.
        // We are guaranteed not to get a null returned if we pass in the hardcoded default params.
        // The above method will have already logged a warning for us.
        if (ret_params == null) {
            ret_params = isClientSenderCommandSpoolFileParamsValueValid(AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_COMMAND_SPOOL_FILE_PARAMS);
        }

        return ret_params;
    }

    /**
     * Given a command spool file parameters value, will determine if its valid or not. If its valid, its individual
     * parameter values are returned. If not valid, <code>null</code> is returned.
     *
     * @param  pref_value the command spool file parameters value
     *
     * @return the individual parameters values or <code>null</code> if not valid
     */
    public long[] isClientSenderCommandSpoolFileParamsValueValid(String pref_value) {
        long[] ret_params = null;

        try {
            String[] numbers = pref_value.split("\\s*:\\s*");
            if (numbers.length == 2) {
                ret_params = new long[2];
                ret_params[0] = Long.parseLong(numbers[0]);
                ret_params[1] = Long.parseLong(numbers[1]);

                if (ret_params[0] < 10000L) {
                    throw new NumberFormatException(LOG
                        .getMsgString(AgentI18NResourceKeys.COMMAND_SPOOL_INVALID_MAX_SIZE));
                }

                if ((ret_params[1] < 0L) || (ret_params[1] >= 100L)) {
                    throw new NumberFormatException(LOG
                        .getMsgString(AgentI18NResourceKeys.COMMAND_SPOOL_INVALID_PURGE_PERCENTAGE));
                }
            } else {
                throw new NumberFormatException(LOG.getMsgString(AgentI18NResourceKeys.COMMAND_SPOOL_INVALID_FORMAT));
            }
        } catch (Exception e) {
            ret_params = null;

            LOG.warn(AgentI18NResourceKeys.BAD_COMMAND_SPOOL_PREF,
                AgentConfigurationConstants.CLIENT_SENDER_COMMAND_SPOOL_FILE_PARAMS, pref_value, e);
        }

        return ret_params;
    }

    /**
     * Returns the command spool file compression flag that, if true, indicates the data in the command spool file
     * should be compressed.
     *
     * @return <code>true</code> if the command spool file should compress its data; <code>false</code> means the data
     *         should be stored in its uncompressed format.
     */
    public boolean isClientSenderCommandSpoolFileCompressed() {
        boolean flag = m_preferences.getBoolean(
            AgentConfigurationConstants.CLIENT_SENDER_COMMAND_SPOOL_FILE_COMPRESSED,
            AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_COMMAND_SPOOL_FILE_COMPRESSED);

        return flag;
    }

    /**
     * Returns an array of send throttling parameters or <code>null</code> if send throttling is to be disabled. The
     * first element of the array is the maximum number of commands that can be sent before the quiet period must start.
     * The second element is the length of time (in milliseconds) that each quiet period lasts. Once that time period
     * expires, commands can again be sent, up to the maximum (and the cycle repeats).
     *
     * <p>Because this is a weakly typed method (i.e. you have to know what the elements in the returned array
     * represent), it is recommended that you call {@link #getClientCommandSenderConfiguration()} because it will return
     * all the configuration, including the throttling configuration, in a more strongly typed data object.</p>
     *
     * @return array of send throttling parameters, <code>null</code> if send throttling is disabled
     *
     * @see    #getClientCommandSenderConfiguration()
     */
    public long[] getClientSenderSendThrottling() {
        String value = m_preferences.get(AgentConfigurationConstants.CLIENT_SENDER_SEND_THROTTLING, null);

        long[] ret_throttling_params = isClientSenderSendThrottlingValueValid(value);

        return ret_throttling_params;
    }

    /**
     * Given a send throttling parameters value, will determine if its valid or not. If its valid, its individual
     * parameter values are returned. If not valid, <code>null</code> is returned. Note that if <code>pref_value</code>
     * is <code>null</code>, then <code>null</code> will be immediately returned.
     *
     * @param  pref_value the send throttling parameters value
     *
     * @return the individual parameters values or <code>null</code> if not valid or the preference value was <code>
     *         null</code>
     */
    public long[] isClientSenderSendThrottlingValueValid(String pref_value) {
        long[] ret_throttling_params = null;

        if (pref_value != null) {
            try {
                String[] numbers = pref_value.split("\\s*:\\s*");
                if (numbers.length == 2) {
                    ret_throttling_params = new long[2];
                    ret_throttling_params[0] = Long.parseLong(numbers[0]);
                    ret_throttling_params[1] = Long.parseLong(numbers[1]);

                    if (ret_throttling_params[0] <= 0L) {
                        throw new NumberFormatException(LOG
                            .getMsgString(AgentI18NResourceKeys.SEND_THROTTLE_INVALID_MAX));
                    }

                    if (ret_throttling_params[1] < 100L) {
                        throw new NumberFormatException(LOG.getMsgString(
                            AgentI18NResourceKeys.SEND_THROTTLE_INVALID_QUIET_PERIOD, 100L));
                    }
                } else {
                    throw new NumberFormatException(LOG
                        .getMsgString(AgentI18NResourceKeys.SEND_THROTTLE_INVALID_FORMAT));
                }
            } catch (Exception e) {
                ret_throttling_params = null;

                LOG.warn(AgentI18NResourceKeys.BAD_SEND_THROTTLE_PREF,
                    AgentConfigurationConstants.CLIENT_SENDER_SEND_THROTTLING, pref_value, e);
            }
        }

        return ret_throttling_params;
    }

    /**
     * Returns an array of queue throttling parameters or <code>null</code> if queue throttling is to be disabled. The
     * first element of the array is the maximum number of commands that can be dequeued in a burst period before the
     * client sender must pause (i.e. cannot dequeue any more commands). The second element is the length of time (in
     * milliseconds) that each burst period lasts. Once that time period expires, commands can again be dequeued, up to
     * the maximum (and the cycle repeats).
     *
     * <p>Because this is a weakly typed method (i.e. you have to know what the elements in the returned array
     * represent), it is recommended that you call {@link #getClientCommandSenderConfiguration()} because it will return
     * all the configuration, including the throttling configuration, in a more strongly typed data object.</p>
     *
     * @return array of queue throttling parameters, <code>null</code> if queue throttling is disabled
     *
     * @see    #getClientCommandSenderConfiguration()
     */
    public long[] getClientSenderQueueThrottling() {
        String value = m_preferences.get(AgentConfigurationConstants.CLIENT_SENDER_QUEUE_THROTTLING, null);

        long[] ret_throttling_params = isClientSenderQueueThrottlingValueValid(value);

        return ret_throttling_params;
    }

    /**
     * Given a queue throttling parameters value, will determine if its valid or not. If its valid, its individual
     * parameter values are returned. If not valid, <code>null</code> is returned. Note that if <code>pref_value</code>
     * is <code>null</code>, then <code>null</code> will be immediately returned.
     *
     * @param  pref_value the queue throttling parameters value
     *
     * @return the individual parameters values or <code>null</code> if not valid or the preference value was <code>
     *         null</code>
     */
    public long[] isClientSenderQueueThrottlingValueValid(String pref_value) {
        long[] ret_throttling_params = null;

        if (pref_value != null) {
            try {
                String[] numbers = pref_value.split("\\s*:\\s*");
                if (numbers.length == 2) {
                    ret_throttling_params = new long[2];
                    ret_throttling_params[0] = Long.parseLong(numbers[0]);
                    ret_throttling_params[1] = Long.parseLong(numbers[1]);

                    if (ret_throttling_params[0] <= 0L) {
                        throw new NumberFormatException(LOG
                            .getMsgString(AgentI18NResourceKeys.QUEUE_THROTTLE_INVALID_MAX));
                    }

                    if (ret_throttling_params[1] < 100L) {
                        throw new NumberFormatException(LOG.getMsgString(
                            AgentI18NResourceKeys.QUEUE_THROTTLE_INVALID_BURST_PERIOD, 100L));
                    }
                } else {
                    throw new NumberFormatException(LOG
                        .getMsgString(AgentI18NResourceKeys.QUEUE_THROTTLE_INVALID_FORMAT));
                }
            } catch (Exception e) {
                ret_throttling_params = null;

                LOG.warn(AgentI18NResourceKeys.BAD_QUEUE_THROTTLE_PREF,
                    AgentConfigurationConstants.CLIENT_SENDER_QUEUE_THROTTLING, pref_value, e);
            }
        }

        return ret_throttling_params;
    }

    /**
     * This will return the fully qualified class name of the command preprocessor object that the sender will use to
     * preprocess all commands that are to be queued and sent. If this returns an empty string, no preprocessing will be
     * performed by the sender. Note that the returned string may consist of multiple class names, in which case they
     * will be separated from one another via colon characters (:).
     *
     * @return the command preprocessor class name (may be <code>null</code>)
     */
    public String getClientSenderCommandPreprocessors() {
        String value = m_preferences.get(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_PREPROCESSORS,
            AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_COMMAND_PREPROCESSORS);

        return value;
    }

    /**
     * This is a convienence method that returns the full client sender configuration. It combines all the
     * getClientSenderXXX methods and puts all the data in the returned data object.
     *
     * @return the full client sender configuration
     */
    public ClientCommandSenderConfiguration getClientCommandSenderConfiguration() {
        ClientCommandSenderConfiguration config = new ClientCommandSenderConfiguration();

        config.defaultTimeoutMillis = getClientSenderCommandTimeout();
        config.maxConcurrent = getClientSenderMaxConcurrent();
        config.queueSize = getClientSenderQueueSize();
        config.dataDirectory = getDataDirectory();
        config.serverPollingIntervalMillis = getClientSenderServerPollingInterval();
        config.commandSpoolFileCompressData = isClientSenderCommandSpoolFileCompressed();
        config.retryInterval = getClientSenderRetryInterval();
        config.maxRetries = getClientSenderMaxRetries();
        config.commandSpoolFileName = getClientSenderCommandSpoolFileName();
        config.commandPreprocessors = getClientSenderCommandPreprocessors();

        long[] cmd_spool_file_params = getClientSenderCommandSpoolFileParams();
        config.commandSpoolFileMaxSize = cmd_spool_file_params[0];
        config.commandSpoolFilePurgePercentage = (int) cmd_spool_file_params[1]; // cast is fine, we've ensured this is between 0 and 99

        long[] queue_throttling = getClientSenderQueueThrottling();
        if (queue_throttling != null) {
            config.enableQueueThrottling = true;
            config.queueThrottleMaxCommands = queue_throttling[0];
            config.queueThrottleBurstPeriodMillis = queue_throttling[1];
        } else {
            config.enableQueueThrottling = false;
        }

        long[] send_throttling = getClientSenderSendThrottling();
        if (send_throttling != null) {
            config.enableSendThrottling = true;
            config.sendThrottleMaxCommands = send_throttling[0];
            config.sendThrottleQuietPeriodDurationMillis = send_throttling[1];
        } else {
            config.enableSendThrottling = false;
        }

        // get the security settings - the client sender probably won't need these
        // these are actually set as part of the RemoteCommunicator configuration (which is passed to remoting Client)
        config.securityServerAuthMode = isClientSenderSecurityServerAuthMode();
        config.securityKeystoreFile = getClientSenderSecurityKeystoreFile();
        config.securityKeystoreType = getClientSenderSecurityKeystoreType();
        config.securityKeystoreAlgorithm = getClientSenderSecurityKeystoreAlgorithm();
        config.securityKeystorePassword = getClientSenderSecurityKeystorePassword();
        config.securityKeystoreKeyPassword = getClientSenderSecurityKeystoreKeyPassword();
        config.securityKeystoreAlias = getClientSenderSecurityKeystoreAlias();
        config.securityTruststoreFile = getClientSenderSecurityTruststoreFile();
        config.securityTruststoreType = getClientSenderSecurityTruststoreType();
        config.securityTruststoreAlgorithm = getClientSenderSecurityTruststoreAlgorithm();
        config.securityTruststorePassword = getClientSenderSecurityTruststorePassword();
        config.securitySecureSocketProtocol = getClientSenderSecuritySocketProtocol();

        return config;
    }

    /**
     * Returns the protocol used over the secure socket.
     *
     * @return protocol name
     */
    public String getClientSenderSecuritySocketProtocol() {
        String value = m_preferences.get(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL,
            AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL);
        return value;
    }

    /**
     * Returns the alias to the client's key in the keystore.
     *
     * @return alias name
     */
    public String getClientSenderSecurityKeystoreAlias() {
        String value = m_preferences.get(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_ALIAS,
            AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_KEYSTORE_ALIAS);
        return value;
    }

    /**
     * Returns the path to the keystore file. This returns a <code>String</code> as opposed to <code>File</code> since
     * some underlying remoting code may allow for this filepath to be relative to a jar inside the classloader.
     *
     * @return keystore file path
     */
    public String getClientSenderSecurityKeystoreFile() {
        String value = m_preferences.get(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_FILE, null);

        if (value == null) {
            value = new File(getDataDirectory(),
                AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_KEYSTORE_FILE_NAME).getAbsolutePath();
        }

        return value;
    }

    /**
     * Returns the algorithm used to manage the keys in the keystore.
     *
     * @return algorithm name
     */
    public String getClientSenderSecurityKeystoreAlgorithm() {
        String value = m_preferences.get(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_ALGORITHM,
            AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_KEYSTORE_ALGORITHM);
        return value;
    }

    /**
     * Returns the type of the keystore file.
     *
     * @return keystore file type
     */
    public String getClientSenderSecurityKeystoreType() {
        String value = m_preferences.get(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_TYPE,
            AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_KEYSTORE_TYPE);
        return value;
    }

    /**
     * Returns the password of the keystore file itself.
     *
     * @return keystore file password
     */
    public String getClientSenderSecurityKeystorePassword() {
        String value = m_preferences
            .get(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_PASSWORD, "rhqpwd");
        return value;
    }

    /**
     * Returns the password to gain access to the key in the keystore. If no key password is configured, this returns
     * the {@link #getClientSenderSecurityKeystorePassword() keystore password}.
     *
     * @return password to the key
     */
    public String getClientSenderSecurityKeystoreKeyPassword() {
        String value = m_preferences
            .get(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_KEY_PASSWORD, null);
        if (value == null) {
            value = getClientSenderSecurityKeystorePassword();
        }

        return value;
    }

    /**
     * Returns the path to the truststore file. This returns a <code>String</code> as opposed to <code>File</code> since
     * some underlying remoting code may allow for this filepath to be relative to a jar inside the classloader.
     *
     * @return truststore file path
     */
    public String getClientSenderSecurityTruststoreFile() {
        String value = m_preferences.get(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE, null);

        if (value == null) {
            value = new File(getDataDirectory(),
                AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE_NAME).getAbsolutePath();
        }

        return value;
    }

    /**
     * Returns the algorithm used to manage the keys in the truststore.
     *
     * @return algorithm name
     */
    public String getClientSenderSecurityTruststoreAlgorithm() {
        String value = m_preferences.get(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_ALGORITHM,
            AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_TRUSTSTORE_ALGORITHM);
        return value;
    }

    /**
     * Returns the type of the truststore file.
     *
     * @return truststore file type
     */
    public String getClientSenderSecurityTruststoreType() {
        String value = m_preferences.get(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_TYPE,
            AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_TRUSTSTORE_TYPE);
        return value;
    }

    /**
     * Returns the password of the truststore file itself.
     *
     * @return truststore file password
     */
    public String getClientSenderSecurityTruststorePassword() {
        String value = m_preferences.get(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_PASSWORD, null);
        return value;
    }

    /**
     * Returns <code>true</code> if the server authentication mode is enabled. If this is enabled, it means when using
     * secure communications, the server certificate will be authenticated with the certificates found in the agent's
     * truststore. If this is <code>false</code>, the server does not have to authenticate itself with a trusted
     * certificate; the agent will trust any remote server (in other words, the secure communications repo will only
     * be used for encryption and not authentication).
     *
     * @return server authenticate mode
     */
    public boolean isClientSenderSecurityServerAuthMode() {
        boolean flag = m_preferences.getBoolean(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE,
            AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE);
        return flag;
    }

    /**
     * This will return all the individual plugin container configuration preferences in a configuration object.
     *
     * @return plugin container configuration
     */
    public PluginContainerConfiguration getPluginContainerConfiguration() {
        // get the plugins directory
        String plugin_dir_str = m_preferences.get(AgentConfigurationConstants.PLUGINS_DIRECTORY,
            AgentConfigurationConstants.DEFAULT_PLUGINS_DIRECTORY);
        File plugin_dir = new File(plugin_dir_str);
        if (!plugin_dir.exists()) {
            plugin_dir.mkdirs();
        }

        // get the time interval in which server discoveries run
        long server_discovery_period = m_preferences.getLong(
            AgentConfigurationConstants.PLUGINS_SERVER_DISCOVERY_PERIOD,
            AgentConfigurationConstants.DEFAULT_PLUGINS_SERVER_DISCOVERY_PERIOD);
        long server_discovery_initial_delay = m_preferences.getLong(
            AgentConfigurationConstants.PLUGINS_SERVER_DISCOVERY_INITIAL_DELAY,
            AgentConfigurationConstants.DEFAULT_PLUGINS_SERVER_DISCOVERY_INITIAL_DELAY);

        // get the time interval in which service discoveries run
        long service_discovery_period = m_preferences.getLong(
            AgentConfigurationConstants.PLUGINS_SERVICE_DISCOVERY_PERIOD,
            AgentConfigurationConstants.DEFAULT_PLUGINS_SERVICE_DISCOVERY_PERIOD);
        long service_discovery_initial_delay = m_preferences.getLong(
            AgentConfigurationConstants.PLUGINS_SERVICE_DISCOVERY_INITIAL_DELAY,
            AgentConfigurationConstants.DEFAULT_PLUGINS_SERVICE_DISCOVERY_INITIAL_DELAY);

        long childResourceDiscoveryDelay = m_preferences.getLong(
            AgentConfigurationConstants.PLUGINS_CHILD_RESOURCE_DISOVERY_PERIOD,
            AgentConfigurationConstants.DEFAULT_PLUGINS_CHILD_RESOURCE_DISCOVERY_PERIOD);

        // get the time interval in which availability scans run
        long avail_scan_period = m_preferences.getLong(AgentConfigurationConstants.PLUGINS_AVAILABILITY_SCAN_PERIOD,
            AgentConfigurationConstants.DEFAULT_PLUGINS_AVAILABILITY_SCAN_PERIOD);
        long avail_scan_initial_delay = m_preferences.getLong(
            AgentConfigurationConstants.PLUGINS_AVAILABILITY_SCAN_INITIAL_DELAY,
            AgentConfigurationConstants.DEFAULT_PLUGINS_AVAILABILITY_SCAN_INITIAL_DELAY);

        // get the avail thread pool size
        int avail_scan_threadpool_size = m_preferences.getInt(
            AgentConfigurationConstants.PLUGINS_AVAILABILITY_SCAN_THREADPOOL_SIZE,
            AgentConfigurationConstants.DEFAULT_PLUGINS_AVAILABILITY_SCAN_THREADPOOL_SIZE);

        // get the initial delay before measurement collections begin
        long meas_scan_initial_delay = m_preferences.getLong(
            AgentConfigurationConstants.PLUGINS_MEASUREMENT_COLLECTION_INITIAL_DELAY,
            AgentConfigurationConstants.DEFAULT_PLUGINS_MEASUREMENT_COLLECTION_INITIAL_DELAY);

        // determine if the plugin container should explicitly be told its name
        String name = getAgentName();

        // determine how many measurement collection threads should be in the measurement threadpool
        int meas_threadpool_size = m_preferences.getInt(
            AgentConfigurationConstants.PLUGINS_MEASUREMENT_COLL_THREADPOOL_SIZE,
            AgentConfigurationConstants.DEFAULT_PLUGINS_MEASUREMENT_COLL_THREADPOOL_SIZE);

        // get the drift settings
        long drift_period = m_preferences.getLong(AgentConfigurationConstants.PLUGINS_DRIFT_DETECTION_PERIOD,
            AgentConfigurationConstants.DEFAULT_PLUGINS_DRIFT_DETECTION_PERIOD);
        long drift_initial_delay = m_preferences.getLong(
            AgentConfigurationConstants.PLUGINS_DRIFT_DETECTION_INITIAL_DELAY,
            AgentConfigurationConstants.DEFAULT_PLUGINS_DRIFT_DETECTION_INITIAL_DELAY);

        // determine how many operation invoker threads should be in the threadpool that is used to execute operations
        int op_threadpool_size = m_preferences.getInt(
            AgentConfigurationConstants.PLUGINS_OPERATION_INVOKER_THREADPOOL_SIZE,
            AgentConfigurationConstants.DEFAULT_PLUGINS_OPERATION_INVOKER_THREADPOOL_SIZE);

        // determine the default operation invocation timeout
        long op_timeout = m_preferences.getLong(AgentConfigurationConstants.PLUGINS_OPERATION_INVOCATION_TIMEOUT,
            AgentConfigurationConstants.DEFAULT_PLUGINS_OPERATION_INVOCATION_TIMEOUT);

        // get the content discovery settings
        long con_period = m_preferences.getLong(AgentConfigurationConstants.PLUGINS_CONTENT_DISCOVERY_PERIOD,
            AgentConfigurationConstants.DEFAULT_PLUGINS_CONTENT_DISCOVERY_PERIOD);
        long con_initial_delay = m_preferences.getLong(
            AgentConfigurationConstants.PLUGINS_CONTENT_DISCOVERY_INITIAL_DELAY,
            AgentConfigurationConstants.DEFAULT_PLUGINS_CONTENT_DISCOVERY_INITIAL_DELAY);
        int con_threadpool_size = m_preferences.getInt(
            AgentConfigurationConstants.PLUGINS_CONTENT_DISCOVERY_THREADPOOL_SIZE,
            AgentConfigurationConstants.DEFAULT_PLUGINS_CONTENT_DISCOVERY_THREADPOOL_SIZE);

        // get configuration discovery settings
        long config_discovery_initial_delay = m_preferences.getLong(
            AgentConfigurationConstants.PLUGINS_CONFIGURATION_DISCOVERY_INITIAL_DELAY,
            AgentConfigurationConstants.DEFAULT_PLUGINS_CONFIGURATION_DISCOVERY_INITIAL_DELAY);
        long config_discovery_period = m_preferences.getLong(
            AgentConfigurationConstants.PLUGINS_CONFIGURATION_DISCOVERY_PERIOD,
            AgentConfigurationConstants.DEFAULT_PLUGINS_CONFIGURATION_DISCOVERY_PERIOD);

        // get event sender/report settings
        long event_sender_initial_delay = m_preferences.getLong(
            AgentConfigurationConstants.PLUGINS_EVENT_SENDER_INITIAL_DELAY,
            AgentConfigurationConstants.DEFAULT_PLUGINS_EVENT_SENDER_INITIAL_DELAY);
        long event_sender_period = m_preferences.getLong(AgentConfigurationConstants.PLUGINS_EVENT_SENDER_PERIOD,
            AgentConfigurationConstants.DEFAULT_PLUGINS_EVENT_SENDER_PERIOD);
        int event_report_max_per_src = m_preferences.getInt(
            AgentConfigurationConstants.PLUGINS_EVENT_REPORT_MAX_PER_SOURCE,
            AgentConfigurationConstants.DEFAULT_PLUGINS_EVENT_REPORT_MAX_PER_SOURCE);
        int event_report_max_total = m_preferences.getInt(AgentConfigurationConstants.PLUGINS_EVENT_REPORT_MAX_TOTAL,
            AgentConfigurationConstants.DEFAULT_PLUGINS_EVENT_REPORT_MAX_TOTAL);

        // determine the data and tmp directories to use
        File data_directory = getDataDirectory();
        File tmp_directory = new File(data_directory, "tmp");
        if (!tmp_directory.exists()) {
            tmp_directory.mkdir();
        }

        // determine what, if any, plugins are to be disabled
        String disabled_pref = m_preferences.get(AgentConfigurationConstants.PLUGINS_DISABLED, null);
        List<String> disabled_plugins = null;
        if (disabled_pref != null) {
            String[] array = disabled_pref.split(",");
            disabled_plugins = new ArrayList<String>(Arrays.asList(array));
        }

        // determine what, if any, resource types are to be disabled
        String disabled_types_pref = m_preferences.get(AgentConfigurationConstants.PLUGINS_DISABLED_RESOURCE_TYPES,
            null);
        List<String> disabled_types = null;
        if (disabled_types_pref != null) {
            String[] array = disabled_types_pref.split("\\|");
            disabled_types = new ArrayList<String>(Arrays.asList(array));
        }

        // Define what plugin container/agent classes are to be hidden from our plugins.
        String clRegex = m_preferences.get(AgentConfigurationConstants.PLUGINS_ROOT_PLUGIN_CLASSLOADER_REGEX, null);
        if (clRegex == null) {

            clRegex = PluginContainerConfiguration.getDefaultClassLoaderFilter();
        }

        // now that we have all the individual preferences, let's squirrel them away in a config object
        PluginContainerConfiguration config = new PluginContainerConfiguration();

        config.setInsideAgent(true);
        config.setPluginDirectory(plugin_dir);
        config.setDataDirectory(data_directory);
        config.setTemporaryDirectory(tmp_directory);
        config.setDisabledPlugins(disabled_plugins);
        config.setDisabledResourceTypes(disabled_types);
        config.setRootPluginClassLoaderRegex(clRegex);
        config.setServerDiscoveryInitialDelay(server_discovery_initial_delay);
        config.setServerDiscoveryPeriod(server_discovery_period);
        config.setServiceDiscoveryInitialDelay(service_discovery_initial_delay);
        config.setServiceDiscoveryPeriod(service_discovery_period);
        config.setChildResourceDiscoveryDelay(childResourceDiscoveryDelay);
        config.setAvailabilityScanInitialDelay(avail_scan_initial_delay);
        config.setAvailabilityScanPeriod(avail_scan_period);
        config.setAvailabilityScanThreadPoolSize(avail_scan_threadpool_size);
        config.setMeasurementCollectionThreadPoolSize(meas_threadpool_size);
        config.setMeasurementCollectionInitialDelay(meas_scan_initial_delay);
        config.setDriftDetectionInitialDelay(drift_initial_delay);
        config.setDriftDetectionPeriod(drift_period);
        config.setOperationInvokerThreadPoolSize(op_threadpool_size);
        config.setOperationInvocationTimeout(op_timeout);
        config.setContentDiscoveryThreadPoolSize(con_threadpool_size);
        config.setContentDiscoveryInitialDelay(con_initial_delay);
        config.setContentDiscoveryPeriod(con_period);
        config.setConfigurationDiscoveryInitialDelay(config_discovery_initial_delay);
        config.setConfigurationDiscoveryPeriod(config_discovery_period);
        config.setEventSenderInitialDelay(event_sender_initial_delay);
        config.setEventSenderPeriod(event_sender_period);
        config.setEventReportMaxPerSource(event_report_max_per_src);
        config.setEventReportMaxTotal(event_report_max_total);

        if (name != null) {
            config.setContainerName(name);
        }

        return config;
    }

    /**
     * This returns the agent's security token that it needs to send with its commands to the server. If <code>
     * null</code>, it means the agent has not yet been registered with the server.
     *
     * @return security token the agent has been given by the server when it registered (may be <code>null</code>)
     */
    public String getAgentSecurityToken() {
        String value = m_preferences.get(AgentConfigurationConstants.AGENT_SECURITY_TOKEN, null);
        return value;
    }

    /**
     * Sets the agent's security token that was received from the server during agent registration. This may be set to
     * <code>null</code> which removes the agent token; however, this does not mean the agent itself is no longer
     * registered with the server. The server will still maintain a registration for the agent.
     *
     * @param value
     */
    public void setAgentSecurityToken(String value) {
        if (value != null) {
            m_preferences.put(AgentConfigurationConstants.AGENT_SECURITY_TOKEN, value);
            flush(AgentConfigurationConstants.AGENT_SECURITY_TOKEN);
        } else {
            m_preferences.remove(AgentConfigurationConstants.AGENT_SECURITY_TOKEN);
        }
    }

    /**
     * If <code>true</code> is returned, this will tell the agent it should not create its management services. In this
     * case, the agent cannot monitor itself, since the agent plugin will need these management MBean services.
     *
     * <p><i>This is usually used only during testing - not really of practical use.</i></p>
     *
     * @return plugin container startup flag
     */
    public boolean doNotEnableManagementServices() {
        boolean flag = m_preferences.getBoolean(AgentConfigurationConstants.DO_NOT_ENABLE_MANAGEMENT_SERVICES, false);
        return flag;
    }

    /**
     * If <code>true</code> is returned, this will tell the agent it should not start the Plugin Container at startup.
     * In this case, the only way for the agent to start the plugin container would be with the plugin container prompt
     * command via the agent command line.
     *
     * <p><i>This is usually used only during testing - not really of practical use.</i></p>
     *
     * @return plugin container startup flag
     */
    public boolean doNotStartPluginContainerAtStartup() {
        boolean flag = m_preferences.getBoolean(AgentConfigurationConstants.DO_NOT_START_PLUGIN_CONTAINER_AT_STARTUP,
            false);
        return flag;
    }

    /**
     * If <code>true</code> is returned, this will tell the agent it should not tell the server that the agent is
     * shutting down. In this case, the agent shutdown will be faster because it won't try to send a message to the
     * server, however, it will cause the server to not know of the agent's unavailability.
     *
     * <p><i>This is usually used only during testing - not really of practical use.</i></p>
     *
     * @return server shutdown notification flag
     */
    public boolean doNotNotifyServerOfShutdown() {
        boolean flag = m_preferences.getBoolean(AgentConfigurationConstants.DO_NOT_NOTIFY_SERVER_OF_SHUTDOWN, false);
        return flag;
    }

    /**
     * If <code>true</code> is returned, this will tell the agent it should not overlay system properties over the agent
     * configuration preferences. The default is that the agent will look at system properties and, if there is one that
     * is the same name as a preference, the system property will take effect and override the preference. If this is
     * <code>true</code>, system properties will be ignored and never override preferences.
     *
     * @return system property override flag
     */
    public boolean doNotOverridePreferencesWithSystemProperties() {
        boolean flag = m_preferences.getBoolean(AgentConfigurationConstants.DO_NOT_OVERRIDE_PREFS_WITH_SYSPROPS, false);
        return flag;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(m_preferences.absolutePath());

        buf.append('[');

        try {
            String[] keys = m_preferences.keys();

            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];

                buf.append(key);
                buf.append('=');
                buf.append(m_preferences.get(key, LOG.getMsgString(AgentI18NResourceKeys.UNKNOWN)));

                if ((i + 1) < keys.length) {
                    buf.append(',');
                }
            }
        } catch (BackingStoreException e) {
            buf.append(LOG.getMsgString(AgentI18NResourceKeys.CANNOT_GET_PREFERENCES, e));
        }

        buf.append(']');

        return buf.toString();
    }

    /**
     * Forces the preferences to flush so they get written to the backing store.
     *
     * @param changedPreference the name of the preference that was changed to
     *                          cause flush to be called (used for error log message)
     */
    private void flush(String changedPreference) {
        try {
            m_preferences.flush();
        } catch (Exception e) {
            LOG.warn(AgentI18NResourceKeys.CANNOT_STORE_PREFERENCES, changedPreference, e);
        }
    }
}
