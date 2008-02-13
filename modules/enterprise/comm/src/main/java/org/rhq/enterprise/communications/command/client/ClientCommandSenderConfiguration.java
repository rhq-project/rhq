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
package org.rhq.enterprise.communications.command.client;

import java.io.File;
import java.io.Serializable;

/**
 * This is a simple object that encapsulates initial configuration settings for the client command sender. Its purpose
 * is to make the {@link ClientCommandSender} constructor less prone to change as new configuration items are added in
 * future versions. There are no getter/setter methods - this object provides public access to all data members. There
 * is a {@link #copy()} method should you wish to make a copy of these values.
 *
 * @author John Mazzitelli
 */
public class ClientCommandSenderConfiguration implements Serializable {
    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * the maximum number of commands that can be queued up (if 0 or less, its unbounded).
     */
    public int queueSize = 10000;

    /**
     * the maximum number of commands that can concurrently be sent.
     */
    public int maxConcurrent = 5;

    /**
     * milliseconds to wait for a command to be sent before timing out (commands can override this in their
     * configuration). If this is less than or equal to 0, by default a command is never timed out (while this could
     * conceivably cause a thread to hang waiting for a rogue command to never finish, it also reduces the amount of
     * short-lived threads created by the system and increases throughput).
     */
    public long defaultTimeoutMillis = 30000L;

    /**
     * If <code>true</code>, the sender will be initialized with queue throttling enabled. The throttling parameters are
     * defined by {@link #queueThrottleMaxCommands} and {@link #queueThrottleBurstPeriodMillis}. If <code>false</code>,
     * the sender will disable its queue throttling mechanism and dequeue commands as fast as it can.
     */
    public boolean enableQueueThrottling = false;

    /**
     * The maximum number of commands that can be dequeued during a burst period. This defines the size of each "burst".
     *
     * <p>This will be ignored if {@link #enableQueueThrottling} is <code>false</code>.</p>
     */
    public long queueThrottleMaxCommands = 100L;

    /**
     * The number of milliseconds the queue throttling mechanism will wait before allowing more commands (over the
     * {@link #maxConcurrent}) to be dequeued. This is the time period of each "burst".
     *
     * <p>This will be ignored if {@link #enableQueueThrottling} is <code>false</code>.</p>
     */
    public long queueThrottleBurstPeriodMillis = 5000L;

    /**
     * If <code>true</code>, then "send throttling" will be enabled. This means that messages will not be sent during a
     * quiet period defined by the send throttling parameters {@link #sendThrottleMaxCommands} and
     * {@link #sendThrottleQuietPeriodDurationMillis}. If <code>false</code> then all messages that are asked to be sent
     * will be sent immediately without a quiet period being enforced. Note that only commands that are configured for
     * "send-throtting" will be affected by this setting.
     */
    public boolean enableSendThrottling = false;

    /**
     * The maximum number of commands that are allowed to be sent before the send throttling's quiet period starts.
     *
     * <p>This will be ignored if {@link #enableSendThrottling} is <code>false</code>.</p>
     */
    public long sendThrottleMaxCommands = 100L;

    /**
     * The duration of the send throttling's quiet period. Commands will not be sent during this quiet period, unless
     * the commands are configured to explicitly ignore the send throttling quiet period.
     *
     * <p>This will be ignored if {@link #enableSendThrottling} is <code>false</code>.</p>
     */
    public long sendThrottleQuietPeriodDurationMillis = 5000L;

    /**
     * If larger than 0, this indicates the sender should periodically poll the server to make sure its still up or (if
     * it was down) see when it comes back up. The value is the number of milliseconds to wait in between polls.
     */
    public long serverPollingIntervalMillis = 60000L;

    /**
     * A path to a data directory where this sender can store things on the file system - like persisted commands.
     */
    public File dataDirectory;

    /**
     * This is the time period the sender will wait before retrying to send a guaranteed command that previously failed.
     * Note: if the sender is currently waiting in this retry period, the agent will not be able to be shutdown. If the
     * agent is asked to shutdown, it will wait until any current retries are completed. This is to help ensure those
     * commands are not lost.
     */
    public long retryInterval = 10000L;

    /**
     * If a guaranteed delivery message is sent, but the sender fails to connect to the server and deliver the message,
     * it will always be retried. However, if the error was something other than a "cannot connect" error, the command
     * will only be retried this amount of times before the command is dropped. When this happens, the guaranteed
     * command will never be delivered. This will normally happen under very odd and rare circumstances (bugs in the
     * software is one cause).
     */
    public int maxRetries = 10;

    /**
     * The name of the command spool file (to be located in the {@link #dataDirectory }}. If this value is <code>
     * null</code>, it will be assumed that commands should not be persisted (this means guaranteed delivery will be
     * implicitly unsupported).
     */
    public String commandSpoolFileName;

    /**
     * This is the maximum size of the command spool file. If the file grows beyond this, it will be purged in order to
     * shrink its size down.
     */
    public long commandSpoolFileMaxSize = 1000000L;

    /**
     * If the command spool file crosses its max size threshold and a purge is initiated, this is the percentage of
     * bytes the command spool file will be allowed to be after the purge completes. This is a percentage of
     * {@link #commandSpoolFileMaxSize}. See {@link PersistentFifo} for more info on this parameter.
     */
    public int commandSpoolFilePurgePercentage = 75;

    /**
     * If this flag is <code>true</code>, the commands stored in the spool file will be compressed. This can potentially
     * save about 30%-40% in disk space (give or take), however, it slows down the persistence considerably. Recommended
     * setting for this should be <code>false</code> unless something on the agent's deployment box warrants disk-saving
     * over persistence performance. The performance hit will only appear when unusual conditions occur, such as
     * shutting down while some guaranteed commands haven't been sent yet or while the server is down. It will not
     * affect the agent under normal conditions (while running with the server up and communicating with the agent). In
     * those unusual/rare conditions, having performance degradation may not be as important.
     */
    public boolean commandSpoolFileCompressData = false;

    /**
     * A fully qualified class name of a {@link CommandPreprocessor} implementation that will be used to preprocess all
     * commands that are to be queued and sent by the client command sender. May be <code>null</code> or empty string in
     * which case no preprocessor is specified. You may optionally specify multiple class names, separating each class
     * name with a colon (e.g. <code>org.foo.Preproc1:org.foo.Preproc2</code>).
     */
    public String commandPreprocessors;

    /**
     * When sending over an SSL socket, this will determine if the server must be authenticated in order for the
     * handshake to be successful.
     */
    public boolean securityServerAuthMode;

    /**
     * The path to the keystore file (that contain's the client's key).
     */
    public String securityKeystoreFile;

    /**
     * The type of file that the keystore file is.
     */
    public String securityKeystoreType;

    /**
     * The key management algorithm used in the keystore file.
     */
    public String securityKeystoreAlgorithm;

    /**
     * The password that gains access to the keystore file.
     */
    public String securityKeystorePassword;

    /**
     * The password that gains access to the client key within the keystore file.
     */
    public String securityKeystoreKeyPassword;

    /**
     * The alias of the client key within the keystore file.
     */
    public String securityKeystoreAlias;

    /**
     * The path to the truststore that contains the public keys of all trusted remote endpoints.
     */
    public String securityTruststoreFile;

    /**
     * The type of file that the truststore file is.
     */
    public String securityTruststoreType;

    /**
     * The key management algorithm used in the truststore file.
     */
    public String securityTruststoreAlgorithm;

    /**
     * The password that gains access to the truststore file.
     */
    public String securityTruststorePassword;

    /**
     * The secure protocol used when connecting to the remote server's socket.
     */
    public String securitySecureSocketProtocol;

    /**
     * Makes a copy of this object; use this if you wish to isolate the caller from seeing changes made to the original
     * object.
     *
     * @return a copy of the configuration values
     */
    public ClientCommandSenderConfiguration copy() {
        ClientCommandSenderConfiguration config_copy = new ClientCommandSenderConfiguration();

        config_copy.queueSize = this.queueSize;
        config_copy.maxConcurrent = this.maxConcurrent;
        config_copy.defaultTimeoutMillis = this.defaultTimeoutMillis;
        config_copy.enableQueueThrottling = this.enableQueueThrottling;
        config_copy.queueThrottleMaxCommands = this.queueThrottleMaxCommands;
        config_copy.queueThrottleBurstPeriodMillis = this.queueThrottleBurstPeriodMillis;
        config_copy.enableSendThrottling = this.enableSendThrottling;
        config_copy.sendThrottleMaxCommands = this.sendThrottleMaxCommands;
        config_copy.sendThrottleQuietPeriodDurationMillis = this.sendThrottleQuietPeriodDurationMillis;
        config_copy.serverPollingIntervalMillis = this.serverPollingIntervalMillis;
        config_copy.dataDirectory = this.dataDirectory;
        config_copy.retryInterval = this.retryInterval;
        config_copy.maxRetries = this.maxRetries;
        config_copy.commandSpoolFileName = this.commandSpoolFileName;
        config_copy.commandSpoolFileMaxSize = this.commandSpoolFileMaxSize;
        config_copy.commandSpoolFilePurgePercentage = this.commandSpoolFilePurgePercentage;
        config_copy.commandSpoolFileCompressData = this.commandSpoolFileCompressData;
        config_copy.commandPreprocessors = this.commandPreprocessors;
        config_copy.securityServerAuthMode = this.securityServerAuthMode;
        config_copy.securityKeystoreFile = this.securityKeystoreFile;
        config_copy.securityKeystoreType = this.securityKeystoreType;
        config_copy.securityKeystoreAlgorithm = this.securityKeystoreAlgorithm;
        config_copy.securityKeystorePassword = this.securityKeystorePassword;
        config_copy.securityKeystoreKeyPassword = this.securityKeystoreKeyPassword;
        config_copy.securityKeystoreAlias = this.securityKeystoreAlias;
        config_copy.securityTruststoreFile = this.securityTruststoreFile;
        config_copy.securityTruststoreType = this.securityTruststoreType;
        config_copy.securityTruststoreAlgorithm = this.securityTruststoreAlgorithm;
        config_copy.securityTruststorePassword = this.securityTruststorePassword;
        config_copy.securitySecureSocketProtocol = this.securitySecureSocketProtocol;

        return config_copy;
    }

    /**
     * If the caller knows this configuration will be assigned to a client command sender that will <i>never</i> need
     * SSL security, the caller can clear (i.e. set to <code>null</code>) all the security-related configuration
     * settings.
     *
     * <p>This is useful because if JBoss/Remoting sees security configuration, it will attempt to create an SSL socket
     * factory even if the transport won't actually need it. See <code>
     * org.jboss.remoting.AbstractInvoker.needsCustomSSLConfiguration()</code>.
     */
    public void clearSecuritySettings() {
        this.securityServerAuthMode = false;
        this.securityKeystoreFile = null;
        this.securityKeystoreType = null;
        this.securityKeystoreAlgorithm = null;
        this.securityKeystorePassword = null;
        this.securityKeystoreKeyPassword = null;
        this.securityKeystoreAlias = null;
        this.securityTruststoreFile = null;
        this.securityTruststoreType = null;
        this.securityTruststoreAlgorithm = null;
        this.securityTruststorePassword = null;
        this.securitySecureSocketProtocol = null;
    }
}