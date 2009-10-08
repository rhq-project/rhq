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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import mazz.i18n.Logger;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * Helps the {@link ClientCommandSender} determine how and when to throttle messages when sending. This object allows
 * messages to be sent up to a maximum number, at which time a quiet period begins. Once the quiet period begins, no
 * more messages are allowed to be sent until the quiet period ends.
 *
 * <p>Send throttling is different than {@link ClientCommandSender#enableQueueThrottling(long, long) queue throttling}
 * in the following respects:
 *
 * <ul>
 *   <li>Send throttling is overridable by individual commands by setting their
 *     {@link ClientCommandSender#CMDCONFIG_PROP_SEND_THROTTLE} config property (queue throttling affects all
 *     commands)</li>
 *   <li>Send throttling is applicable when sending both synchronously and asynchronously (queue throttling only affects
 *     async)</li>
 * </ul>
 * </p>
 *
 * <p>To use this object, the sender must call {@link #waitUntilOkToSend()} every time it wants to send a command. This
 * will increment a logical counter of commands sent and when it hits the maximum, that method blocks until the quiet
 * period is over.</p>
 *
 * <p>The sender can completely {@link #disableSendThrottling() disable send throttling} and
 * {@link #enableSendThrottling() re-enable it}. The sender is allowed to reconfigure the send throttling configuration
 * by calling {@link #setSendThrottleParameters(ClientCommandSenderConfiguration)} at any point; regardless of whether
 * send throttling is enabled or not and regardless of whether the quiet period is enabled. The new configuration will
 * take effect as soon as it can.</p>
 *
 * <p>This object is packaged scoped so the {@link ClientCommandSender} can use it.</p>
 *
 * @author John Mazzitelli
 */
class SendThrottle {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(SendThrottle.class);

    /**
     * This will be true when send-throttling has been enabled. This is also used for its monitor lock when accessing
     * the other send throttle data members.
     */
    private AtomicBoolean m_sendThrottleEnabled;

    /**
     * When send throttling is enabled, this runnable runs in a thread that will monitor when the quiet period starts
     * and ends. Each time throttling is enabled, a new runnable object is created; when throttling is disabled, the
     * runnable object is discarded and this variable is set to <code>null</code>.
     */
    private QuietPeriodRunnable m_quietPeriodRunnable;

    /**
     * If send throttling is enabled, this is the maximum number of commands that can be sent before the quiet period
     * must start.
     */
    private long m_sendThrottleMaxCommands;

    /**
     * If send throttling is enabled, this is the length of time (in milliseconds) of the quiet period.
     */
    private long m_sendThrottleQuietPeriodDurationMillis;

    /**
     * Constructor for {@link SendThrottle} that takes the client command sender configuration which contains this
     * {@link SendThrottle} object's configuration.
     *
     * @param config contains this object's configuration
     */
    public SendThrottle(ClientCommandSenderConfiguration config) {
        m_sendThrottleEnabled = new AtomicBoolean(false);

        setSendThrottleParameters(config);

        if (config.enableSendThrottling) {
            enableSendThrottling();
        } else {
            disableSendThrottling();
        }

        return;
    }

    /**
     * Allows the caller to modify this throttler's configuration. If send throttling is already enabled, these will
     * take effect as soon as possible (though not necessarily immediately; for example, if this object is already in
     * its quiet period, the old quiet period duration remains in effect until that quiet period ends).
     *
     * @param config the object that contains this object's new configuration
     */
    public void setSendThrottleParameters(ClientCommandSenderConfiguration config) {
        synchronized (m_sendThrottleEnabled) {
            m_sendThrottleMaxCommands = config.sendThrottleMaxCommands;
            m_sendThrottleQuietPeriodDurationMillis = config.sendThrottleQuietPeriodDurationMillis;

            LOG.debug(CommI18NResourceKeys.SEND_THROTTLE_CONFIGURED, m_sendThrottleMaxCommands,
                m_sendThrottleQuietPeriodDurationMillis);
        }

        return;
    }

    /**
     * Used to determine if send throttling is currently enabled.
     *
     * @return <code>true</code> if send throttling is enabled; <code>false</code> if disabled
     */
    public boolean isSendThrottlingEnabled() {
        synchronized (m_sendThrottleEnabled) {
            return m_sendThrottleEnabled.get();
        }
    }

    /**
     * Enables send throttling so as to allow commands to be sent with quiet periods in between. If send throttling is
     * already enabled, this method does nothing.
     */
    public void enableSendThrottling() {
        synchronized (m_sendThrottleEnabled) {
            if (m_sendThrottleEnabled.get()) {
                return; // already enabled, do nothing and return immediately
            }

            m_sendThrottleEnabled.set(true);

            m_quietPeriodRunnable = new QuietPeriodRunnable();
            Thread thread = new Thread(m_quietPeriodRunnable, "RHQ Send Throttle Quiet Period Thread");
            thread.setDaemon(true);
            thread.start();

            LOG.debug(CommI18NResourceKeys.SEND_THROTTLE_ENABLED, m_sendThrottleMaxCommands,
                m_sendThrottleQuietPeriodDurationMillis);
        }

        return;
    }

    /**
     * Disables the send throttling such that the sender can send commands as fast as possible with no quiet periods.
     */
    public void disableSendThrottling() {
        synchronized (m_sendThrottleEnabled) {
            m_sendThrottleEnabled.set(false);

            if (m_quietPeriodRunnable != null) {
                m_quietPeriodRunnable.killThread();
                m_quietPeriodRunnable = null;
            }
        }

        LOG.debug(CommI18NResourceKeys.SEND_THROTTLE_DISABLED);

        return;
    }

    /**
     * This method should be called whenever the owning {@link ClientCommandSender} object wants to send a command. This
     * method will check to see if this throttler is currently in its quiet period and if it is, this method will block
     * until the quiet period expires.
     *
     * <p>If send throttling is {@link #disableSendThrottling() disabled}, this method never blocks - it returns
     * immediately.</p>
     */
    public void waitUntilOkToSend() {
        QuietPeriodRunnable quiet_period_runnable;

        synchronized (m_sendThrottleEnabled) {
            if (!m_sendThrottleEnabled.get()) {
                return;
            }

            quiet_period_runnable = m_quietPeriodRunnable; // since we are enabled, this will always be non-null
        }

        // we do this via a local variable because we can't wait inside the sync block so as to avoid deadlocks
        quiet_period_runnable.waitUntilOkToSend();

        return;
    }

    /**
     * This is the runnable that will run in a thread when send throttling is enabled. The thread will wait for the
     * quiet period to end and performs notifications when it does end.
     */
    private class QuietPeriodRunnable implements Runnable {
        /**
         * This is the queue where all threads waiting for the OK will place their latches. After the quiet period ends,
         * latches will be pulled from the queue and opened up (the amount of latches opened up depend on the number of
         * maximum commands that can be sent before another quiet period starts). When this runnable's thread dies, this
         * latch queue will be set to <code>null</code>.
         */
        private LinkedBlockingQueue<CountDownLatch> m_latchQueue = new LinkedBlockingQueue<CountDownLatch>();

        /**
         * This object serves a dual-purpose - it is used to both block during a quiet period and to determine if the
         * thread where this runnable is running should exit. The slot will get a Boolean.TRUE placed in it when send
         * throttling has been disabled and this thread should die. The dual use allows {@link #killThread()} to
         * immediately wake up the thread while in the quiet period so it can die quickly.
         */
        private ArrayBlockingQueue<Boolean> m_quietPeriodLock = new ArrayBlockingQueue<Boolean>(1);

        /**
         * This will block until it is OK for the caller to send its command. This happens after the quiet period ends
         * (if currently in a quiet period). If the throttler is not currently in a quiet period, this returns fairly
         * quickly.
         */
        public void waitUntilOkToSend() {
            CountDownLatch latch;

            // we need to synchronize here because we don't want to sneek a latch onto the queue during the thread's shutdown
            synchronized (this) {
                // if this condition is true, it means we are either in the process or have already killed the thread
                // this means send throttling has been disabled so there should be no waiting - all commands can be sent immediately
                if ((m_quietPeriodLock.peek() != null) || (m_latchQueue == null)) {
                    return;
                }

                try {
                    // put a latch on the queue; our thread will release the latch when its time
                    latch = new CountDownLatch(1);
                    m_latchQueue.put(latch);
                } catch (InterruptedException e) {
                    // unsure when this condition would ever occur; maybe never - but just in case, let's immediately give the OK to send
                    return;
                }
            }

            // notice that we are no longer synchronized - we don't want to block on this while we wait for the latch to open up
            // at this point we are guaranteed the thread will open our latch either during normal processing or during shutdown
            try {
                latch.await();
            } catch (InterruptedException e) {
                // unsure when this condition would ever occur; maybe never - but just in case, let's immediately give the OK to send
            }

            return;
        }

        /**
         * This immediately exits the quiet period (if currently in the quiet period) and ensures the thread this
         * runnable is running in dies. All threads waiting to send will be allowed to do so immediately.
         *
         * @throws RuntimeException if failed to notify the thread that it should die
         */
        public void killThread() {
            try {
                // synchronize here to avoid two threads calling killThread at the same time and screwing things up
                synchronized (this) {
                    // if this condition is false, it means we are either in the process or have already killed the thread
                    if ((m_quietPeriodLock.peek() == null) && (m_latchQueue != null)) {
                        // put an object in the slot to signify that the thread should die
                        m_quietPeriodLock.put(Boolean.TRUE);

                        // put a dummy latch on the queue to wake up the thread if its currently blocked trying to take from the queue
                        m_latchQueue.put(new CountDownLatch(1));
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(LOG.getMsgString(CommI18NResourceKeys.SEND_THROTTLE_CANNOT_KILL), e);
            }

            return;
        }

        /**
         * This thread runs when send throttling is enabled. It will sleep until a quiet period is started. At that
         * point, it waits for the quiet period duration and when that period ends, will notify all those threads
         * waiting for the quiet period to end so they can begin sending again.
         */
        public void run() {
            try {
                long max_commands;
                long quiet_period_millis = 1000L;
                boolean keep_going = true;

                while (keep_going) {
                    long num_commands = 0L; // the number of commands we've allowed this cycle

                    do {
                        // let's get the next command in the queue waiting to send and open its latch to let it go
                        CountDownLatch latch = m_latchQueue.take();
                        latch.countDown();

                        // let's see if we were told to exit - if we were, break out of our loop
                        if (m_quietPeriodLock.peek() != null) {
                            break;
                        }

                        num_commands++;

                        // we might have been reconfigured while we were sleeping in take(), let's get the values again
                        synchronized (m_sendThrottleEnabled) {
                            max_commands = m_sendThrottleMaxCommands;
                            quiet_period_millis = m_sendThrottleQuietPeriodDurationMillis;
                        }
                    } while (num_commands < max_commands);

                    // going into our quiet period - if something is in the slot, that means our thread was told to exit
                    keep_going = m_quietPeriodLock.poll(quiet_period_millis, TimeUnit.MILLISECONDS) == null;
                }
            } catch (InterruptedException e) {
                // kill the thread; unsure when this would ever occur; maybe never - but if it happens, the thread should probably die
            } finally {
                // before we exit, let's make sure no one is still waiting - flush the queue and give everyone the go ahead to send
                // we need to synchronize here so no one sneeks in and adds a latch to the queue after we think we are finished flushing it
                synchronized (this) {
                    try {
                        while (!m_latchQueue.isEmpty()) {
                            CountDownLatch latch = m_latchQueue.take();
                            latch.countDown();
                        }
                    } catch (InterruptedException e) {
                    } finally {
                        m_latchQueue = null;
                    }
                }
            }

            // thread is dying - send throttling is now disabled so all commands will be allowed to be sent immediately now
            return;
        }
    }
}