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
package org.rhq.enterprise.server.core.comm;

import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import mazz.i18n.Logger;

import org.rhq.core.util.obfuscation.ObfuscatedPreferences;
import org.rhq.enterprise.communications.ServiceContainerConfiguration;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderConfiguration;
import org.rhq.enterprise.communications.command.client.PersistentFifo;
import org.rhq.enterprise.communications.command.client.RemoteInputStream;

/**
 * Just provides some convienence methods to extract server configuration properties. The bulk of the server
 * configuration really is just the {@link ServiceContainerConfiguration}, with some additional client-side type
 * preferences to support {@link RemoteInputStream remote streaming}.
 *
 * @author John Mazzitelli
 */
public class ServerConfiguration {
    /**
     * Logger
     */
    private static final Logger LOG = ServerI18NFactory.getLogger(ServerConfiguration.class);

    /**
     * The server configuration properties this object wraps. This should be the server preferences node.
     */
    private final Preferences m_preferences;

    /**
     * Wraps a preferences object in this instance.
     *
     * @param  prefs the configuration preferences
     *
     * @throws IllegalArgumentException if props is <code>null</code>
     */
    public ServerConfiguration(Preferences prefs) {
        if (prefs == null) {
            throw new IllegalArgumentException("prefs=null");
        }

        m_preferences = new ObfuscatedPreferences(prefs, ServerConfigurationConstants.class);
    }

    /**
     * Returns the raw preferences containing the server configuration.
     *
     * @return the server configuration preferences
     */
    public Preferences getPreferences() {
        return m_preferences;
    }

    /**
     * Returns the service container configuration object that provides strongly typed methods to retrieve the
     * server-side communications preferences.
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
    public int getServerConfigurationVersion() {
        int value = m_preferences.getInt(ServerConfigurationConstants.CONFIG_SCHEMA_VERSION, 0);

        return value;
    }

    /**
     * This tags the existing preferences by setting the configuration schema version preference appropriately.
     */
    public void tagWithServerConfigurationVersion() {
        m_preferences.putInt(ServerConfigurationConstants.CONFIG_SCHEMA_VERSION,
            ServerConfigurationConstants.CURRENT_CONFIG_SCHEMA_VERSION);
    }

    /**
     * Returns the data directory where all internally persisted data can be written to. If the data directory does not
     * exist, it will be created. The data directory is the one that is defined in the
     * {@link #getServiceContainerPreferences() service container configuration}. See
     * {@link ServiceContainerConfiguration#getDataDirectory()}.
     *
     * <p>Because this does alittle extra work, it may not be suitable to call if you just want to get the value of the
     * data directory or if you just want to know if its defined or not. In those instances, use
     * {@link #getDataDirectoryIfDefined()} instead.</p>
     *
     * @return the data directory
     */
    public File getDataDirectory() {
        return getServiceContainerPreferences().getDataDirectory();
    }

    /**
     * This will return the data directory string as found in the preferences. If the data directory is not defined in
     * the preferences, <code>null</code> is returned. The data directory is the one that is defined in the
     * {@link #getServiceContainerPreferences() service container configuration}. See
     * {@link ServiceContainerConfiguration#getDataDirectoryIfDefined()}.
     *
     * @return the data directory string as defined in the preferences or <code>null</code> if it is not defined
     *
     * @see    #getDataDirectory()
     */
    public String getDataDirectoryIfDefined() {
        return getServiceContainerPreferences().getDataDirectoryIfDefined();
    }

    /**
     * This will return the directory name where the server stores all the files it can distribute to its agents. If
     * this directory is not defined, <code>null</code> is returned. If <code>null</code> is returned, the server is not
     * configured to distribute any files remotely.
     *
     * @return the directory string as defined in the preferences or <code>null</code> if it is not defined
     */
    public String getAgentFilesDirectory() {
        String dir_str = m_preferences.get(ServerConfigurationConstants.AGENT_FILES_DIRECTORY, null);

        return dir_str;
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
        int value = m_preferences.getInt(ServerConfigurationConstants.CLIENT_SENDER_QUEUE_SIZE,
            ServerConfigurationConstants.DEFAULT_CLIENT_SENDER_QUEUE_SIZE);

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
        int value = m_preferences.getInt(ServerConfigurationConstants.CLIENT_SENDER_MAX_CONCURRENT,
            ServerConfigurationConstants.DEFAULT_CLIENT_SENDER_MAX_CONCURRENT);

        if (value < 1) {
            LOG.warn(ServerI18NResourceKeys.PREF_MUST_BE_GREATER_THAN_0,
                ServerConfigurationConstants.CLIENT_SENDER_MAX_CONCURRENT, value,
                ServerConfigurationConstants.DEFAULT_CLIENT_SENDER_MAX_CONCURRENT);

            value = ServerConfigurationConstants.DEFAULT_CLIENT_SENDER_MAX_CONCURRENT;
        }

        return value;
    }

    /**
     * Returns the default timeout that the client sender will wait for a command to be processed by the remote
     * endpoint. The timeout may be less than or equal to zero in which case the default will be to never timeout
     * commands.
     *
     * @return timeout in milliseconds
     *
     * @see    #getClientCommandSenderConfiguration()
     */
    public long getClientSenderCommandTimeout() {
        long value = m_preferences.getLong(ServerConfigurationConstants.CLIENT_SENDER_COMMAND_TIMEOUT,
            ServerConfigurationConstants.DEFAULT_CLIENT_SENDER_COMMAND_TIMEOUT);
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
        long value = m_preferences.getLong(ServerConfigurationConstants.CLIENT_SENDER_RETRY_INTERVAL,
            ServerConfigurationConstants.DEFAULT_CLIENT_SENDER_RETRY_INTERVAL);

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
        int value = m_preferences.getInt(ServerConfigurationConstants.CLIENT_SENDER_MAX_RETRIES,
            ServerConfigurationConstants.DEFAULT_CLIENT_SENDER_MAX_RETRIES);

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
        String str = m_preferences.get(ServerConfigurationConstants.CLIENT_SENDER_COMMAND_SPOOL_FILE_NAME,
            ServerConfigurationConstants.DEFAULT_CLIENT_SENDER_COMMAND_SPOOL_FILE_NAME);

        if (str != null && str.length() == 0) {
            str = null;
        }

        return str;
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
        String value = m_preferences.get(ServerConfigurationConstants.CLIENT_SENDER_COMMAND_SPOOL_FILE_PARAMS,
            ServerConfigurationConstants.DEFAULT_CLIENT_SENDER_COMMAND_SPOOL_FILE_PARAMS);

        long[] ret_params = isClientSenderCommandSpoolFileParamsValueValid(value);

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
                        .getMsgString(ServerI18NResourceKeys.COMMAND_SPOOL_INVALID_MAX_SIZE));
                }

                if ((ret_params[1] < 0L) || (ret_params[1] >= 100L)) {
                    throw new NumberFormatException(LOG
                        .getMsgString(ServerI18NResourceKeys.COMMAND_SPOOL_INVALID_PURGE_PERCENTAGE));
                }
            } else {
                throw new NumberFormatException(LOG.getMsgString(ServerI18NResourceKeys.COMMAND_SPOOL_INVALID_FORMAT));
            }
        } catch (Exception e) {
            ret_params = null;

            LOG.warn(ServerI18NResourceKeys.BAD_COMMAND_SPOOL_PREF,
                ServerConfigurationConstants.CLIENT_SENDER_COMMAND_SPOOL_FILE_PARAMS, pref_value, e);
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
            ServerConfigurationConstants.CLIENT_SENDER_COMMAND_SPOOL_FILE_COMPRESSED,
            ServerConfigurationConstants.DEFAULT_CLIENT_SENDER_COMMAND_SPOOL_FILE_COMPRESSED);

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
        String value = m_preferences.get(ServerConfigurationConstants.CLIENT_SENDER_SEND_THROTTLING, null);

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
                            .getMsgString(ServerI18NResourceKeys.SEND_THROTTLE_INVALID_MAX));
                    }

                    if (ret_throttling_params[1] < 100L) {
                        throw new NumberFormatException(LOG.getMsgString(
                            ServerI18NResourceKeys.SEND_THROTTLE_INVALID_QUIET_PERIOD, 100L));
                    }
                } else {
                    throw new NumberFormatException(LOG
                        .getMsgString(ServerI18NResourceKeys.SEND_THROTTLE_INVALID_FORMAT));
                }
            } catch (Exception e) {
                ret_throttling_params = null;

                LOG.warn(ServerI18NResourceKeys.BAD_SEND_THROTTLE_PREF,
                    ServerConfigurationConstants.CLIENT_SENDER_SEND_THROTTLING, pref_value, e);
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
    public int[] getClientSenderQueueThrottling() {
        String value = m_preferences.get(ServerConfigurationConstants.CLIENT_SENDER_QUEUE_THROTTLING, null);

        int[] ret_throttling_params = isClientSenderQueueThrottlingValueValid(value);

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
    public int[] isClientSenderQueueThrottlingValueValid(String pref_value) {
        int[] ret_throttling_params = null;

        if (pref_value != null) {
            try {
                String[] numbers = pref_value.split("\\s*:\\s*");
                if (numbers.length == 2) {
                    ret_throttling_params = new int[2];
                    ret_throttling_params[0] = Integer.parseInt(numbers[0]);
                    ret_throttling_params[1] = Integer.parseInt(numbers[1]);

                    if (ret_throttling_params[0] <= 0L) {
                        throw new NumberFormatException(LOG
                            .getMsgString(ServerI18NResourceKeys.QUEUE_THROTTLE_INVALID_MAX));
                    }

                    if (ret_throttling_params[1] < 100L) {
                        throw new NumberFormatException(LOG.getMsgString(
                            ServerI18NResourceKeys.QUEUE_THROTTLE_INVALID_BURST_PERIOD, 100L));
                    }
                } else {
                    throw new NumberFormatException(LOG
                        .getMsgString(ServerI18NResourceKeys.QUEUE_THROTTLE_INVALID_FORMAT));
                }
            } catch (Exception e) {
                ret_throttling_params = null;

                LOG.warn(ServerI18NResourceKeys.BAD_QUEUE_THROTTLE_PREF,
                    ServerConfigurationConstants.CLIENT_SENDER_QUEUE_THROTTLING, pref_value, e);
            }
        }

        return ret_throttling_params;
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
        config.serverPollingIntervalMillis = -1;
        config.commandSpoolFileCompressData = isClientSenderCommandSpoolFileCompressed();
        config.retryInterval = getClientSenderRetryInterval();
        config.maxRetries = getClientSenderMaxRetries();
        config.commandSpoolFileName = getClientSenderCommandSpoolFileName();

        long[] cmd_spool_file_params = getClientSenderCommandSpoolFileParams();
        config.commandSpoolFileMaxSize = cmd_spool_file_params[0];
        config.commandSpoolFilePurgePercentage = (int) cmd_spool_file_params[1]; // cast is fine, we've ensured this is between 0 and 99

        int[] queue_throttling = getClientSenderQueueThrottling();
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
        String value = m_preferences.get(ServerConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "TLS");
        return value;
    }

    /**
     * Returns the alias to the client's key in the keystore.
     *
     * @return alias name
     */
    public String getClientSenderSecurityKeystoreAlias() {
        String value = m_preferences.get(ServerConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_ALIAS, "rhq");
        return value;
    }

    /**
     * Returns the path to the keystore file. This returns a <code>String</code> as opposed to <code>File</code> since
     * some underlying remoting code may allow for this filepath to be relative to a jar inside the classloader.
     *
     * @return keystore file path
     */
    public String getClientSenderSecurityKeystoreFile() {
        String value = m_preferences.get(ServerConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_FILE, null);

        if (value == null) {
            value = new File(getDataDirectory(), "keystore.dat").getAbsolutePath();
        }

        return value;
    }

    /**
     * Returns the algorithm used to manage the keys in the keystore.
     *
     * @return algorithm name
     */
    public String getClientSenderSecurityKeystoreAlgorithm() {
        String value = m_preferences.get(ServerConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_ALGORITHM,
            (isIBM() ? "IbmX509" : "SunX509"));
        return value;
    }

    /**
     * Returns the type of the keystore file.
     *
     * @return keystore file type
     */
    public String getClientSenderSecurityKeystoreType() {
        String value = m_preferences.get(ServerConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_TYPE, "JKS");
        return value;
    }

    /**
     * Returns the password of the keystore file itself.
     *
     * @return keystore file password
     */
    public String getClientSenderSecurityKeystorePassword() {
        String value = m_preferences.get(ServerConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_PASSWORD,
            "rhqpwd");
        return value;
    }

    /**
     * Returns the password to gain access to the key in the keystore. If no key password is configured, this returns
     * the {@link #getClientSenderSecurityKeystorePassword() keystore password}.
     *
     * @return password to the key
     */
    public String getClientSenderSecurityKeystoreKeyPassword() {
        String value = m_preferences.get(ServerConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_KEY_PASSWORD,
            null);
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
        String value = m_preferences.get(ServerConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE, null);

        if (value == null) {
            value = new File(getDataDirectory(), "truststore.dat").getAbsolutePath();
        }

        return value;
    }

    /**
     * Returns the algorithm used to manage the keys in the truststore.
     *
     * @return algorithm name
     */
    public String getClientSenderSecurityTruststoreAlgorithm() {
        String value = m_preferences.get(ServerConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_ALGORITHM,
            (isIBM() ? "IbmX509" : "SunX509"));
        return value;
    }

    /**
     * Returns the type of the truststore file.
     *
     * @return truststore file type
     */
    public String getClientSenderSecurityTruststoreType() {
        String value = m_preferences.get(ServerConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_TYPE, "JKS");
        return value;
    }

    /**
     * Returns the password of the truststore file itself.
     *
     * @return truststore file password
     */
    public String getClientSenderSecurityTruststorePassword() {
        String value = m_preferences.get(ServerConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_PASSWORD, null);
        return value;
    }

    /**
     * Returns <code>true</code> if the server authentication mode is enabled. If this is enabled, it means when using
     * secure communications, the agents' certificates will be authenticated with the certificates found in the server's
     * truststore. If this is <code>false</code>, the agents do not have to authenticate themselves with a trusted
     * certificate; the server will trust any remote agent (in other words, the secure communications repo will only
     * be used for encryption and not authentication).
     *
     * @return server authenticate mode
     */
    public boolean isClientSenderSecurityServerAuthMode() {
        boolean flag = m_preferences.getBoolean(ServerConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE,
            ServerConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE);
        return flag;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer(m_preferences.absolutePath());

        buf.append('[');

        try {
            String[] keys = m_preferences.keys();

            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];

                buf.append(key);
                buf.append('=');
                if (key.toLowerCase().contains("password")) {
                    buf.append("***");
                } else {
                    buf.append(m_preferences.get(key, LOG.getMsgString(ServerI18NResourceKeys.UNKNOWN)));
                }

                if ((i + 1) < keys.length) {
                    buf.append(',');
                }
            }
        } catch (BackingStoreException e) {
            buf.append(LOG.getMsgString(ServerI18NResourceKeys.CANNOT_GET_PREFERENCES, e));
        }

        buf.append(']');

        return buf.toString();
    }

    private boolean isIBM() {
        return System.getProperty("java.vendor", "").contains("IBM");
    }
}