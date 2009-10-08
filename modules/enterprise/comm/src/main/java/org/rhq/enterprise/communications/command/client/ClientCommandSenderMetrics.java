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

import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This is a simple object that encapsulates all the different metrics collected by a single
 * {@link ClientCommandSender sender}.
 *
 * <p>This class is thread-safe.</p>
 *
 * @author John Mazzitelli
 */
public class ClientCommandSenderMetrics {
    private final CommandQueue queue;
    private final PersistentFifo commandStore;
    private ThreadPoolExecutor threadPool;

    // these member variables are package-protected to allow the sender to directly set their values
    AtomicBoolean sendingMode = new AtomicBoolean(false);
    AtomicLong queueThrottleMaxCommands = new AtomicLong(0L);
    AtomicLong queueThrottleBurstPeriodMillis = new AtomicLong(0L);
    AtomicLong sendThrottleMaxCommands = new AtomicLong(0L);
    AtomicLong sendThrottleQuietPeriodDurationMillis = new AtomicLong(0L);
    AtomicLong successfulCommands = new AtomicLong(0L);
    AtomicLong failedCommands = new AtomicLong(0L);
    AtomicLong averageExecutionTime = new AtomicLong(0L);

    /**
     * Creates a new {@link ClientCommandSenderMetrics} object given the queue and store used by the sender object whose
     * metrics this object contains.
     *
     * @param queue        the sender's queue (may be <code>null</code>)
     * @param commandStore the place where guaranteed commands are persisted (may be <code>null</code>)
     * @param threadPool   contains the threads that execute the queued tasks (may be <code>null</code>)
     */
    public ClientCommandSenderMetrics(CommandQueue queue, PersistentFifo commandStore, ThreadPoolExecutor threadPool) {
        this.queue = queue; // if null, just consider it always empty
        this.commandStore = commandStore; // if null, just consider its size to always be 0
        setThreadPool(threadPool); // if null, just assume everything about it is 0
    }

    /**
     * Constructor for {@link ClientCommandSenderMetrics} that represents a "dummy" sender. Both the queue and command
     * store are assumed <code>null</code> so their sizes will always be assumed 0. This constructor is used to simulate
     * metrics for a sender that has been shutdown.
     */
    public ClientCommandSenderMetrics() {
        this(null, null, null);
    }

    /**
     * Sets the thread pool whose metrics are to be collected by this object.
     * Package-scoped because only the sender object is able to set this.
     * @param threadPool the new thread pool whose metrics are to be collected (may be <code>null</code>)
     */
    void setThreadPool(ThreadPoolExecutor threadPool) {
        this.threadPool = threadPool;
    }

    /**
     * Returns the number of commands that are currently being processed.
     * @return commands that are actively in progress
     */
    public long getNumberCommandsActive() {
        long num = 0L;
        if (threadPool != null) {
            num = threadPool.getActiveCount();
        }
        return num;
    }

    /**
     * Returns the number of commands that were successfully sent.
     *
     * @return number of commands sent
     */
    public long getNumberSuccessfulCommandsSent() {
        return successfulCommands.get();
    }

    /**
     * Returns the number of commands that failed to be sent successfully. This counts those commands that failed to
     * reach the server and those that did reach the server but were not successfully processed by the server.
     *
     * @return number of failed commands
     */
    public long getNumberFailedCommandsSent() {
        return failedCommands.get();
    }

    /**
     * Returns the amount of time (in milliseconds) that it took to send all commands that were ultimately
     * {@link #getNumberSuccessfulCommandsSent() successful}.
     *
     * @return average time the successful commands took to complete
     */
    public long getAverageExecutionTimeSent() {
        return averageExecutionTime.get();
    }

    /**
     * Returns the approximate number of commands that are currently in the queue waiting to be sent.
     *
     * @return number of commands queued
     */
    public long getNumberCommandsInQueue() {
        return (queue != null) ? queue.size() : 0L;
    }

    /**
     * Returns the number of commands that are currently spooled to disk (which tells you the number of guaranteed
     * commands that failed to be sent and are waiting to be retried.
     *
     * @return number of persisted commands
     *
     * @see    PersistentFifo
     */
    public long getNumberCommandsSpooled() {
        long num = 0L;

        if (commandStore != null) {
            try {
                num = commandStore.count();
            } catch (IOException e) {
            }
        }

        return num;
    }

    /**
     * Indicates if the sender object is currently in "sending" mode. If <code>true</code>, this means the sender thinks
     * the server on the other end is alive and is able to send messages to it.
     *
     * @return sending mode flag
     */
    public boolean isSending() {
        return sendingMode.get();
    }

    /**
     * See {@link ClientCommandSender#enableQueueThrottling(long, long)}.
     *
     * @return maximum commands allowed by the queue throttle (0 if queue throttling is disabled)
     */
    public long getQueueThrottleMaxCommands() {
        return queueThrottleMaxCommands.get();
    }

    /**
     * See {@link ClientCommandSender#enableQueueThrottling(long, long)}.
     *
     * @return queue throttle burst period, in milliseconds (0 if queue throttling is disabled)
     */
    public long getQueueThrottleBurstPeriodMillis() {
        return queueThrottleBurstPeriodMillis.get();
    }

    /**
     * See {@link ClientCommandSender#enableSendThrottling(long, long)}.
     *
     * @return maximum commands allowed by the send throttle (0 if send throttling is disabled)
     */
    public long getSendThrottleMaxCommands() {
        return sendThrottleMaxCommands.get();
    }

    /**
     * See {@link ClientCommandSender#enableSendThrottling(long, long)}.
     *
     * @return send throttle quiet period duration, in milliseconds (0 if send throttling is disabled)
     */
    public long getSendThrottleQuietPeriodDurationMillis() {
        return sendThrottleQuietPeriodDurationMillis.get();
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("Sender Metrics: [");

        buf.append("is-sending=" + isSending());
        buf.append(",num-successful-commands-sent=" + getNumberSuccessfulCommandsSent());
        buf.append(",num-failed-commands-sent=" + getNumberFailedCommandsSent());
        buf.append(",avg-execution-time=" + getAverageExecutionTimeSent());
        buf.append(",num-commands-in-queue=" + getNumberCommandsInQueue());
        buf.append(",num-commands-spooled=" + getNumberCommandsSpooled());
        buf.append(",queue-throttle-max-commands=" + getQueueThrottleMaxCommands());
        buf.append(",queue-throttle-burst-period=" + getQueueThrottleBurstPeriodMillis());
        buf.append(",send-throttle-max-commands=" + getSendThrottleMaxCommands());
        buf.append(",send-throttle-max-commands=" + getSendThrottleMaxCommands());
        buf.append(']');

        return buf.toString();
    }
}