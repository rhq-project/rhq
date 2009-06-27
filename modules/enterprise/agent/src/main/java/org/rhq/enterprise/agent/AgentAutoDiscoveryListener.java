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

import java.net.MalformedURLException;

import mazz.i18n.Logger;

import org.jboss.remoting.InvokerLocator;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.client.RemoteCommunicator;
import org.rhq.enterprise.communications.command.impl.generic.GenericCommandClient;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommand;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommandResponse;
import org.rhq.enterprise.communications.command.server.discovery.AutoDiscoveryListener;

/**
 * This is the agent's listener that will get notified when new RHQ Servers come on and offline.
 *
 * <p>Because a remoting server's invoker locator representation may differ slightly on the client-side, this listener
 * performs some additional work to try to determine what the RHQ Server calls itself (as opposed to what the agent
 * calls the server). Otherwise, we may get a notification about the RHQ Server and not realize it because we will be
 * looking for one invoker locator representation when the notification will have a slightly different one (this
 * illustrates a slight hole in the InvokerLocator.equals() implementation). If we cannot determine what the RHQ
 * Server's true locator is, then when we compare the locator in the notification with the one we are looking for, they
 * may look like different locators when they are actually referring to the same server endpoint.</p>
 *
 * @author John Mazzitelli
 */
public class AgentAutoDiscoveryListener implements AutoDiscoveryListener {
    /**
     * Logger
     */
    private static final Logger LOG = AgentI18NFactory.getLogger(AgentAutoDiscoveryListener.class);

    /**
     * The agent that created this object. This agent has the sender object that this listener will start and stop when
     * appropriate.
     */
    private final AgentMain m_agent;

    /**
     * A communicator we can use to send commands directly to the server
     */
    private RemoteCommunicator m_remoteCommunicator;

    /**
     * If known, this will be the locator that the server advertises itself as. If this is not yet known, this will be
     * <code>null</code>.
     */
    private InvokerLocator m_serverToBeListenedFor;

    /**
     * This is simply a flag to eliminate a flood of log messages that would get dumped each time the server failed to
     * be communicated with. We want to warn once in the case the failure is due to a misconfiguration (the log message
     * should help diagnose the misconfiguration), but we don't want to continually log warnings in the normal case when
     * the server just isn't up yet.
     */
    private boolean m_warnedAboutConnectionFailure;

    /**
     * Constructor for {@link AgentAutoDiscoveryListener} that is given the agent that created this listener.
     *
     * @param agent
     * @param communicator a communicator that we can use to send commands directly to the server that we are listening
     *                     for
     */
    public AgentAutoDiscoveryListener(AgentMain agent, RemoteCommunicator communicator) {
        m_agent = agent;
        m_remoteCommunicator = communicator;
        m_serverToBeListenedFor = null;
        m_warnedAboutConnectionFailure = false;
    }

    /**
     * If the auto-detected endpoint is the RHQ Server we are looking for, enable the agent to start sending messages to
     * it.
     *
     * <p>If this listener does not yet know what the RHQ Server calls itself, then we assume the new remote server
     * coming online is our server and so we attempt to directly send our server an {@link IdentifyCommand} to ask it
     * for its true invoker locator representation. If our server is online, then the identify command's response will
     * contain the server's true invoker locator and we will use it rather than the locator as configured in the
     * agent.</p>
     *
     * @see AutoDiscoveryListener#serverOnline(InvokerLocator)
     */
    public void serverOnline(InvokerLocator locator) {
        // if we do not yet know the exact invoker locator that our server calls itself, let's try to send
        // an identify command to our server via its remote communicator and ask it for its locator
        if (m_serverToBeListenedFor == null) {
            m_serverToBeListenedFor = attemptToIdentifyServer();
        }

        if (isServerToBeListenedFor(locator)) {
            LOG.info(AgentI18NResourceKeys.SERVER_ONLINE, locator);

            m_agent.getClientCommandSender().startSending();
        }

        return;
    }

    /**
     * If the auto-detected endpoint is the server we are looking for, this tells the agent to stop sending messages.
     *
     * @see AutoDiscoveryListener#serverOffline(InvokerLocator)
     */
    public void serverOffline(InvokerLocator locator) {
        if (isServerToBeListenedFor(locator)) {
            LOG.info(AgentI18NResourceKeys.SERVER_OFFLINE, locator);

            // stop sending commands - there is no sense processing the commands currently in the queue since they will fail anyway
            m_agent.getClientCommandSender().stopSending(false);
        }

        return;
    }

    /**
     * This will attempt to identify the server's true invoker locator object by sending it a direct message. If it gets
     * a response, it will return the server's invoker locator. If, for any reason, this method fails to identify the
     * server, no exception is thrown and <code>null</code> is returned.
     *
     * @return server's invoker locator or <code>null</code> if server could not be identified
     */
    private InvokerLocator attemptToIdentifyServer() {
        try {
            GenericCommandClient client = new GenericCommandClient(m_remoteCommunicator);
            CommandResponse genericResponse = client.invoke(new IdentifyCommand());
            IdentifyCommandResponse identifyResponse = new IdentifyCommandResponse(genericResponse);

            if (identifyResponse.getException() != null) {
                throw identifyResponse.getException();
            }

            return new InvokerLocator(identifyResponse.getIdentification().getInvokerLocator());
        } catch (Throwable ignore) {
            // This probably just means that the server isn't online yet; we can ignore - we'll be called later for another attempt.
            // However, we want to log a warning at least once in case this is a configuration error (in which case
            // the connection will never succeed - without this log message, it will be hard to debug the misconfiguration).

            if (!m_warnedAboutConnectionFailure) {
                m_warnedAboutConnectionFailure = true;

                LOG.debug(AgentI18NResourceKeys.SERVER_ID_FAILURE, ThrowableUtil.getAllMessages(ignore));
            }
        }

        return null;
    }

    /**
     * Compares the given invoker locator with the RHQ Server locator that this object is listening for. If its the
     * same, then <code>true</code> is returned; if they are not the same, <code>false</code> is returned.
     *
     * @param  compare_me the locator to compare with the locator of the RHQ Server this object is listening for
     *
     * @return <code>true</code> if the given invoker locator represents the same server endpoint that this object is
     *         listening for
     */
    private boolean isServerToBeListenedFor(InvokerLocator compare_me) {
        InvokerLocator server_locator = getServerToBeListenedFor();

        // we just care about host and port - assume transport and transport params may be different between client and server
        return server_locator.getHost().equals(compare_me.getHost())
            && server_locator.getPort() == compare_me.getPort();
    }

    /**
     * This returns the endpoint locator of the RHQ Server that our listener is listening for.
     *
     * @return the endpoint locator of the server we are listening for
     *
     * @throws RuntimeException if the configured locator URI is malformed
     */
    private InvokerLocator getServerToBeListenedFor() {
        // if we've already established the server's actual invoker locator, then return it immediately
        if (m_serverToBeListenedFor != null) {
            return m_serverToBeListenedFor;
        }

        // we do not yet know what our server's true locator is, let's just use the locator as configured in the agent
        String locator_uri = m_agent.getConfiguration().getServerLocatorUri();
        InvokerLocator locator;

        try {
            locator = new InvokerLocator(locator_uri);
        } catch (MalformedURLException e) {
            // this should never happen
            throw new RuntimeException(LOG.getMsgString(AgentI18NResourceKeys.INVALID_LOCATOR_URI), e);
        }

        return locator;
    }
}