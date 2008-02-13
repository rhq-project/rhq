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

import mazz.i18n.Logger;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommand;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * An object that runs in a thread whose sole job is to poll the given remote server. When the server's status changes
 * (that is, goes down or comes back up), the client sender that owns this thread will be told to stop or start sending
 * as appropriate.
 *
 * @author John Mazzitelli
 */
class ServerPollingThread extends Thread {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(ServerPollingThread.class);

    /**
     * The client sender that asked us to poll the server.
     */
    private final ClientCommandSender m_clientSender;

    /**
     * The amount of time in milliseconds that this thread will sleep in between polling the server.
     */
    private final long m_interval;

    /**
     * Will be <code>true</code> when this thread is told to stop polling. Note that this does not necessarily mean the
     * thread is stopped, it just means this thread was told to stop. See {@link #m_stopped}.
     */
    private boolean m_stop;

    /**
     * Will be <code>true</code> when this thread is stopped.
     */
    private boolean m_stopped;

    /**
     * This is simply a flag to eliminate a flood of log messages that would get dumped each time the server failed to
     * be communicated with. We want to warn once in the case the failure is due to a misconfiguration (the log message
     * should help diagnose the misconfiguration), but we don't want to continually log warnings in the normal case when
     * the server just isn't up yet.
     */
    private boolean m_warnedAboutConnectionFailure;

    /**
     * Constructor for {@link ServerPollingThread} making this thread a daemon thread.
     *
     * @param client           the client sender on whose behalf we are polling
     * @param polling_interval the amount of time in milliseconds that the thread will pause in between polls
     */
    public ServerPollingThread(ClientCommandSender client, long polling_interval) {
        super("RHQ Server Polling Thread");
        setDaemon(true);

        m_clientSender = client;
        m_interval = polling_interval;
        m_stop = false;
        m_stopped = true;
        m_warnedAboutConnectionFailure = false;

        return;
    }

    /**
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        m_stopped = false;

        LOG.debug(CommI18NResourceKeys.SERVER_POLLING_THREAD_STARTED, m_interval);

        while (!m_stop) {
            try {
                try {
                    // Send a small, simple identify command to the server and if it succeeds, tell the client
                    // sender that it is OK to start sending messages, if it is not already sending
                    IdentifyCommand id_cmd = new IdentifyCommand();
                    m_clientSender.preprocessCommand(id_cmd);
                    m_clientSender.send(id_cmd);

                    if (m_clientSender.startSending()) {
                        LOG.info(CommI18NResourceKeys.SERVER_POLLING_THREAD_SERVER_ONLINE);
                        m_warnedAboutConnectionFailure = false; // if we detect the server is down again, lets log the exception again
                    }
                } catch (Throwable e) {
                    // This probably just means that the server isn't online yet
                    // However, we want to log a warning at least once in case this is a configuration error (in which case
                    // the connection will never succeed - without this log message, it will be hard to debug the misconfiguration).
                    if (!m_warnedAboutConnectionFailure) {
                        m_warnedAboutConnectionFailure = true;
                        LOG.debug(CommI18NResourceKeys.SERVER_POLL_FAILURE, ThrowableUtil.getAllMessages(e));
                    }

                    // Failed to send the command for some reason, make sure the client sender isn't trying to send the server messages.
                    // Since the server is down, no sense processing commands currently in the queue.
                    if (m_clientSender.stopSending(false)) {
                        LOG.warn(CommI18NResourceKeys.SERVER_POLLING_THREAD_SERVER_OFFLINE);
                    }
                }

                synchronized (this) {
                    wait(m_interval); // go to sleep before we poll again
                }
            } catch (InterruptedException e) {
                m_stop = true;
            }
        }

        LOG.debug(CommI18NResourceKeys.SERVER_POLLING_THREAD_STOPPED);
        m_stopped = true;

        return;
    }

    /**
     * Tells this thread to stop polling. This will block and wait for the thread to die.
     */
    public void stopPolling() {
        m_stop = true;

        // tell the thread that we flipped the stop flag in case it is waiting in a sleep interval
        synchronized (this) {
            while (!m_stopped) {
                try {
                    notify();
                    wait(5000L);
                } catch (InterruptedException e) {
                }
            }
        }

        return;
    }
}