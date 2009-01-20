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
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import mazz.i18n.Logger;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.impl.generic.GenericCommandClient;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * A client that sends commands to a server. This is different than {@link CommandClient} implementations because it
 * maintains a queue of outgoing command requests which allows it to emit them asynchronously when appropriate Requests
 * can be sent {@link #sendSynch(Command) synchronously} or
 * {@link #sendAsynch(Command, CommandResponseCallback) asynchronously}.
 *
 * <p>You have to tell this class when it is allowed to send commands (see {@link #startSending()} and when it should no
 * longer be sending commands (see {@link #stopSending(boolean)}. You are still allowed to queue commands via
 * {@link #sendAsynch(Command, CommandResponseCallback)} even if this object was told to stop sending. The queue will
 * simply grow until its full or until this object is told to start sending again. This starting and stopping allows you
 * to cleanly switch the server that this object sends commands to - see
 * {@link #setRemoteCommunicator(RemoteCommunicator)}. See {@link ServerPollingThread} as a helper thread that can be
 * used to automatically start and stop this sender when the server comes up or goes down.</p>
 *
 * <p>This sender object supports two different kinds of throttling - <b>queue throttling</b> and <b>send
 * throttling</b>. The {@link ClientCommandSenderConfiguration configuration} defines the parameters for both - you can
 * enable one, both or none. They work independently.</p>
 *
 * <p>Queue throttling affects all commands sent asynchronously and <i>only</i> asynchronously. The queue where the
 * asynchronous commands are placed can be throttled in such a way that only X commands can be taken from the queue in
 * any T time period (called the 'burst period', specified in milliseconds). This means that if X+n commands are queued,
 * only X will be dequeued and sent in the first burst period of T milliseconds. After that T millisecond burst period
 * passes, the rest of the commands are dequeued and sent. Note that if n > X, then only the first X will be dequeued
 * until the second burst period passes. The cycle continues until queue throttling is disabled or this sender is told
 * to {@link #stopSending(boolean) stop sending messages altogether}.</p>
 *
 * <p>Send throttling affects commands sent both asynchronously and synchronously. Unlike queue throttling, an
 * individual command can be configured to ignore send throttling. That is to say, some commands can be sent immediately
 * even if send throttling is enabled. Send throttling works by allowing X commands to be sent - once that limit of X
 * commands is reached, a forced 'quiet period' begins, with that quiet period defined in milliseconds. No commands can
 * be sent until the quiet time period expires. Once the quiet period ends, another X commands can be sent before
 * another quiet period must start. The cycle continues until sent throttling is disabled or this sender is told to stop
 * sending messages altogether.</p>
 *
 * <p>Queue throttling and send throttling can be enabled simultaneously. Queue throttling occurs first, and then send
 * throttling second. That is to say, a command to be sent must pass through the queue throttle first and then must pass
 * the send throttle in order for the command to actually be sent.</p>
 *
 * @author John Mazzitelli
 */
public class ClientCommandSender {
    /**
     * This is the name of the command configuration property whose value (if it exists) defines what timeout (in
     * milliseconds) should be used when issuing commands. If this timeout is exceeded before the command response is
     * received, the command is aborted.
     */
    public static final String CMDCONFIG_PROP_TIMEOUT = "rhq.timeout";

    /**
     * This is the name of the command configuration property whose value (if it exists) defines if the command should
     * be sent with guaranteed delivery. If the property is <code>true</code>, the command will be guaranteed to be
     * delivered. The default is <code>false</code>.
     */
    public static final String CMDCONFIG_PROP_GUARANTEED_DELIVERY = "rhq.guaranteed-delivery";

    /**
     * This is the name of the command configuration property whose value (if it exists) defines if the command should
     * be sent with send-throttling enabled. That is to say, if the command sender has send-throttling enabled, this
     * command should be throttled when the sender deems it appropriate. If this configuration is not defined, it is
     * assumed <code>false</code> (that is, this command will be sent as soon as possible and ignoring any send
     * throttling that may be enabled). This setting should be used when a command is usually sent in large amounts and
     * may, if not kept in check, overload the server or cause starvation of other agents from the server.
     */
    public static final String CMDCONFIG_PROP_SEND_THROTTLE = "rhq.send-throttle";

    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(ClientCommandSender.class);

    /**
     * The object that defines what server to send commands to.
     */
    private RemoteCommunicator m_remoteCommunicator;

    /**
     * The queue that contains commands that need to be sent. Used for asychronously sending commands. The actual
     * objects queued in here will be those of type {@link ClientCommandSenderTask}. This is the queue that the thread
     * pool will use.
     */
    private final CommandQueue m_queue;

    /**
     * This is the object that implements the send throttling functionality.
     */
    private final SendThrottle m_sendThrottle;

    /**
     * Our thread pool for our asynchronous commands.
     */
    private ThreadPoolExecutor m_executor;

    /**
     * Thread index used to uniquely name threads in the executor pool.
     */
    private long m_executorIndex = 0L;

    /**
     * Our thread pool for timed command tasks to run in.
     */
    private ThreadPoolExecutor m_timerThreadPool;

    /**
     * Thread index used to uniquely name threads in the timer thread pool.
     */
    private long m_timerThreadIndex = 0L;

    /**
     * If <code>true</code>, the sender is currently enabled to send commands. If <code>false</code>, this sender will
     * not send commands, but will allow new commands to be queued up (see
     * {@link #sendAsynch(Command, CommandResponseCallback)}).
     */
    private boolean m_isSending;

    /**
     * This object is locked when this object is currently in the process of {@link #startSending() starting} or
     * {@link #stopSending() stopping}. We need to synchronize on this object when we don't want the mode changing while
     * in the middle of doing something.
     */
    private final Object m_changingModeLock = new Object();

    /**
     * This will be write-locked immediately when {@link #stopSending(boolean)} is called to indicate that the tasks in
     * the thread pool are going to be shutdown ({@link #m_shuttingDownTasks} will be set to <code>true</code> - hence
     * why the need for a write-lock).Once a read-lock is acquired, the thread can examine {@link #m_shuttingDownTasks}.
     */
    private final ReentrantReadWriteLock m_shuttingDownTasksLock = new ReentrantReadWriteLock();

    /**
     * This is a boolean that indicates if the thread pool tasks are about to be shutdown. You cannot examine this
     * boolean unless you have acquired a read-lock from {@link #m_shuttingDownTasksLock}. When this is <code>
     * true</code>, it means the tasks need to prepare to be shut down and to not obtain the {@link #m_changingModeLock}
     * since this will cause a deadlock (the tasks will be waiting for {@link #stopSending(boolean)} to release that
     * lock but that method will be waiting for the tasks to finish).
     */
    private boolean m_shuttingDownTasks;

    /**
     * If non-<code>null</code>, this is the thread that actually does the server polling.
     */
    private ServerPollingThread m_serverPollingThread;

    /**
     * A FIFO that will persist our commands when a command fails or for when we are not sending. This will only persist
     * {@link #isDeliveryGuaranteed(Command) guaranteed commands}. The actual objects that will be stored in this object
     * will be {@link CommandAndCallback} objects.
     */
    private PersistentFifo m_commandStore;

    /**
     * The configuration for this sender.
     */
    private ClientCommandSenderConfiguration m_configuration;

    /**
     * Preprocessor objects that will have an opportunity to manipulate commands before they are queued and sent.
     */
    private CommandPreprocessor[] m_preprocessors;

    /**
     * The list of state listeners that will be notified when the sender starts and stops sending.
     */
    private final List<ClientCommandSenderStateListener> m_stateListeners = new ArrayList<ClientCommandSenderStateListener>();

    /**
     * Maintains the metric data that is collected by this sender.
     */
    private ClientCommandSenderMetrics m_metrics;

    /**
     * Constructor for {@link ClientCommandSender}. Note that if the configuration's queue size is less than or equal to
     * 0, the queue will be unbounded - be careful since this means there will be no way to stop resources from being
     * used up if commands keep getting queued but nothing is getting sent.
     *
     * <p>The object will not be sending commands yet - you must explicitly call {@link #startSending()} to get this
     * object to begin to start sending commands.</p>
     *
     * @param  remote_communicator defines what server all commands will be sent to
     * @param  config              the initial configuration of this object
     *
     * @throws IllegalArgumentException if the given remote communicator or config is <code>null</code>
     */
    public ClientCommandSender(RemoteCommunicator remote_communicator, ClientCommandSenderConfiguration config)
        throws IllegalArgumentException {
        if (remote_communicator == null) {
            throw new IllegalArgumentException(LOG
                .getMsgString(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_NULL_REMOTE_COMM));
        }

        if (config == null) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_NULL_CONFIG));
        }

        // we don't store the config object itself for fear the caller will change its values in the future; make copies of the values
        m_configuration = config.copy();

        m_isSending = false;
        m_shuttingDownTasks = false;
        m_remoteCommunicator = remote_communicator;
        m_executor = null;
        m_timerThreadPool = null;
        m_queue = new CommandQueue(config);
        m_sendThrottle = new SendThrottle(config);
        m_preprocessors = null;

        if (config.commandSpoolFileName != null) {
            File cmd_spool_file = new File(config.dataDirectory, config.commandSpoolFileName);

            try {
                m_commandStore = new PersistentFifo(cmd_spool_file, config.commandSpoolFileMaxSize,
                    config.commandSpoolFilePurgePercentage, config.commandSpoolFileCompressData);
            } catch (Exception e) {
                m_commandStore = null;
                LOG.warn(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_COMMAND_SPOOL_ACCESS_ERROR, cmd_spool_file,
                    remote_communicator, e);
            }
        } else {
            m_commandStore = null;
            LOG.debug(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_NO_COMMAND_SPOOL_FILENAME, remote_communicator);
        }

        if ((config.commandPreprocessors != null) && (config.commandPreprocessors.trim().length() > 0)) {
            String[] preprocs = config.commandPreprocessors.split("\\s*:\\s*");

            if ((preprocs != null) && (preprocs.length > 0)) {
                List<CommandPreprocessor> list = new ArrayList<CommandPreprocessor>();

                for (int i = 0; i < preprocs.length; i++) {
                    try {
                        Class<?> clazz = Class.forName(preprocs[i]);
                        list.add((CommandPreprocessor) clazz.newInstance());
                    } catch (Exception e) {
                        LOG.error(e, CommI18NResourceKeys.CLIENT_COMMAND_SENDER_INVALID_PREPROCESSOR, preprocs[i],
                            remote_communicator);
                    }
                }

                setCommandPreprocessors(list.toArray(new CommandPreprocessor[list.size()]));
            }
        }

        // prepare the object that will house all the metrics collected by this sender
        // and let this new metric object know what our initial throttling settings are
        m_metrics = new ClientCommandSenderMetrics(m_queue, m_commandStore);

        if (config.enableSendThrottling) {
            m_metrics.sendThrottleMaxCommands.set(config.sendThrottleMaxCommands);
            m_metrics.sendThrottleQuietPeriodDurationMillis.set(config.sendThrottleQuietPeriodDurationMillis);
        }

        if (config.enableQueueThrottling) {
            m_metrics.queueThrottleMaxCommands.set(config.queueThrottleMaxCommands);
            m_metrics.queueThrottleBurstPeriodMillis.set(config.queueThrottleBurstPeriodMillis);
        }

        return;
    }

    /**
     * Constructor for {@link ClientCommandSender}. Note that if the configuration's queue size is less than or equal to
     * 0, the queue will be unbounded - be careful since this means there will be no way to stop resources from being
     * used up if commands keep getting queued but nothing is getting sent.
     *
     * <p>The object will not be sending commands yet - you must explicitly call {@link #startSending()} to get this
     * object to begin to start sending commands.</p>
     *
     * <p>This constructor allows you to reconstitute the queue with a set of previously queued tasks from an old sender
     * - see {@link #drainQueuedCommands()}.</p>
     *
     * @param  remote_communicator     defines what server all commands will be sent to
     * @param  config                  the initial configuration of this object
     * @param  previously_queued_tasks if not <code>null</code>, a list of previously queued tasks that we should bulk
     *                                 queue now in order to attempt to send them as soon as this sender starts
     *
     * @throws IllegalArgumentException if the given remote communicator or config is <code>null</code>
     */
    public ClientCommandSender(RemoteCommunicator remote_communicator, ClientCommandSenderConfiguration config,
        LinkedList<Runnable> previously_queued_tasks) throws IllegalArgumentException {
        this(remote_communicator, config);

        if ((previously_queued_tasks != null) && (previously_queued_tasks.size() > 0)) {
            long requeued = 0L;

            while (previously_queued_tasks.size() > 0) {
                try {
                    ClientCommandSenderTask task = getTaskFromRunnable(previously_queued_tasks.removeFirst());
                    task.setClientCommandSender(this);
                    m_queue.put(task);
                    requeued++;
                } catch (InterruptedException e) {
                    LOG.warn(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_REQUEUE_FAILED, remote_communicator);
                }
            }

            LOG.debug(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_REQUEUE, requeued, remote_communicator);
        }

        return;
    }

    /**
     * Provides a copy of this sender's configuration - because it is a copy, changes made to the returned object have
     * no effect on this sender's behavior.
     *
     * @return a copy of this sender's configuration
     */
    public ClientCommandSenderConfiguration getConfiguration() {
        return m_configuration.copy();
    }

    /**
     * Returns the communicator object that actually sends the commands to the remote endpoint.
     *
     * @return remote communicator that defines the remote endpoint where the commands will be sent
     */
    public RemoteCommunicator getRemoteCommunicator() {
        return m_remoteCommunicator;
    }

    /**
     * Returns the metrics that have currently been collected (e.g. the number of commands this sender has sent).
     *
     * @return this sender's metric data
     */
    public ClientCommandSenderMetrics getMetrics() {
        return m_metrics;
    }

    /**
     * Sets the information about the server to which all commands are sent. You can set this to immediately switch
     * which server will get sent the commands (that is, you don't have to be {@link #stopSending(boolean) stopped} in
     * order to switch).
     *
     * @param  remote_communicator information on the new server
     *
     * @throws IllegalArgumentException if the given remote communicator is <code>null</code>
     */
    public void setRemoteCommunicator(RemoteCommunicator remote_communicator) throws IllegalArgumentException {
        if (remote_communicator == null) {
            throw new IllegalArgumentException(LOG
                .getMsgString(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_NULL_REMOTE_COMM));
        }

        // we don't need to synchronize on this.  As per JLS, section 17.7:
        // "Writes to and reads of references are always atomic, regardless of whether they are implemented as 32 or 64 bit values."
        m_remoteCommunicator = remote_communicator;

        return;
    }

    /**
     * Returns the list of command preprocessors that are currently assigned to this sender. These are the objects that
     * manipulate the command prior to being queued and sent.
     *
     * @return the list of command preprocessors (will never be <code>null</code>)
     */
    public CommandPreprocessor[] getCommandPreprocessors() {
        return (m_preprocessors != null) ? m_preprocessors : new CommandPreprocessor[0];
    }

    /**
     * This method sets the list of preprocessor objects this sender will use - these are the preprocessors that will
     * have an opportunity to manipulate a command prior to it being queued and sent. If <code>null</code> is passed in,
     * or the list has a size of 0, then this sender will not preprocess commands that it sends.
     *
     * @param preprocs the objects that will be handed all commands that are to be sent by this sender for preprocessing
     *                 (may be <code>null</code> or empty)
     */
    public void setCommandPreprocessors(CommandPreprocessor[] preprocs) {
        m_preprocessors = ((preprocs != null) && (preprocs.length > 0)) ? preprocs : null;
    }

    /**
     * Adds the given listener to this sender object. When the sender {@link #startSending() starts sending} or
     * {@link #stopSending(boolean) stops sending}, this listener will be notified. If the listener already exists in
     * the list of known listeners (i.e. it has been added before), a duplicate will <b>not</b> be added; however, if
     * <code>immediately_notify</code> is <code>true</code>, it will be notified now.
     *
     * @param listener
     * @param immediately_notify if <code>true</code>, the listener will be immediately notified of the current state of
     *                           the sender; if <code>false</code>, the listener will next be notified when the state
     *                           changes next
     */
    public void addStateListener(ClientCommandSenderStateListener listener, boolean immediately_notify) {
        boolean is_sending;

        synchronized (m_stateListeners) {
            is_sending = isSending();

            if (!m_stateListeners.contains(listener)) {
                m_stateListeners.add(listener);

                LOG.debug(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_ADDED_STATE_LISTENER, listener, is_sending,
                    immediately_notify);
            }
        }

        // just like in notifyStateListeners, we do not want to be synchronized when within the listener callback code
        if (immediately_notify) {
            notifyStateListener(is_sending, listener);
        }

        return;
    }

    /**
     * Removes the given listener to this sender object. When the sender {@link #startSending() starts sending} or
     * {@link #stopSending(boolean) stops sending}, this listener will no longer be notified.
     *
     * @param listener
     */
    public void removeStateListener(ClientCommandSenderStateListener listener) {
        synchronized (m_stateListeners) {
            if (m_stateListeners.remove(listener)) {
                LOG.debug(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_REMOVED_STATE_LISTENER, listener);
            }
        }

        return;
    }

    /**
     * Notifies all state listeners of the change in state.
     *
     * @param started_sending if<code>true</code> the listeners will be notified that this object has started sending;
     *                        if <code>false</code>, the listeners will be notified that this object has stopped sending
     */
    private void notifyStateListeners(boolean started_sending) {
        LOG.debug(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_NOTIFYING_STATE_LISTENERS, started_sending);

        List<ClientCommandSenderStateListener> listeners_copy;

        // make a copy so we don't remained synchronized within the listener callback methods
        // this avoids any potential deadlocks and allows a listener to remove itself
        synchronized (m_stateListeners) {
            listeners_copy = new ArrayList<ClientCommandSenderStateListener>(m_stateListeners);
        }

        for (ClientCommandSenderStateListener listener : listeners_copy) {
            notifyStateListener(started_sending, listener);
        }

        return;
    }

    /**
     * Notifies the given state listener of the change in state.
     *
     * @param started_sending if<code>true</code> the listener will be notified that this object has started sending; if
     *                        <code>false</code>, the listener will be notified that this object has stopped sending
     * @param listener        the listener to notify
     */
    private void notifyStateListener(boolean started_sending, ClientCommandSenderStateListener listener) {
        boolean keep_listening;

        try {
            if (started_sending) {
                keep_listening = listener.startedSending(this);
            } else {
                keep_listening = listener.stoppedSending(this);
            }
        } catch (Exception e) {
            LOG.warn(e, CommI18NResourceKeys.CLIENT_COMMAND_SENDER_STATE_LISTENER_EXCEPTION, listener, e);
            keep_listening = false; // it threw an exception! remove listener
        }

        if (!keep_listening) {
            removeStateListener(listener);
        }

        return;
    }

    /**
     * Sends the command asynchronously with guaranteed delivery enabled.
     *
     * <p>The caller is not guaranteed <i>when</i> the command is sent or in what order from previously queue
     * commands.</p>
     *
     * @param  command  the command to queue and send asynchronously
     * @param  callback the callback object to be notified when the command has been sent and executed on the server
     *
     * @throws Exception if failed to queue the command
     */
    public void sendAsynchGuaranteed(Command command, CommandResponseCallback callback) throws Exception {
        command.getConfiguration().setProperty(ClientCommandSender.CMDCONFIG_PROP_GUARANTEED_DELIVERY, "true");
        sendAsynch(command, callback);
        return;
    }

    /**
     * Sends the command asynchronously. This will essentially queue the command and return. If <code>callback</code> is
     * not <code>null</code>, that callback will be notified when the command has completed and will be told the results
     * of the command execution.
     *
     * <p>The caller can configure the command for guaranteed delivery.</p>
     *
     * <p>The caller is not guaranteed <i>when</i> the command is sent or in what order from previously queue
     * commands.</p>
     *
     * @param  command  the command to queue and send asynchronously
     * @param  callback the callback object to be notified when the command has been sent and executed on the server
     *
     * @throws Exception if failed to queue the command
     */
    public void sendAsynch(Command command, CommandResponseCallback callback) throws Exception {
        try {
            preprocessCommand(command);

            CommandAndCallback cnc = new CommandAndCallback(command, callback);
            long timeout = getCommandTimeout(command);
            ClientCommandSenderTask task = new ClientCommandSenderTask(this, cnc, timeout, true, null);

            synchronized (m_changingModeLock) {
                if (isSending()) {
                    m_executor.submit((Runnable) task);
                } else {
                    // If this command wants its delivery guaranteed, we need to spool it to disk - when we start sending, it'll be queued up.
                    // Otherwise, we just queue it up in memory - if the JVM dies before we start sending commands, it is lost but so be it.
                    if (isDeliveryGuaranteed(command)) {
                        spoolCommandAndCallback(cnc);
                    } else {
                        // queue it up, failing if the buffer fills up
                        boolean is_queued = m_queue.offer(task, 1000L, TimeUnit.MILLISECONDS);

                        if (!is_queued) {
                            throw new Exception(LOG.getMsgString(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_FULL_QUEUE,
                                command));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new Exception(LOG.getMsgString(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_QUEUE_FAILED, command), e);
        }

        return;
    }

    /**
     * Sends the command synchronously. The results of the command are returned. This method blocks until the command
     * has been sent, executed on the server and the results sent back.
     *
     * <p>An exception might be thrown if the client could not send the command. Errors that occur on the server during
     * execution of the command generally are returned in the response object. Exceptions thrown by this method are
     * usually the result of rare low-level communications problems or a problem exists with the command such that it
     * cannot be sent. Callers of this method should always check the returned {@link CommandResponse#isSuccessful()}
     * flag to see if the command was actually successfully processed.</p>
     *
     * @param  command the command to send
     *
     * @return the response of the command as returned by the server
     *
     * @throws Exception             if failed to send the command
     * @throws IllegalStateException if this sender object has not been told to
     *                               {@link #startSending() start sending commands}
     */
    public CommandResponse sendSynch(Command command) throws Exception {
        if (!isSending()) {
            throw new IllegalStateException(LOG.getMsgString(
                CommI18NResourceKeys.CLIENT_COMMAND_SENDER_CANNOT_SEND_NOT_SENDING, command));
        }

        preprocessCommand(command);

        // notice that we are not blocking on the sending flag (we don't want to block here for fear of being deadlocked due to
        // the server not processing our command in a timely manner).
        // If someone calls stopSending now, it won't stop us. We are OK to continue to send this synchronously now.

        CommandAndCallback cnc = new CommandAndCallback(command, null);
        long timeout = getCommandTimeout(command);
        CommandResponse[] response = new CommandResponse[1];

        // send the command - its results will be stored in the first element of response
        ClientCommandSenderTask task = new ClientCommandSenderTask(this, cnc, timeout, false, response);
        task.run();

        return response[0];
    }

    /**
     * Returns the object that can be used to create remote POJO proxies which allows you to make remote calls to the
     * POJOs that are remoted. The caller is free to change the returned factory's asynchronous mode by calling
     * {@link ClientRemotePojoFactory#setAsynch(boolean, CommandResponseCallback)}.
     *
     * @return remote POJO factory
     */
    public ClientRemotePojoFactory getClientRemotePojoFactory() {
        ClientRemotePojoFactory factory = new ClientRemotePojoFactory(this);
        return factory;
    }

    /**
     * Turns on queue throttling and sets the queue throttling parameters such that commands are dequeued in bursts with
     * pauses in between.
     *
     * @param max_commands  the maximum number of commands that are allowed to be sent in a single burst
     * @param period_millis the time period in milliseconds that must pass before another burst of commands can be sent
     */
    public void enableQueueThrottling(long max_commands, long period_millis) {
        // our queue object only needs the queueThrottleXXX config properties set
        ClientCommandSenderConfiguration config = new ClientCommandSenderConfiguration();
        config.queueThrottleMaxCommands = max_commands;
        config.queueThrottleBurstPeriodMillis = period_millis;

        m_queue.setQueueThrottleParameters(config);
        m_queue.enableQueueThrottling();

        // let our metric object know that we changed our queue throttling settings
        m_metrics.queueThrottleMaxCommands.set(max_commands);
        m_metrics.queueThrottleBurstPeriodMillis.set(period_millis);

        return;
    }

    /**
     * Disables the queue throttling such that this sender will dequeue commands as fast as possible.
     */
    public void disableQueueThrottling() {
        m_queue.disableQueueThrottling();

        // let our metric object know that we disabled our queue throttling
        m_metrics.queueThrottleMaxCommands.set(0L);
        m_metrics.queueThrottleBurstPeriodMillis.set(0L);

        return;
    }

    /**
     * Turns on send throttling and sets the send throttling parameters such that the given maximum number of commands
     * are sent before a quiet period of the given time duration must pass without any commands being sent. If send
     * throttling was already enabled, this changes the send throttling parameters - those changes will take effect as
     * soon as possible (for example, if send throttle is already in its quiet period, the old quiet period duration
     * remains in effect; the new quiet period duration takes effect the next quiet period that occurs).
     *
     * @param max_commands          the maximum number of commands that can be sent before a quiet period must start
     * @param quiet_period_duration the length of time of the quiet period when no commands can be sent (unless the
     *                              command is configured to allow itself to be sent regardless of the send throttling)
     */
    public void enableSendThrottling(long max_commands, long quiet_period_duration) {
        // our send throttle object only needs the sendThrottleXXX config properties set
        ClientCommandSenderConfiguration config = new ClientCommandSenderConfiguration();
        config.sendThrottleMaxCommands = max_commands;
        config.sendThrottleQuietPeriodDurationMillis = quiet_period_duration;

        m_sendThrottle.setSendThrottleParameters(config);
        m_sendThrottle.enableSendThrottling();

        // let our metric object know that we changed our send throttling settings
        m_metrics.sendThrottleMaxCommands.set(max_commands);
        m_metrics.sendThrottleQuietPeriodDurationMillis.set(quiet_period_duration);

        return;
    }

    /**
     * Disables the send throttling such that this sender will send commands as fast as possible.
     */
    public void disableSendThrottling() {
        m_sendThrottle.disableSendThrottling();

        // let our metric object know that we disabled send throttling
        m_metrics.sendThrottleMaxCommands.set(0L);
        m_metrics.sendThrottleQuietPeriodDurationMillis.set(0L);

        return;
    }

    /**
     * If configured to do so, this will start server polling by periodically sending requests to the server to ensure
     * that it is online. The interval between polls is defined by the configuration passed into this object's
     * constructor. If the interval was 0 or less, this method does nothing and simply returns immediatly.
     *
     * @see #stopServerPolling()
     */
    public void startServerPolling() {
        if (m_configuration.serverPollingIntervalMillis <= 0) {
            return;
        }

        m_serverPollingThread = new ServerPollingThread(this, m_configuration.serverPollingIntervalMillis);
        m_serverPollingThread.start();

        return;
    }

    /**
     * Stops this object from {@link #startServerPolling() polling} the server. If server polling wasn't started, this
     * method does nothing.
     */
    public void stopServerPolling() {
        if (m_serverPollingThread != null) {
            m_serverPollingThread.stopPolling();
            m_serverPollingThread = null;
        }

        return;
    }

    public void addPollingListener(PollingListener listener) {
        ServerPollingThread thread = m_serverPollingThread;
        if (thread != null) {
            thread.addPollingListener(listener);
        }
        return;
    }

    public void removePollingListener(PollingListener listener) {
        ServerPollingThread thread = m_serverPollingThread;
        if (thread != null) {
            thread.removePollingListener(listener);
        }
        return;
    }

    /**
     * This starts to send commands that are found in the queue and allows commands to be synchronously sent via
     * {@link #send(Command)}. Commands are placed in the queue by {@link #sendAsynch(Command, CommandResponseCallback)}
     * . This method also puts commands on the queue that it finds in the persistence command store (for those commands
     * that failed but need to be delivered).
     *
     * <p>If this client was not already {@link #isSending() sending}, this method returns <code>true</code> to indicate
     * the client's mode has changed from stopped to started.</p>
     *
     * <p>If this client is already {@link #isSending() sending}, this method does nothing and returns <code>
     * false</code> to indicate the mode hasn't changed - it is still in the sending mode.</p>
     *
     * @return <code>true</code> if this client was not sending prior to this method being called (and hence this client
     *         changed modes); <code>false</code> if this client was already sending and thus this method ended up being
     *         a no-op
     */
    public boolean startSending() {
        boolean changed_mode = false;

        synchronized (m_changingModeLock) {
            if (!m_isSending) {
                // If you give the pooled executor a queue with items in it, it ignores those items,
                // so we need to drain the queue, instantiate the executor, and then requeue everything.
                // We can drain now because we locked m_changingModeLock - everyone else is locked out from adding to the queue.
                LinkedList<Runnable> queued_commands;

                try {
                    queued_commands = new LinkedList<Runnable>();
                    m_queue.drainTo(queued_commands);
                } catch (Exception e) {
                    queued_commands = new LinkedList<Runnable>();
                    LOG.warn(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_DRAIN_INTERRUPTED, m_remoteCommunicator);
                }

                // create and configure our thread pool with our now empty queue
                m_executor = new ThreadPoolExecutor(1, m_configuration.maxConcurrent, 60000L, TimeUnit.MILLISECONDS,
                    m_queue);
                m_executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
                m_executor.setThreadFactory(new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "ClientCommandSenderTask Thread #" + (m_executorIndex++));
                    }
                });

                // create and configure our timer thread pool
                m_timerThreadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60000L, TimeUnit.MILLISECONDS,
                    new SynchronousQueue<Runnable>());
                m_timerThreadPool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
                m_timerThreadPool.setThreadFactory(new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "ClientCommandSenderTask Timer Thread #" + (m_timerThreadIndex++));
                    }
                });

                // If there are any persisted commands, we need to unspool them and put them on the queue so they can be resent.
                // Queue these first since they were guaranteed to be delivered and hence (presumably) more important.
                // We own the changing mode lock, so no more commands can be added to the queue and no commands are currently executing
                // so nothing will be added to the command store.
                queueAllPersistedCommands();

                // now lets re-queue our commands that originally were in-memory in our queue so the thread pool will handle them
                try {
                    Runnable task;
                    int num_commands_to_queue = queued_commands.size();

                    for (int i = 0; i < num_commands_to_queue; i++) {
                        task = queued_commands.removeFirst();
                        m_executor.execute(task);
                    }
                } catch (Exception e) {
                    LOG.warn(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_REQUEUE_INTERRUPTED, m_remoteCommunicator);
                }

                m_isSending = true;
                changed_mode = true;

                LOG.debug(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_SENDING);
            }

            m_metrics.sendingMode.set(true);
        }

        if (changed_mode) {
            notifyStateListeners(true);
        }

        return changed_mode;
    }

    /**
     * This stops all sending, both synchronous and asynchronous. Asynchronous commands are still allowed to be queued
     * up, they just won't be sent. If <code>processCurrentlyQueuedCommands</code> is <code>false</code>, then any
     * current commands in the queue will be dequeued - guaranteed commands will be persisted; if and when
     * {@link #startSending()} is called, they will be re-queued (if the VM dies in the meantime, only the guaranteed
     * commands will be preserved upon the restart). Any commands currently being executed will be interrupted and, if
     * they do not require guaranteed delivery, will be lost. If <code>processCurrentlyQueuedCommands</code> is <code>
     * true</code>, then all commands in the queue will be processed - once the queue is empty and all commands sent,
     * this method will return.
     *
     * <p>If this client was not already {@link #isSending() stopped}, this method returns <code>true</code> to indicate
     * the client's mode has changed from started to stopped.</p>
     *
     * <p>If this client is already {@link #isSending() stopped}, this method does nothing and returns <code>
     * false</code> to indicate the mode hasn't changed - it is still in the stopped mode.</p>
     *
     * <p>This method will not return until the the executor thread pool has completely shutdown.</p>
     *
     * @param  process_currently_queued_commands if <code>true</code>, this will block and wait for all current commands
     *                                           in the queue to be sent before returning
     *
     * @return <code>true</code> if this client was sending prior to this method being called (and hence this client
     *         changed modes); <code>false</code> if this client was not already sending and thus this method ended up
     *         being a no-op
     */
    public boolean stopSending(boolean process_currently_queued_commands) {
        boolean changed_mode = false;

        // Try to acquire the write lock now - we will wait until any tasks currently in retry method release their read locks.
        // Once we set the shutting-down-tasks flag, any currently executing commands that fail will get persisted, rather than requeued.
        // Since we are shutting down and will no longer send commands, this is the right thing to do, even though technically
        // at this slice of time, the thread pool is still up and accepting tasks (in a second, that won't be the case).
        // Note that we have to do this prior to obtaining the m_changingModeLock.
        m_shuttingDownTasksLock.writeLock().lock();
        try {
            m_shuttingDownTasks = true;
        } finally {
            m_shuttingDownTasksLock.writeLock().unlock();
        }

        synchronized (m_changingModeLock) {
            try {
                if (m_isSending) {
                    m_isSending = false;

                    try {
                        m_executor.setKeepAliveTime(0L, TimeUnit.MILLISECONDS);
                        m_timerThreadPool.setKeepAliveTime(0L, TimeUnit.MILLISECONDS);

                        if (process_currently_queued_commands) {
                            m_executor.shutdown();
                            m_timerThreadPool.shutdown();
                        } else {
                            m_executor.shutdownNow();
                            m_timerThreadPool.shutdownNow();
                        }

                        m_executor.awaitTermination(1000L * 60 * 1, TimeUnit.MILLISECONDS);
                        m_timerThreadPool.awaitTermination(1000L * 60 * 1, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                    }

                    m_executor = null;
                    m_timerThreadPool = null;
                    m_remoteCommunicator.disconnect();

                    // We must assume that we will never be started again in this VM's lifetime
                    // so we must persist the guaranteed commands still left in the queue.
                    // All volatile commands will remain on the queue after this call in case startSending is called again.
                    persistAllQueuedCommands();

                    changed_mode = true;
                    LOG.debug(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_NO_LONGER_SENDING, m_remoteCommunicator);
                }

                m_metrics.sendingMode.set(false);
            } finally {
                // before we release the changing mode lock (and thus allow someone to call startSending again)
                // we need to flip our shutting down tasks flag to false to indicate we are done shutting down all tasks
                m_shuttingDownTasksLock.writeLock().lock();
                try {
                    m_shuttingDownTasks = false;
                } finally {
                    m_shuttingDownTasksLock.writeLock().unlock();
                }
            }
        }

        if (changed_mode) {
            notifyStateListeners(false);
        }

        return changed_mode;
    }

    /**
     * Returns the flag to indicate if this sender object has been enabled to send commands. If <code>false</code>, this
     * object can still {@link #sendAsynch(Command, CommandResponseCallback) queue up commands}, it just won't send
     * them.
     *
     * @return <code>true</code> if this sender is enabled to send commands; <code>false</code> if this sender will not
     *         send commands
     */
    public boolean isSending() {
        synchronized (m_changingModeLock) {
            return m_isSending;
        }
    }

    /**
     * This drains all commands from this sender's queue and returns them in the list. The items in the list are opaque
     * objects. The purpose of this method is to be able to reconstitute the queue in a new sender object by taking the
     * returned list and passing to the appropriate constructor when building the new sender. Typically you only call
     * this when the sender is stopped, but that isn't a hard requirement. If you do call this while this sender is not
     * sending, only volatile commands (i.e. non-guaranteed) will be in the returned list. If this sender is not
     * stopped, there may be guaranteed commands in the returned list.
     *
     * @return list of commands that are still queued waiting to be sent (<code>null</code> if we couldn't get them)
     */
    public LinkedList<Runnable> drainQueuedCommands() {
        LinkedList<Runnable> list = null;

        synchronized (m_changingModeLock) {
            try {
                list = new LinkedList<Runnable>();
                m_queue.drainTo(list);
            } catch (Exception e) {
                LOG.warn(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_DRAIN_METHOD_INTERRUPTED);
            }
        }

        return list;
    }

    /**
     * Preprocesses the given command if this sender was configured with one or more command preprocessors.
     *
     * @param command the command to preprocess
     */
    public void preprocessCommand(Command command) {
        if (m_preprocessors != null) {
            for (int i = 0; i < m_preprocessors.length; i++) {
                m_preprocessors[i].preprocess(command, this);
            }
        }

        return;
    }

    /**
     * Actually sends the command using the {@link GenericCommandClient}. This method always sends, regardless of the
     * value of {@link #isSending()}.
     *
     * <p>This is package-scoped because this is the method that the {@link ClientCommandSenderTask} will use to
     * actually send its commands. This is also used by {@link ServerPollingThread} to send poll commands.</p>
     *
     * @param  command the command to send
     *
     * @return the response of the command as returned by the server
     *
     * @throws Throwable if failed to send the command
     */
    CommandResponse send(Command command) throws Throwable {
        // Keep this method short and simple - all it should do is blindly send the command, return or throw exceptions.
        // Do not attempt to recover or otherwise persist information - callers will be responsible for error handling.
        CommandResponse response;

        try {
            GenericCommandClient client = new GenericCommandClient(m_remoteCommunicator);

            long start = System.currentTimeMillis();
            response = client.invoke(command);
            long elapsed = System.currentTimeMillis() - start;

            if ((response != null) && response.isSuccessful()) {
                long num = m_metrics.successfulCommands.incrementAndGet();

                // calculate the running average - num is the current command count
                // this may not be accurate if we execute this code concurrently,
                // but its good enough for our simple monitoring needs
                long currentAvg = m_metrics.averageExecutionTime.get();
                currentAvg = (((num - 1) * currentAvg) + elapsed) / num;
                m_metrics.averageExecutionTime.set(currentAvg);
            } else {
                m_metrics.failedCommands.incrementAndGet();
            }
        } catch (Throwable t) {
            m_metrics.failedCommands.incrementAndGet();
            throw t;
        }

        return response;
    }

    /**
     * This method is called by {@link ClientCommandSenderTask} to indicate the task attempted to send the command but
     * failed and since the command needs guaranteed delivery, it should be retried.
     *
     * <p>This is package-scoped because this is meant for use by {@link ClientCommandSenderTask}.</p>
     *
     * @param cnc the command/callback pair that needs to be resent
     */
    void retryGuaranteedTask(CommandAndCallback cnc) {
        // Because this is coming in from a task thread (running in our thread pool), we cannot lock on changing mode lock
        // during a shutdown; otherwise, during a stopSending, we will have a deadlock.
        // We use a read-write lock because under normal circumstances, shutting-down-tasks flag will always be false and we want
        // to concurrently requeue failed commands.  Allowing multiple read locks to read that flag rather than using a normal
        // mutex accomplishes this.  Only when this sender is in stopSending will that flag flip to true.  In that case, we
        // cannot call any method that acquires the changing-mode lock - we must not block and we must quickly let this
        // task thread finish so the shutdown can complete.
        // [12/19/2007 - switching from oswego to java.util.concurrent these calls to lock/unlock no longer
        //               throw InterruptedException - keeping the try-catch, converting to catch(Exception)

        try {
            m_shuttingDownTasksLock.readLock().lock();
        } catch (Exception ie1) {
            // its quite possible that the timing was just right and while waiting to acquire, someone stopped this sender hence
            // interrupting this task thread.  Let's try to acquire it again - if we get interrupted again, then that's strange
            // and we need to log an error and abort.
            try {
                m_shuttingDownTasksLock.readLock().unlock();
            } catch (Exception ie2) {
                LOG.error(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_RETRY_READ_LOCK_ACQUIRE_FAILURE, cnc.getCommand());
                return;
            }
        }

        try {
            if (m_shuttingDownTasks) {
                // hurry up and let this thread finish - the thread pool is waiting for us to die
                // since we know this sender is going to stop sending, the only thing we need to do is spool the failed command
                spoolCommandAndCallback(cnc);
            } else {
                try {
                    Thread.sleep(m_configuration.retryInterval);
                } catch (InterruptedException ie) {
                }

                // This acquires our changing mode lock.  But that's OK - since we are holding onto the read lock, stopSending
                // will never get a hold of its write lock and thus can't get the changing mode lock, avoiding a deadlock
                sendAsynch(cnc.getCommand(), cnc.getCallback());
            }
        } catch (Exception e) {
            LOG.error(e, CommI18NResourceKeys.CLIENT_COMMAND_SENDER_RETRY_FAILURE, m_remoteCommunicator, cnc
                .getCommand());
        } finally {
            m_shuttingDownTasksLock.readLock().unlock();
        }

        return;
    }

    /**
     * This method will wait for the send throttle to give us the OK to send the given command. This method will block
     * until the OK is given. Note, however, that this method will not wait for an OK and return immediately unless
     * <code>command</code> {@link #isSendThrottled(Command) is enabled for send throttling}.
     *
     * <p>This is package-scoped because this is the method that the {@link ClientCommandSenderTask} will use to
     * determine when it can send its commands.</p>
     *
     * @param command the command to send
     */
    void waitForSendThrottle(Command command) {
        if (isSendThrottled(command)) {
            m_sendThrottle.waitUntilOkToSend();
        }

        return;
    }

    /**
     * Given a command, will determine if it should be sent with guaranteed delivery.
     *
     * <p>This is package-scoped because this is the method that the {@link ClientCommandSenderTask} will use to
     * determine if a failed command needs to be re-sent.</p>
     *
     * @param  command the command
     *
     * @return <code>true</code> if the command should be sent with guaranteed delivery
     */
    boolean isDeliveryGuaranteed(Command command) {
        boolean guaranteed = false;

        if (command.getConfiguration() != null) {
            String property = command.getConfiguration().getProperty(CMDCONFIG_PROP_GUARANTEED_DELIVERY);
            guaranteed = Boolean.valueOf(property).booleanValue();
        }

        return guaranteed;
    }

    /**
     * Returns the thread pool whose threads will be used to run a timed command task. These threads will be interrupted
     * if they take too long to execute their commands.
     *
     * <p>This is package-scoped because this is the method that the {@link ClientCommandSenderTask} will use to submit
     * timed command tasks.</p>
     *
     * @return thread pool to submit timed command tasks into
     */
    ThreadPoolExecutor getTimerThreadPool() {
        return m_timerThreadPool;
    }

    /**
     * Given a command, will determine if it should be {@link #enableSendThrottling(long, long) throttled}. This will
     * return <code>true</code> iff the command is configured with the property {@link #CMDCONFIG_PROP_SEND_THROTTLE}
     * set to <code>true</code>.
     *
     * @param  command the command
     *
     * @return <code>true</code> if the command should be throttled.
     */
    private boolean isSendThrottled(Command command) {
        boolean value = false;

        if (command.getConfiguration() != null) {
            String property = command.getConfiguration().getProperty(CMDCONFIG_PROP_SEND_THROTTLE);
            value = Boolean.valueOf(property).booleanValue();
        }

        return value;
    }

    /**
     * Given a command, will determine what timeout should be used when sending it. A command define its own timeout to
     * be used in its {@link Command#getConfiguration() configuration}. If it is not defined, the default timeout will
     * be returned.
     *
     * @param  command the command whose timeout is to be determined
     *
     * @return the timeout that should be used when sending the command; if this amount of milliseconds expires before
     *         getting a response, the command should be aborted
     */
    private long getCommandTimeout(Command command) {
        long timeout = m_configuration.defaultTimeoutMillis;

        if (command.getConfiguration() != null) {
            String timeoutStr = command.getConfiguration().getProperty(CMDCONFIG_PROP_TIMEOUT);
            if (timeoutStr != null) {
                try {
                    timeout = Long.parseLong(timeoutStr);
                } catch (NumberFormatException nfe) {
                    LOG.warn(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_INVALID_TIMEOUT, timeoutStr, timeout, command);
                }
            }
        }

        return timeout;
    }

    /**
     * This will unspool all commands persisted in the command spool and add them to the thread pool queue so they will
     * execute.
     */
    private void queueAllPersistedCommands() {
        // this method is supposed to only be called from inside the sync block in startSending,
        // but let's synchronize on the changing mode lock here just in case.
        // We need to lock here because we don't want other threads adding to the queue or persisting more commands
        synchronized (m_changingModeLock) {
            if (m_commandStore != null) {
                try {
                    long timeout;
                    ClientCommandSenderTask task;
                    CommandAndCallback cnc;
                    long num_commands_persisted = m_commandStore.count();
                    long total_store_size = num_commands_persisted;

                    LOG.debug(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_LOADING_COMMAND_SPOOL, total_store_size,
                        m_remoteCommunicator);

                    while (num_commands_persisted > 0) {
                        cnc = unspoolCommandAndCallback();

                        if (cnc != null) {
                            timeout = getCommandTimeout(cnc.getCommand());
                            task = new ClientCommandSenderTask(this, cnc, timeout, true, null);
                            m_executor.execute(task);
                            num_commands_persisted--;
                        } else {
                            // this else should never have occurred since the count should have matched what is in the store
                            // unless someone else is unspooling from the store too (which should never happen, but, just in case...)
                            LOG.warn(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_UNSPOOL_CNC_FAILURE, total_store_size
                                - num_commands_persisted, num_commands_persisted, m_remoteCommunicator);
                            num_commands_persisted = 0;
                        }
                    }

                    LOG.debug(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_UNSPOOLED, total_store_size,
                        m_remoteCommunicator);
                } catch (Exception e) {
                    LOG.warn(e, CommI18NResourceKeys.CLIENT_COMMAND_SENDER_UNSPOOL_FAILURE, m_remoteCommunicator);
                }
            }
        }

        return;
    }

    /**
     * This will spool all commands found in the queue to the command store. Note that only those commands that ask for
     * guaranteed delivery will actually be persisted.
     */
    private void persistAllQueuedCommands() {
        // this method is supposed to only be called from inside the sync block in stopSending,
        // but let's synchronize on the changing mode lock here just in case.
        // We need to lock here because we don't want other threads messing with the queue
        synchronized (m_changingModeLock) {
            if (m_commandStore != null) {
                LOG.debug(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_SPOOLING);

                long commands_persisted = 0L;
                long volatile_commands = 0L;

                try {
                    // Note that we put back on the queue the volatile commands just in case we are told to start sending again.
                    // We do not put the guaranteed commands back on the queue because startSending will do that.
                    LinkedList<Runnable> remaining_tasks = new LinkedList<Runnable>();
                    m_queue.drainTo(remaining_tasks);

                    while (remaining_tasks.size() > 0) {
                        ClientCommandSenderTask next_task = getTaskFromRunnable(remaining_tasks.removeFirst());
                        Command task_command = next_task.getCommandAndCallback().getCommand();

                        if (isDeliveryGuaranteed(task_command)) {
                            spoolCommandAndCallback(next_task.getCommandAndCallback());
                            commands_persisted++;
                        } else {
                            m_queue.put(next_task);
                            volatile_commands++;
                        }
                    }
                } catch (Exception e) {
                    LOG.warn(e, CommI18NResourceKeys.CLIENT_COMMAND_SENDER_SPOOL_FAILURE, m_remoteCommunicator);
                }

                LOG.debug(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_SPOOL_DONE, commands_persisted, volatile_commands,
                    m_remoteCommunicator);
            }
        }

        return;
    }

    /**
     * Given a command/callback pair, this will spool it in its serialized form to the command spool file.
     *
     * <p>If callback is not serializable, just the command will be serialized with the callback set to <code>
     * null</code>. This means that when the pair is read back in, the command/callback pair will just contain the
     * command with the callback being <code>null</code>.</p>
     *
     * <p>If the command is not serializable, an exception is thrown.</p>
     *
     * @param cnc the command/callback to persist to the local spool file
     */
    private void spoolCommandAndCallback(CommandAndCallback cnc) {
        if (m_commandStore != null) {
            byte[] serialized_bytes;

            try {
                serialized_bytes = StreamUtil.serialize(cnc);
            } catch (RuntimeException e) {
                // assume the callback was not serializable, just serialize the command in a cnc
                LOG.warn(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_CALLBACK_NOT_SERIALIZABLE, e);
                serialized_bytes = StreamUtil.serialize(new CommandAndCallback(cnc.getCommand(), null));
            }

            try {
                m_commandStore.put(serialized_bytes);
            } catch (IOException e) {
                LOG.error(e, CommI18NResourceKeys.CLIENT_COMMAND_SENDER_PERSIST_FAILURE, cnc.getCommand());
            }
        }

        return;
    }

    /**
     * This will take the next avaialable command/callback pair from the spool file and return it.
     *
     * @return the next command/callback found in the local spool file; or <code>null</code> if the spool file is empty
     */
    private CommandAndCallback unspoolCommandAndCallback() {
        CommandAndCallback next = null;

        if (m_commandStore != null) {
            try {
                next = (CommandAndCallback) m_commandStore.takeObject();
            } catch (Exception e) {
                LOG.error(e, CommI18NResourceKeys.CLIENT_COMMAND_SENDER_COMMAND_STORE_TAKE_FAILURE,
                    m_remoteCommunicator);
            }
        }

        return next;
    }

    /**
     * Simply casts the given runnable to a task - if the runnable is not of the expected type, a runtime exception is
     * thrown. All runnables placed in our queue must be of the {@link ClientCommandSenderTask} type.
     *
     * @param  r the runnable that also must be a task
     *
     * @return the runnable cast to the proper task class
     */
    private ClientCommandSenderTask getTaskFromRunnable(Runnable r) {
        return (ClientCommandSenderTask) r;
    }
}