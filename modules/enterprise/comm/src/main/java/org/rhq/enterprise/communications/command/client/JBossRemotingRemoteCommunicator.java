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

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvoker;

import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.impl.generic.GenericCommandResponse;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * Provides basic functionality to all command clients that want to use JBoss/Remoting as the remoting framework.
 *
 * <p>This superclass provides the hooks by which users of the client can select the
 * {@link #setInvokerLocator(String) location of the remote server} and the {@link #setSubsystem(String) subsystem}
 * where the command is to be invoked.</p>
 *
 * <p>Under the covers, a {@link org.jboss.remoting.Client remoting client} is created and maintained by this object.
 * The users of this object may manually {@link #connect() connect} and {@link #disconnect() disconnect} that remoting
 * client. Typically, there will not be a need to connect since it will be done automatically when appropriate; however,
 * it is good practice to tell this object to disconnect its remoting client when this object is no longer needed to
 * issue commands to the remote server.</p>
 *
 * <p>All subclasses should include a no-arg constructor so they can be built dynamically by the cmdline client.</p>
 *
 * @author John Mazzitelli
 */
public class JBossRemotingRemoteCommunicator implements RemoteCommunicator {
    /**
     * The default subsystem to use when sending messages via the JBoss/Remoting client.
     */
    public static final String DEFAULT_SUBSYSTEM = "RHQ";

    /**
     * the JBoss/Remoting locator that this client will use to remotely connect to the command server
     */
    private InvokerLocator m_invokerLocator;

    /**
     * The subsystem to target when invoking commands. The subsystem is defined by the JBoss/Remoting API - it specifies
     * the actual invoker handler to target. The Command framework uses the subsystem to organize command processors
     * into different domains.
     */
    private String m_subsystem;

    /**
     * The configuration to send to the client - used to configure things like the SSL setup.
     */
    private Map<String, String> m_clientConfiguration;

    /**
     * the actual JBoss/Remoting client object that will be used to transport the commands to the server
     */
    private Client m_remotingClient;

    /**
     * Optionally-defined callback that will be called when a failure is detected when sending a message. 
     */
    private FailureCallback m_failureCallback;

    /**
     * Constructor for {@link JBossRemotingRemoteCommunicator} that initializes the client with no invoker locator
     * defined. It must later be specified through {@link #setInvokerLocator(InvokerLocator)} before any client commands
     * can be issued. In addition, the {@link #getSubsystem()} will be set to the {@link #DEFAULT_SUBSYSTEM}.
     *
     * <p>Note that all subclasses are strongly urged to include this no-arg constructor so it can plug into the cmdline
     * client seamlessly.</p>
     */
    public JBossRemotingRemoteCommunicator() {
        this((InvokerLocator) null, DEFAULT_SUBSYSTEM);
    }

    /**
     * Constructor for {@link JBossRemotingRemoteCommunicator} that allows you to indicate the
     * {@link InvokerLocator invoker locator} to use by specifying the locator's URI. The subsystem will be set to the
     * {@link #DEFAULT_SUBSYSTEM}.
     *
     * @param  locatorUri the locator's URI (must not be <code>null</code>)
     *
     * @throws MalformedURLException if failed to create the locator (see {@link InvokerLocator#InvokerLocator(String)})
     */
    public JBossRemotingRemoteCommunicator(String locatorUri) throws MalformedURLException {
        this(new InvokerLocator(locatorUri), DEFAULT_SUBSYSTEM);
    }

    /**
     * Constructor for {@link JBossRemotingRemoteCommunicator} that allows you to indicate the
     * {@link InvokerLocator invoker locator} to use by specifying the locator's URI. The subsystem will be set to the
     * {@link #DEFAULT_SUBSYSTEM}. The given <code>Map</code> should contain <code>Client</code> configuration
     * attributes.
     *
     * @param  locatorUri    the locator's URI (must not be <code>null</code>)
     * @param  client_config the client configuration (may be <code>null</code> or empty)
     *
     * @throws MalformedURLException if failed to create the locator (see {@link InvokerLocator#InvokerLocator(String)})
     */
    public JBossRemotingRemoteCommunicator(String locatorUri, Map<String, String> client_config)
        throws MalformedURLException {
        this(new InvokerLocator(locatorUri), DEFAULT_SUBSYSTEM, client_config);
    }

    /**
     * Constructor for {@link JBossRemotingRemoteCommunicator} that allows you to specify the
     * {@link InvokerLocator invoker locator} to use. <code>locator</code> may be <code>null</code>, in which case, it
     * must later be specified through {@link #setInvokerLocator(InvokerLocator)} before any client commands can be
     * issued. The subsystem will be set to the {@link #DEFAULT_SUBSYSTEM}.
     *
     * @param locator the locator to use (may be <code>null</code>)
     */
    public JBossRemotingRemoteCommunicator(InvokerLocator locator) {
        this(locator, DEFAULT_SUBSYSTEM);
    }

    /**
     * Constructor for {@link JBossRemotingRemoteCommunicator} that allows you to specify the
     * {@link InvokerLocator invoker locator} to use. <code>locator</code> may be <code>null</code>, in which case, it
     * must later be specified through {@link #setInvokerLocator(InvokerLocator)} before any client commands can be
     * issued.The subsystem will be set to the {@link #DEFAULT_SUBSYSTEM}. The given <code>Map</code> should contain
     * <code>Client</code> configuration attributes.
     *
     * @param locator       the locator to use (may be <code>null</code>)
     * @param client_config the client configuration (may be <code>null</code> or empty)
     */
    public JBossRemotingRemoteCommunicator(InvokerLocator locator, Map<String, String> client_config) {
        this(locator, DEFAULT_SUBSYSTEM, client_config);
    }

    /**
     * Constructor for {@link JBossRemotingRemoteCommunicator} that allows you to specify the
     * {@link InvokerLocator invoker locator} to use. <code>locator</code> may be <code>null</code>, in which case, it
     * must later be specified through {@link #setInvokerLocator(InvokerLocator)} before any client commands can be
     * issued.
     *
     * @param locator   the locator to use (may be <code>null</code>)
     * @param subsystem the subsystem (or command domain) in which commands will be invoked (may be <code>null</code>)
     */
    public JBossRemotingRemoteCommunicator(InvokerLocator locator, String subsystem) {
        this(locator, subsystem, null);
    }

    /**
     * Constructor for {@link JBossRemotingRemoteCommunicator} that allows you to specify the
     * {@link InvokerLocator invoker locator} to use. <code>locator</code> may be <code>null</code>, in which case, it
     * must later be specified through {@link #setInvokerLocator(InvokerLocator)} before any client commands can be
     * issued.
     *
     * @param locator       the locator to use (may be <code>null</code>)
     * @param subsystem     the subsystem (or command domain) in which commands will be invoked (may be <code>
     *                      null</code>)
     * @param client_config the client configuration (may be <code>null</code> or empty)
     */
    public JBossRemotingRemoteCommunicator(InvokerLocator locator, String subsystem, Map<String, String> client_config) {
        m_invokerLocator = locator;
        m_subsystem = subsystem;
        m_clientConfiguration = new HashMap<String, String>();

        if (client_config != null) {
            m_clientConfiguration.putAll(client_config);
        }

        return;
    }

    /**
     * Constructor for {@link JBossRemotingRemoteCommunicator} that allows you to indicate the
     * {@link InvokerLocator invoker locator} to use by specifying the locator's URI.
     *
     * @param  locatorUri the locator's URI (must not be <code>null</code>)
     * @param  subsystem  the subsystem (or command domain) in which commands will be invoked (may be <code>null</code>)
     *
     * @throws MalformedURLException if failed to create the locator (see {@link InvokerLocator#InvokerLocator(String)})
     */
    public JBossRemotingRemoteCommunicator(String locatorUri, String subsystem) throws MalformedURLException {
        this(new InvokerLocator(locatorUri), subsystem);
    }

    /**
     * Constructor for {@link JBossRemotingRemoteCommunicator} that allows you to indicate the
     * {@link InvokerLocator invoker locator} to use by specifying the locator's URI. The given <code>Map</code> should
     * contain <code>Client</code> configuration attributes.
     *
     * @param  locatorUri    the locator's URI (must not be <code>null</code>)
     * @param  subsystem     the subsystem (or command domain) in which commands will be invoked (may be <code>
     *                       null</code>)
     * @param  client_config the client configuration (may be <code>null</code> or empty)
     *
     * @throws MalformedURLException if failed to create the locator (see {@link InvokerLocator#InvokerLocator(String)})
     */
    public JBossRemotingRemoteCommunicator(String locatorUri, String subsystem, Map<String, String> client_config)
        throws MalformedURLException {
        this(new InvokerLocator(locatorUri), subsystem, client_config);
    }

    /**
     * Returns the invoker locator that is to be used to find the remote JBoss/Remoting server. If <code>null</code> is
     * returned, this communicator will not be able to issue commands.
     *
     * @return invoker locator used by this client to issue commands
     */
    public InvokerLocator getInvokerLocator() {
        return m_invokerLocator;
    }

    /**
     * Sets the invoker locator URI and creates a new locator that is to be used to find the remote JBoss/Remoting
     * server for its subsequent command client invocations. Any existing remoting client is automatically disconnected.
     * The client configuration properties will, however, remain the same as before - so the new clients that are
     * created will have the same configuration attributes. See {@link #setInvokerLocator(String, Map)} if you want to
     * reconfigure the client with different properties that are more appropriate for the new locator.
     *
     * @param  locatorUri the new invoker locator's URI to use for future command client invocations (must not be <code>
     *                    null</code>)
     *
     * @throws MalformedURLException if failed to create the locator (see {@link InvokerLocator#InvokerLocator(String)})
     *
     * @see    #setInvokerLocator(InvokerLocator)
     */
    public void setInvokerLocator(String locatorUri) throws MalformedURLException {
        setInvokerLocator(new InvokerLocator(locatorUri));
    }

    /**
     * Sets the invoker locator URI and creates a new locator that is to be used to find the remote JBoss/Remoting
     * server for its subsequent command client invocations. Any existing remoting client is automatically disconnected.
     * New remoting clients will also be configured with the new set of configuration properties - thus allowing you to
     * configure the client to be able to handle the new locator.
     *
     * @param  locatorUri    the new invoker locator's URI to use for future command client invocations (must not be
     *                       <code>null</code>)
     * @param  client_config the client configuration for any new remoting clients that are created (may be <code>
     *                       null</code> or empty)
     *
     * @throws MalformedURLException if failed to create the locator (see {@link InvokerLocator#InvokerLocator(String)})
     *
     * @see    #setInvokerLocator(InvokerLocator)
     */
    public void setInvokerLocator(String locatorUri, Map<String, String> client_config) throws MalformedURLException {
        setInvokerLocator(new InvokerLocator(locatorUri), client_config);
    }

    /**
     * Sets the invoker locator that this communicator should use for its subsequent command client invocations. Any
     * existing remoting client is automatically disconnected.The client configuration properties will, however, remain
     * the same as before - so the new clients that are created will have the same configuration attributes. See
     * {@link #setInvokerLocator(InvokerLocator, Map)} if you want to reconfigure the client with different properties
     * that are more appropriate for the new locator.
     *
     * @param  locator the new invoker locator to use for future command client invocations (must not be <code>
     *                 null</code>)
     *
     * @throws IllegalArgumentException if locator is <code>null</code>
     */
    public void setInvokerLocator(InvokerLocator locator) {
        setInvokerLocator(locator, null);
    }

    /**
     * Sets the invoker locator that this communicator should use for its subsequent command client invocations. Any
     * existing remoting client is automatically disconnected. New remoting clients will also be configured with the new
     * set of configuration properties - thus allowing you to configure the client to be able to handle the new locator.
     *
     * @param  locator       the new invoker locator to use for future command client invocations (must not be <code>
     *                       null</code>)
     * @param  client_config the client configuration for any new remoting clients that are created (may be <code>
     *                       null</code> or empty)
     *
     * @throws IllegalArgumentException if locator is <code>null</code>
     */
    public void setInvokerLocator(InvokerLocator locator, Map<String, String> client_config) {
        if (locator == null) {
            throw new IllegalArgumentException("locator=null");
        }

        // since a new invoker locator is being specified, disconnect any old client that already exists
        if (m_remotingClient != null) {
            m_remotingClient.disconnect();
            m_remotingClient = null;
        }

        m_invokerLocator = locator;

        if (client_config != null) {
            m_clientConfiguration.clear();
            m_clientConfiguration.putAll(client_config);
        }

        return;
    }

    /**
     * Returns the value of the subsystem that will be used to target command invocations. The subsystem is defined by
     * the JBoss/Remoting API and can be used by the Command Framework to organize command processors into different
     * domains.
     *
     * @return subsystem (may be <code>null</code>)
     */
    public String getSubsystem() {
        return m_subsystem;
    }

    /**
     * Sets the value of the subsystem that will be used to target command invocations. The subsystem is defined by the
     * JBoss/Remoting API and can be used by the Command Framework to organize command processors into different
     * domains.
     *
     * <p>If a remoting client already exists, its subsystem will be changed to the given subsystem.</p>
     *
     * @param subsystem the new value of subsystem (may be <code>null</code>)
     */
    public void setSubsystem(String subsystem) {
        m_subsystem = subsystem;

        if (m_remotingClient != null) {
            m_remotingClient.setSubsystem(subsystem);
        }

        return;
    }

    public FailureCallback getFailureCallback() {
        return m_failureCallback;
    }

    public void setFailureCallback(FailureCallback callback) {
        m_failureCallback = callback;
    }

    public String getRemoteEndpoint() {
        return (m_invokerLocator != null) ? m_invokerLocator.getLocatorURI() : "<null>";
    }

    /**
     * Returns the map of name/value pairs of client configuration settings used when creating the client. The returned
     * map is a copy - changing its contents has no effect on the clients that already have been or will be created by
     * this object. Use {@link #setInvokerLocator(InvokerLocator, Map)} or {@link #setInvokerLocator(String, Map)} to
     * change the map contents.
     *
     * @return copy of the configuration that will be used when creating clients (may be <code>null</code> or empty)
     */
    public Map<String, String> getClientConfiguration() {
        return new HashMap<String, String>(m_clientConfiguration);
    }

    public void connect() throws Exception {
        if ((m_remotingClient != null) && !m_remotingClient.isConnected()) {
            m_remotingClient.connect();
        }

        return;
    }

    public void disconnect() {
        if (m_remotingClient != null) {
            m_remotingClient.disconnect();
        }

        return;
    }

    public boolean isConnected() {
        return (m_remotingClient != null) && m_remotingClient.isConnected();
    }

    public CommandResponse sendWithoutFailureCallback(Command command) throws Throwable {
        Object ret_response;

        try {
            ret_response = getRemotingClient().invoke(command, null);
        } catch (ServerInvoker.InvalidStateException serverDown) {
            // see comments in #send for why this is here
            ret_response = getRemotingClient().invoke(command, null);
        }

        // this is to support http(s) transport - those transports will return Exception objects when errors occur
        if (ret_response instanceof Exception) {
            throw (Exception) ret_response;
        }

        try {
            return (CommandResponse) ret_response;
        } catch (Exception e) {
            // see comments in #send for why this is here
            CommI18NFactory.getLogger(JBossRemotingRemoteCommunicator.class).error(CommI18NResourceKeys.COMM_CCE,
                ret_response);
            return new GenericCommandResponse(command, false, ret_response, e);
        }
    }

    public CommandResponse send(Command command) throws Throwable {
        Object ret_response = null;
        boolean retry = false;

        do {
            try {
                try {
                    ret_response = getRemotingClient().invoke(command, null);
                } catch (ServerInvoker.InvalidStateException serverDown) {
                    // under rare condition, a bug in remoting 2.2 causes this when the server restarted
                    // try it one more time, this will get a new server thread on the server side (JBREM-745)
                    // once JBREM-745 is fixed, we can probably get rid of this catch block
                    ret_response = getRemotingClient().invoke(command, null);
                }

                // this is to support http(s) transport - those transports will return Exception objects when errors occur
                if (ret_response instanceof Exception) {
                    throw (Exception) ret_response;
                }

                retry = invokeCallbackIfNeeded(command,
                    (ret_response instanceof CommandResponse) ? (CommandResponse) ret_response : null, null);

            } catch (Throwable t) {
                retry = invokeCallbackIfNeeded(command,
                    (ret_response instanceof CommandResponse) ? (CommandResponse) ret_response : null, t);

                if (!retry) {
                    throw t;
                }
            }
        } while (retry);

        try {
            return (CommandResponse) ret_response;
        } catch (Exception e) {
            // JBNADM-2461 - I don't know if this is the right thing to do but...
            // I purposefully did not throw an exception here because the command actually did make a successful
            // round trip, so I think we would want to have a CommandResponse sent back, not throw an exception.
            // However, we got an exception when casting the remote endpoint's reply (the most likely cause here
            // is a ClassCastException - the endpoint didn't reply with the expected CommandResponse).
            CommI18NFactory.getLogger(JBossRemotingRemoteCommunicator.class).error(CommI18NResourceKeys.COMM_CCE,
                ret_response);
            return new GenericCommandResponse(command, false, ret_response, e);
        }
    }

    /**
     * This will invoke the failure callback when necessary.  It is necessary to call the callback
     * when the throwable is not <code>null</code> or the command response has a non-<code>null</code> exception.
     * 
     * This method will force a retry by returning <code>true</code>. If <code>false</code> is returned,
     * the request need not be retried.
     * 
     * @param command the command that was sent (or attempted to be sent)
     * @param response the response of the command (may be <code>null</code>)
     * @param throwable the exception that was thrown when the command was sent (may be <code>null</code>)
     * 
     * @return <code>true</code> if the command should be retried, <code>false</code> otherwise
     */
    private boolean invokeCallbackIfNeeded(Command command, CommandResponse response, Throwable throwable) {

        FailureCallback callback = getFailureCallback(); // get a local reference to avoid this being changed underneath us
        boolean retry = false;

        // only do something if there is a callback defined        
        if (callback != null) {
            // only do something if the command resulted in an exception
            if (throwable != null || ((response != null) && (response.getException() != null))) {
                // tell our callback that we detected a failure and see if it wants us to retry.
                // the callback is free to reconfigure us, in case it wants to failover to another endpoint.
                try {
                    retry = callback.failureDetected(this, command, response, throwable);
                } catch (Throwable t) {
                    // tsk tsk - why did the callback itself fail? just keep going...
                }
            }
        }

        return retry;
    }

    @Override
    public String toString() {
        return "remoting endpoint [" + ((m_invokerLocator != null) ? m_invokerLocator.getLocatorURI() : "?") + ']';
    }

    /**
     * Returns the remoting client that is to be used to transport the command request to the server. If for any reason
     * the client cannot be created, an exception is thrown. This will happen if the invoker locator has not been
     * specified (see {@link #setInvokerLocator(InvokerLocator)}).
     *
     * <p>This method will cache the client and connect to the server automatically. Note that the client will be
     * disconnected whenever the invoker is reset via {@link #setInvokerLocator(InvokerLocator)}. Therefore, callers
     * should never cache the returned object themselves - always call this method to obtain a reference to the
     * client.</p>
     *
     * @return the client to be used to transport the command request to the server
     *
     * @throws Exception if failed to create the client for whatever reason
     */
    protected Client getRemotingClient() throws Exception {
        if (m_remotingClient == null) {
            m_remotingClient = new Client(getInvokerLocator(), getSubsystem(), m_clientConfiguration);
        }

        if (!m_remotingClient.isConnected()) {
            m_remotingClient.connect();
        }

        return m_remotingClient;
    }
}