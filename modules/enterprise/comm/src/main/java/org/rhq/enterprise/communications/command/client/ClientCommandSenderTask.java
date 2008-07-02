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

import java.net.ConnectException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import mazz.i18n.Logger;

import org.jboss.remoting.CannotConnectException;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.impl.generic.GenericCommandResponse;
import org.rhq.enterprise.communications.command.impl.remotepojo.RemotePojoInvocationCommand;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;
import org.rhq.enterprise.communications.util.NotPermittedException;

/**
 * This is runnable task that will be queued in the executor pool within the {@link ClientCommandSender}. It is a
 * Callable to support the timed execution of the task (that is, it can be used to abort if command failed to be
 * processed in a given amount of time. It is a Runnable to allow us to submit this as a task to a thread pool.
 *
 * <p>This is package-scoped because it must live with {@link ClientCommandSender}. This class must be careful calling
 * into its {@link ClientCommandSender}, specifically those sender methods that require a monitor lock to protect
 * against doing things while changing modes between sending/not sending. Deadlocks may occur if you are not careful
 * calling back into the sender.</p>
 *
 * @author John Mazzitelli
 */
class ClientCommandSenderTask implements Callable<CommandResponse>, Runnable {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(ClientCommandSenderTask.class);

    private ClientCommandSender m_sender;
    private final CommandAndCallback m_cnc;
    private final long m_timeout;
    private final boolean m_isAsync;
    private final CommandResponse[] m_response;

    /**
     * Constructor for {@link ClientCommandSenderTask}. The <code>reponse</code> is an array so it can be used as an
     * "out" parameter; the response will be stored in the first element of that array. <code>response</code> may be
     * <code>null</code>, in which case it will be ignored and the response will not be returned. The <code>async</code>
     * flag must be <code>true</code> if the task is running asynchronously from the caller that submitted the command.
     * This flag must be <code>false</code> if this task's {@link #run()} method is called outside of any thread pool
     * and is synchronous with the caller that submitted the command.
     *
     * <p>Under current use-case scenarios, <code>response</code> will usually be non-<code>null</code> iff <code>
     * async</code> is <code>false</code>. Rather than assume that will always be the case, that is not enforced - in
     * other words, an asynchronously executed task can be told to store its response.</p>
     *
     * @param sender   the object to use to actually send the command
     * @param cnc      the command to send and an optional callback object to notify when we are done
     * @param timeout  the amount of time to wait for the action to complete before aborting - if less than 1, will not
     *                 timeout
     * @param async    <code>true</code> if this task is executed in a thread from a thread pool; <code>false</code> if
     *                 synchronously executed by the caller that submitted the command
     * @param response the response that was received after the command was processed (may be <code>null</code>)
     */
    public ClientCommandSenderTask(ClientCommandSender sender, CommandAndCallback cnc, long timeout, boolean async,
        CommandResponse[] response) {
        m_sender = sender;
        m_cnc = cnc;
        m_timeout = timeout;
        m_isAsync = async;
        m_response = response;

        return;
    }

    /**
     * Performs the sending of the command to the server.
     *
     * @see Callable#call()
     */
    public CommandResponse call() throws Exception {
        CommandResponse response;

        try {
            boolean retry;
            do {
                retry = false;

                response = send(m_sender, m_cnc);

                Throwable exception = response.getException();
                if ((exception != null) && (exception instanceof NotPermittedException)) {
                    long pause = ((NotPermittedException) exception).getSleepBeforeRetry();
                    LOG.warn(CommI18NResourceKeys.COMMAND_NOT_PERMITTED, m_cnc.getCommand(), pause);
                    retry = true;
                    Thread.sleep(pause);
                }
            } while (retry);
        } catch (Exception e) {
            throw e;
        } catch (Throwable t) {
            // jboss/remoting can throw throwables, but Callable only allows for Exceptions to be thrown
            throw new Exception(t);
        }

        return response;
    }

    /**
     * Performs the sending of the command and waits for it to complete or timeout. Upon completion, the
     * {@link #getCommandAndCallback() callback} will be notified of the response unless the command is to be retried
     * due to the command having its guaranteed delivery flag enabled.
     *
     * @see java.lang.Runnable#run()
     */
    public void run() {
        CommandResponse response;
        Command command = m_cnc.getCommand();
        boolean notify_callback = (m_cnc.getCallback() != null); // only notify the callback if we actually have one

        try {
            m_sender.waitForSendThrottle(command);

            if (m_timeout > 0) {
                // this may need to spawn another thread and effect overall performance
                // if the timer thread pool is null, the sender is shutdown, so immediately abort
                ThreadPoolExecutor timerThreadPool = m_sender.getTimerThreadPool();
                if (timerThreadPool == null) {
                    throw new InterruptedException();
                }

                Future<CommandResponse> futureTask = timerThreadPool.submit((Callable<CommandResponse>) this);
                try {
                    response = futureTask.get(m_timeout, TimeUnit.MILLISECONDS);
                } catch (ExecutionException ee) {
                    throw ee.getCause();
                } catch (TimeoutException te) {
                    // our timeout has expired, cancel the command and abort
                    futureTask.cancel(true);
                    throw te;
                } catch (InterruptedException ie) {
                    // waiting for the future was interrupted, the sender executor thread pool is probably shutting down
                    futureTask.cancel(true);
                    throw ie;
                }
            } else {
                // we won't timeout - let the thread take as long as it needs - no need to spawn another thread
                response = call();
            }
        } catch (Throwable t) {
            // See if the failing command was a ping and th exception was a CanNotConnectException
            boolean isPing = false;
            if (command instanceof RemotePojoInvocationCommand) {
                RemotePojoInvocationCommand rp = (RemotePojoInvocationCommand) command;
                if (rp.getTargetInterfaceName().endsWith("Ping")) {
                    if (t instanceof CannotConnectException) {
                        isPing = true;
                    }
                }
            }

            if (isPing) {
                String agent = m_sender.getRemoteCommunicator().toString();
                LOG.info(CommI18NResourceKeys.AGENT_PING_FAILED, agent);
            } else {
                LOG.error(t, CommI18NResourceKeys.SEND_FAILED, command, ThrowableUtil.getAllMessages(t));
            }
            response = new GenericCommandResponse(command, false, null, t);

            boolean retry = shouldCommandBeRetried(command, t);

            if (retry) {
                notify_callback = false; // since we are going to retry this command, do not notify the callback
                LOG.warn(CommI18NResourceKeys.QUEUING_FAILED_COMMAND);
                try {
                    m_sender.retryGuaranteedTask(m_cnc);
                } catch (Exception e) {
                    LOG.error(CommI18NResourceKeys.CLIENT_COMMAND_SENDER_TASK_REQUEUE_FAILED, command);
                }
            }
        }

        // if the command attempt finished (regardless of success or failure) we need to now notify our callback of the results
        if (notify_callback) {
            try {
                m_cnc.getCallback().commandSent(response);
            } catch (Throwable t) {
                LOG.warn(t, CommI18NResourceKeys.CALLBACK_FAILED, response);
            }
        }

        if (m_response != null) {
            m_response[0] = response;
        }

        return;
    }

    /**
     * Returns the command/callback pair that this task will use.
     *
     * @return command/callback pair
     */
    public CommandAndCallback getCommandAndCallback() {
        return m_cnc;
    }

    /**
     * This actually sends the command to the sender. Subclasses are free to override this if they wish to send
     * additional data.
     *
     * @param  sender the object that will send the command
     * @param  cnc    the object that contains the command to send
     *
     * @return the response from the server
     *
     * @throws Throwable if failed to send the command
     */
    protected CommandResponse send(ClientCommandSender sender, CommandAndCallback cnc) throws Throwable {
        return sender.send(cnc.getCommand());
    }

    /**
     * This allows the task to get pointed to a different endpoint by switching its sender.
     *
     * <p>This is package scoped - only the command sender object is allowed to override a task's sender.</p>
     *
     * @param sender the new sender object to use when executing this task
     */
    void setClientCommandSender(ClientCommandSender sender) {
        m_sender = sender;
    }

    /**
     * This method should be called when an excepton occurred during the sending of a command. This will determine if
     * the command should be retried or not.
     *
     * <p>If the given command is not serializable, this returns <code>false</code> always.</p>
     *
     * <p>If the given command is not async with guaranteed delivery enabled, this returns <code>false</code>
     * always.</p>
     *
     * <p>If the given command is serializable and the exception thrown indicates a failed connection to the server,
     * then this will return <code>true</code> always.</p>
     *
     * <p>If the given command is serializable and the exception does not explicitly indicate a failed connection to the
     * server, then this will return <code>true</code> unless this command was retried too many times.</p>
     *
     * @param  command   the command that failed
     * @param  throwable the exception that occurred; this is the cause of the failure
     *
     * @return <code>true</code> if the command should be retried; <code>false</code> if the command should abort
     */
    private boolean shouldCommandBeRetried(Command command, Throwable throwable) {
        // we only need to resend this command if it asked for its delivery to be guaranteed (we only guarantee async commands)
        if (m_isAsync && m_sender.isDeliveryGuaranteed(command)) {
            try {
                final String RETRY_CONFIG_PROP = "rhq.retry";
                Properties config = command.getConfiguration();
                int retryCount = Integer.parseInt(config.getProperty(RETRY_CONFIG_PROP, "0"));

                // throw exception if not serializable; no need to test if we already retried before
                if (retryCount == 0) {
                    StreamUtil.serialize(command);
                }

                // increment the retry count in the command config
                retryCount++;
                config.put(RETRY_CONFIG_PROP, String.valueOf(retryCount));

                return (isCannotConnectException(throwable) || (retryCount <= m_sender.getConfiguration().maxRetries));
            } catch (Exception e) {
                return false; // don't retry if not serializable or the retry number is munged in the command
            }
        }

        return false;
    }

    /**
     * Returns <code>true</code> if the given throwable was caused by a connection error - that is, if the remote
     * endpoint could not be contacted. This does a best guess - hopefully, it won't catch any false positives or false
     * negative.
     *
     * @param  t the exception to check (may be <code>null</code>, in which case <code>false</code> is returned)
     *
     * @return <code>true</code> if the exception was caused by not being able to connect to the remote endpoint
     */
    private boolean isCannotConnectException(Throwable t) {
        if (t == null) {
            return false;
        }

        boolean yes = (t instanceof ConnectException) || (t instanceof CannotConnectException);

        // if this isn't it, go down the cause chain (e.g. t might be an invocation target exception)
        if (!yes) {
            yes = isCannotConnectException(t.getCause());
        }

        return yes;
    }
}