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

import mazz.i18n.Logger;

import org.rhq.core.domain.cluster.composite.FailoverListComposite;
import org.rhq.core.domain.cluster.composite.FailoverListComposite.ServerEntry;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.RemoteCommunicator;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommand;
import org.rhq.enterprise.communications.util.NotProcessedException;
import org.rhq.enterprise.communications.util.SecurityUtil;

/**
 * This thread's job is to periodically try to get the agent to point back to
 * its primary server, if it isn't pointing to that server already.
 * 
 * The "primary server" is the server found at the top of the agent's failover list.
 * If the agent is already talking to this server, or if the agent does not yet have
 * a failover list, nothing needs to be done.
 * 
 * If the agent is talking to another server, this thread will probe the primary server
 * and if it can, this thread will switch the agent's sender back to point to the primary.
 * 
 * If the agent is not in sending mode, this thread will not do anything until it is.  The agent
 * will decide what server it should talk to in that case.  This thread is only here
 * to prevent an agent talking to a non-primary server for a long time when the primary
 * server is available.
 * 
 * @author John Mazzitelli
 */
public class PrimaryServerSwitchoverThread extends Thread {

    private static final Logger LOG = AgentI18NFactory.getLogger(AgentMain.class);

    private final AgentMain agent;

    /**
     * The amount of time in milliseconds that this thread will sleep in between polling the server.
     */
    private long interval = 1000L * 60 * 60; // 1 hour

    /**
     * Will be <code>true</code> when this thread is told to stop polling. Note that this does not necessarily mean the
     * thread is stopped, it just means this thread was told to stop.
     */
    private boolean toldToStop = false;

    public PrimaryServerSwitchoverThread(AgentMain agent) {
        super("RHQ Primary Server Switchover Thread");
        setDaemon(true);
        this.agent = agent;
    }

    @Override
    public void run() {
        LOG.debug(AgentI18NResourceKeys.PRIMARY_SERVER_SWITCHOVER_THREAD_STARTED);

        while (!toldToStop) {
            try {
                // Note that if the agent is not sending or the failover list doesn't have any servers,
                // then we skip this time and wait some more.
                // However, it the agent is sending and we have a failover list, then we need to check
                // to see if the server we are currently talking to is the same as primary server, listed
                // at the top of the failover list. If not the same, we ask the agent to switch to that server.
                ClientCommandSender sender = this.agent.getClientCommandSender();
                if (sender.isSending()) {
                    FailoverListComposite failoverList = this.agent.downloadServerFailoverList(); // ask the server for a new one

                    // if the failover list doesn't have any servers, skip our poll and wait some more
                    if (failoverList.size() > 0) {
                        AgentConfiguration config = this.agent.getConfiguration();
                        String transport = config.getServerTransport();
                        String transportParams = config.getServerTransportParams();
                        String currentServerAddress = config.getServerBindAddress();
                        int currentServerPort = config.getServerBindPort();

                        ServerEntry primary = failoverList.get(0); // get the top of the list, aka primary server
                        String primaryAddress = primary.address;
                        int primaryPort = (SecurityUtil.isTransportSecure(transport)) ? primary.securePort
                            : primary.port;

                        if (!primaryAddress.equals(currentServerAddress) || primaryPort != currentServerPort) {
                            LOG.debug(AgentI18NResourceKeys.NOT_TALKING_TO_PRIMARY_SERVER, primaryAddress, primaryPort,
                                currentServerAddress, currentServerPort);
                            // create our own comm so we ping in an isolated client - don't reuse the sender's comm for this
                            RemoteCommunicator comm = this.agent.createServerRemoteCommunicator(transport,
                                primaryAddress, primaryPort, transportParams);
                            if (ping(comm)) {
                                LOG.info(AgentI18NResourceKeys.PRIMARY_SERVER_UP, primaryAddress, primaryPort);
                                failoverList.resetIndex(); // so the failover method call starts at the top
                                this.agent.failoverToNewServer(sender.getRemoteCommunicator()); // note that we make sure we pass in the sender's comm
                            } else {
                                LOG.debug(AgentI18NResourceKeys.PRIMARY_SERVER_STILL_DOWN, primaryAddress, primaryPort);
                            }
                        }
                    }
                }

                // to do sleep until its time to check again
                synchronized (this) {
                    wait(interval);
                }
            } catch (InterruptedException ie) {
                toldToStop = true;
            } catch (Exception e) {
                LOG.warn(e, AgentI18NResourceKeys.PRIMARY_SERVER_SWITCHOVER_EXCEPTION, e);
            }
        }

        LOG.debug(AgentI18NResourceKeys.PRIMARY_SERVER_SWITCHOVER_THREAD_STOPPED);
        return;
    }

    /**
     * Sets the time (in milliseconds) that this thread sleeps between checks.
     * 
     * @param interval sleep time, in milliseconds (must not be less than 1000)
     */
    public void setInterval(long interval) {
        this.interval = interval;
    }

    /**
     * Call this method when you want to stop this thread, which effectively stops it from
     * checking that the agent is pointing to its primary server.
     */
    public void stopChecking() {
        toldToStop = true;
    }

    /**
     * Forces this thread to check now and switch to the primary if needed.  If the thread
     * is already checking, this method does nothing. Effectively, this method wakes up
     * this thread if its sleeping during the {@link #setInterval(long) sleep interval}.
     */
    public void checkNow() {
        synchronized (this) {
            notify();
        }
    }

    /**
     * Given the remote communicator (which isn't the one in the agent's command sender), this sends a ping
     * request to the remote endpoint and returns <code>true</code> if the remote endpoint is up.
     * 
     * @param comm the communicator used to send the message
     * 
     * @return <code>true</code> if the communicator can send the message; <code>false</code> if the remote endpoint is down
     * 
     * @throws Throwable
     */
    private boolean ping(RemoteCommunicator comm) {
        boolean ok = true; // assume we can ping; on error, we'll set this to false
        IdentifyCommand id_cmd = new IdentifyCommand();
        this.agent.getClientCommandSender().preprocessCommand(id_cmd);
        try {
            CommandResponse response = comm.sendWithoutFailureCallback(id_cmd);
            // there is a special case when we might get a response back but it should be considered "server down".
            // that is: when the server replies with a NotProcessedException response
            if (response.getException() instanceof NotProcessedException) {
                ok = false;
            }
        } catch (Throwable e) {
            ok = false;
        }
        return ok;
    }
}
