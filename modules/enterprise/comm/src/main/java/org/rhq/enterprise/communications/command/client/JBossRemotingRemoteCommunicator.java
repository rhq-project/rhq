/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.communications.command.client;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import mazz.i18n.Logger;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvoker;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.impl.generic.GenericCommandResponse;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;
import org.rhq.enterprise.communications.util.NotPermittedException;

/**
 * Provides basic functionality to all command clients that want to use JBoss/Remoting as the remoting framework.
 *
 * <p>This superclass provides the hooks by which users of the client can select the
 * {@link #setInvokerLocator(String) location of the remote server} and the {@link #setSubsystem(String) subsystem}
 * where the command is to be invoked.</p>
 *
 * <p>Under the covers, a {@link org.jboss.remoting.Client remoting client} is created and maintained by this object.
 * There is no need to call {@link #connect()} since it will be done automatically when appropriate; however,
 * it is good practice to tell this object to {@link #disconnect()} its client when no longer necessary to
 * issue commands to the remote server.</p>
 *
 * <p>All subclasses should include a no-arg constructor so they can be built dynamically by the command line client.</p>
 *
 * @author John Mazzitelli
 */
public class JBossRemotingRemoteCommunicator implements RemoteCommunicator {
    private static final Logger LOG = CommI18NFactory.getLogger(JBossRemotingRemoteCommunicator.class);

    /**
     * The default subsystem to use when sending messages via the JBoss/Remoting client.
     */
    public static final String DEFAULT_SUBSYSTEM = "RHQ";

    /**
     * the JBoss/Remoting locator that this client will use to remotely connect to the command server
     */
    private volatile InvokerLocator m_invokerLocator;

    /**
     * The subsystem to target when invoking commands. The subsystem is defined by the JBoss/Remoting API - it specifies
     * the actual invoker handler to target. The Command framework uses the subsystem to organize command processors
     * into different domains.
     */
    private final String m_subsystem;

    /**
     * The configuration to send to the client - used to configure things like the SSL setup.
     */
    private final Map<String, String> m_clientConfiguration;

    /**
     * the actual JBoss/Remoting client object that will be used to transport the commands to the server
     */
    private volatile AtomicReference<Client> m_client = new AtomicReference<Client>();

    /**
     * Optionally-defined callback that will be called when a failure is detected when sending a message.
     */
    private volatile FailureCallback m_failureCallback;

    /**
     * Optionally-defined callback that will be called when this communicator sends its first command.
     */
    private volatile InitializeCallback m_initializeCallback;

    /**
     * When <code>true</code>, the initialize callback will need to be called prior
     * to sending any commands. Used in conjunection with its associated RW lock.
     */
    private volatile boolean m_needToCallInitializeCallback;

    /**
     * RW lock when needing to access its associated atomic boolean flag.
     */
    private final ReentrantReadWriteLock m_needToCallInitializeCallbackLock;

    /**
     * Number of minutes to wait while attempting to acquire a lock before attempting
     * to invoke the initialize callback. If this amount of minutes expires before the lock
     * is acquired, an error will occur and the initialize callback will have to be attempted later.
     */
    private final long m_initializeCallbackLockAcquisitionTimeoutMins;

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
        this(new InvokerLocator(locatorUri), DEFAULT_SUBSYSTEM, null);
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

        m_needToCallInitializeCallback = false;
        m_needToCallInitializeCallbackLock = new ReentrantReadWriteLock();

        long mins;
        try {
            String minsStr = System.getProperty("rhq.communications.initial-callback-lock-wait-mins", "60");
            mins = Long.parseLong(minsStr);
        } catch (Exception e) {
            mins = 60L;
        }
        m_initializeCallbackLockAcquisitionTimeoutMins = mins;

        return;
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
     */
    public void setRemoteEndpoint(String endpoint) throws Exception {
        InvokerLocator locator = new InvokerLocator(endpoint);
        LOG.info(CommI18NResourceKeys.COMMUNICATOR_CHANGING_ENDPOINT, m_invokerLocator, locator);
        m_invokerLocator = locator;

        // since a new invoker locator is being specified, disconnect any old client that already exists
        disconnect();
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

    public FailureCallback getFailureCallback() {
        return m_failureCallback;
    }

    public void setFailureCallback(FailureCallback callback) {
        m_failureCallback = callback;
    }

    public InitializeCallback getInitializeCallback() {
        return m_initializeCallback;
    }

    public void setInitializeCallback(InitializeCallback callback) {
        m_initializeCallback = callback;
        m_needToCallInitializeCallback = (callback != null); // specifically do not synchronize by using lock, just set it
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

    /**
     * Does nothing; send a request to connect.
     */
    public void connect() throws Exception {
        /*
         * For the HTTP invoker, simply calling connect() doesn't do anything. It makes
         * sense to at least send something to test connectivity. However, the code doesn't
         * make use of this method.
        try {
            send(new EchoCommand());
        } catch (Exception e) {
            throw e;
        } catch (Throwable t) {
            throw new Error(t);
        }
        */
    }

    public void disconnect() {
        cacheClient(null);
    }

    public boolean isConnected() {
        Client client = m_client.get();
        return (client != null) && client.isConnected();
    }

    public CommandResponse sendWithoutCallbacks(Command command) throws Throwable {
        // handle NotPermittedException in here
        CommandResponse ret_response = null;
        boolean retry;
        do {
            retry = false;
            ret_response = rawSend(command);
            Throwable exception = ret_response.getException();
            if (exception instanceof NotPermittedException) {
                long pause = ((NotPermittedException) exception).getSleepBeforeRetry();
                LOG.debug(CommI18NResourceKeys.COMMAND_NOT_PERMITTED, command, pause);
                retry = true;
                Thread.sleep(pause);
            }
        } while (retry);

        return ret_response;
    }

    public CommandResponse sendWithoutInitializeCallback(Command command) throws Throwable {
        CommandResponse ret_response = null;
        boolean retry = false;

        do {
            try {
                ret_response = sendWithoutCallbacks(command);
                retry = invokeFailureCallbackIfNeeded(command, ret_response, null);
            } catch (Throwable t) {
                retry = invokeFailureCallbackIfNeeded(command, ret_response, t);
                if (!retry) {
                    throw t;
                }
            }
        } while (retry);

        return ret_response;
    }

    public CommandResponse send(Command command) throws Throwable {
        // invoke our initialize callback - if our method returns a response, it means
        // the initialize callback had an error and we need to abort the sending of this command
        CommandResponse initializeErrorResponse = invokeInitializeCallbackIfNeeded(command);
        if (initializeErrorResponse != null) {
            return initializeErrorResponse;
        }

        return sendWithoutInitializeCallback(command);
    }

    /**
     * The code that sends the command via the remote client.
     *
     * @param command the command to send
     *
     * @return the command response
     *
     * @throws Throwable if a low-level, unhandled exception occurred
     */
    private CommandResponse rawSend(Command command) throws Throwable {
        Object ret_response;

        try {
            try {
                OutgoingCommandTrace.start(command);
                ret_response = invoke(command);
                OutgoingCommandTrace.finish(command, ret_response);
            } catch (ServerInvoker.InvalidStateException serverDown) {
                // under rare condition, a bug in remoting 2.2 causes this when the server restarted
                // try it one more time, this will get a new server thread on the server side (JBREM-745)
                // once JBREM-745 is fixed, we can probably get rid of this catch block
                ret_response = invoke(command);
                OutgoingCommandTrace.finish(command, ret_response);
            } catch (java.rmi.MarshalException rmie) {
                // Due to JBREM-1245 we may fail due to SSL being shutdown and we need to retry.
                if (rmie.getCause() != null && rmie.getCause() instanceof javax.net.ssl.SSLException
                    && rmie.getCause().getMessage() != null
                    && rmie.getCause().getMessage().startsWith("Connection has been shutdown")) { //$NON-NLS-1$
                    ret_response = invoke(command);
                    OutgoingCommandTrace.finish(command, ret_response);
                } else {
                    throw rmie;
                }
            }
        } catch (Throwable t) {
            OutgoingCommandTrace.finish(command, t);
            throw t;
        }

        // this is to support http(s) transport - those transports will return Exception objects when errors occur
        if (ret_response instanceof Exception) {
            throw (Exception) ret_response;
        }

        try {
            return (CommandResponse) ret_response;
        } catch (Exception e) {
            // JBNADM-2461 - I don't know if this is the right thing to do but...
            // I purposefully did not throw an exception here because the command actually did make a successful
            // round trip, so I think we would want to have a CommandResponse sent back, not throw an exception.
            // However, we got an exception when casting the remote endpoint's reply (the most likely cause here
            // is a ClassCastException - the endpoint didn't reply with the expected CommandResponse).
            LOG.error(CommI18NResourceKeys.COMM_CCE, ret_response);
            return new GenericCommandResponse(command, false, ret_response, e);
        }
    }

    /**
     * This will determine if the initialize callback needs to be invoked and if it does, it will
     * call it.  The initialize callback has the responsibility to handle calling
     * {@link #sendWithoutInitializeCallback(Command)} if it wants to send its own commands to the server
     * but wants failover to happen when appropriate for those commands.
     *
     * If there is an initialize callback set, this method will block all callers until
     * the callback has been invoked.
     *
     * @param command the command that it going to be sent after the callback is invoked
     *
     * @return if the initialize callback had an error, this response will be non-<code>null</code> and
     *         will indicate that the sending of <code>command</code> should be aborted.
     */
    private CommandResponse invokeInitializeCallbackIfNeeded(Command command) {
        InitializeCallback callback = getInitializeCallback();
        if (callback != null) {
            // block here - in effect, this will stop all commands from going out until the callback is done
            // to avoid infinite blocking, we'll only wait for a set time (though long).

            WriteLock writeLock = m_needToCallInitializeCallbackLock.writeLock();
            boolean locked;
            try {
                locked = writeLock.tryLock(m_initializeCallbackLockAcquisitionTimeoutMins * 60, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                locked = false;
            }

            if (locked) {
                try {
                    if (m_needToCallInitializeCallback) {
                        try {
                            m_needToCallInitializeCallback = (!callback.sendingInitialCommand(this, command));
                            LOG.debug(CommI18NResourceKeys.INITIALIZE_CALLBACK_DONE, m_needToCallInitializeCallback);
                        } catch (Throwable t) {
                            m_needToCallInitializeCallback = true; // callback failed, we'll want to call it again
                            LOG.error(t, CommI18NResourceKeys.INITIALIZE_CALLBACK_FAILED, ThrowableUtil
                                .getAllMessages(t));
                            return new GenericCommandResponse(command, false, null, t);
                        }
                    }
                } finally {
                    writeLock.unlock();
                }
            } else {
                Throwable t = new Throwable("Initialize callback lock could not be acquired");
                LOG.error(CommI18NResourceKeys.INITIALIZE_CALLBACK_FAILED, t.getMessage());
                return new GenericCommandResponse(command, false, null, t);
            }
        }
        return null;
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
    private boolean invokeFailureCallbackIfNeeded(Command command, CommandResponse response, Throwable throwable) {

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
     * Invokes JBoss Remoting using the given command.
     * Attempts to cache the client if sending the message was successful.
     *
     * @return object as a result of this call
     */
    private Object invoke(Command command) throws Throwable {
        InvokerLocator locator = m_invokerLocator;
        if (locator == null) {
            throw new IllegalStateException("m_invokerLocator is null");
        }

        Client client = m_client.get();
        if (client != null && client.getInvoker() == null) {
            client.disconnect();
        }
        if (client == null || !client.isConnected()) {
            client = new Client(locator, getSubsystem(), m_clientConfiguration);
            client.connect();
            try {
                return client.invoke(command);
            } finally {
                cacheClient(client);
            }
        }

        // Note: Despite all the checks above, the client might have been
        // disconnected before invoke is reached. Let's hope that doesn't happen.

        return client.invoke(command);
    }

    /**
     * Cache the client, disconnecting the old client.
     *
     * @param client optionally null; new client to cache
     */
    private void cacheClient(Client client) {
        Client old = m_client.getAndSet(client);
        if (old != null) {
            old.disconnect();
            m_needToCallInitializeCallback = (getInitializeCallback() != null);
        }
    }

}
