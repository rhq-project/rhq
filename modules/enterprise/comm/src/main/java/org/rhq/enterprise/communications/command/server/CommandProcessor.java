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
package org.rhq.enterprise.communications.command.server;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import mazz.i18n.Logger;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.stream.StreamInvocationHandler;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.impl.generic.GenericCommandResponse;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommand;
import org.rhq.enterprise.communications.command.impl.remotepojo.RemotePojoInvocationCommand;
import org.rhq.enterprise.communications.command.server.CommandProcessorMetrics.UnsuccessfulReason;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;
import org.rhq.enterprise.communications.util.NotPermittedException;
import org.rhq.enterprise.communications.util.NotProcessedException;

/**
 * Handles invoked {@link Command commands} from remote clients.
 *
 * <p>This server invocation handler will delegate the actual execution of the commands to
 * {@link CommandServiceMBean command services} located in the same
 * {@link org.jboss.remoting.ServerInvocationHandler#setMBeanServer(MBeanServer) MBeanServer} as this handler.</p>
 *
 * <p>When this handler is given an {@link org.jboss.remoting.InvocationRequest invocation request}, it determines the
 * <code>ObjectName</code> of the command service to delegate to by:</p>
 *
 * <ol>
 *   <li>Using the handler's {@link org.jboss.remoting.InvocationRequest#getSubsystem() subsystem} as the name of the
 *     command service's subsystem</li>
 *   <li>Passing the subsystem and {@link Command#getCommandType() command type} to the
 *     {@link CommandServiceDirectoryMBean directory} which looks up the command service that provides the command and
 *     returns its name</li>
 * </ol>
 *
 * <p>This handler will delegate the command to that service's execute method and will return its return value as-is
 * back to this invocation handler's client.</p>
 *
 * @author John Mazzitelli
 */
public class CommandProcessor implements StreamInvocationHandler {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(CommandProcessor.class);

    /**
     * the MBeanServer where this handler is located
     */
    private MBeanServer m_mBeanServer;

    /**
     * a proxy to the directory service
     */
    private CommandServiceDirectoryMBean m_directoryService;

    /**
     * The object to authenticate all incoming commands. Maybe <code>null</code>, in which case all commands will be
     * allowed to be processed.
     */
    private CommandAuthenticator m_authenticator;

    /**
     * These are notified whenever a new command is received.
     */
    private final List<CommandListener> m_commandListeners;

    /**
     * Where all the statistics are stored.
     */
    private final CommandProcessorMetrics m_metrics;

    /**
     * Constructor for {@link CommandProcessor}.
     */
    public CommandProcessor() {
        m_mBeanServer = null;
        m_directoryService = null;
        m_authenticator = null;
        m_commandListeners = new CopyOnWriteArrayList<CommandListener>();
        m_metrics = new CommandProcessorMetrics();
    }

    /**
     * @see org.jboss.remoting.ServerInvocationHandler#setMBeanServer(javax.management.MBeanServer)
     */
    public void setMBeanServer(MBeanServer mbs) {
        m_mBeanServer = mbs;
    }

    /**
     * @see org.jboss.remoting.ServerInvocationHandler#setInvoker(org.jboss.remoting.ServerInvoker)
     */
    public void setInvoker(ServerInvoker invoker) {
        return; // we don't care what invoker is being used
    }

    /**
     * Sets the object that will perform the security checks necessary to authenticate incoming commands. If <code>
     * null</code>, no security checks will be performed and all commands will be considered authenticated.
     *
     * @param authenticator the object to perform authentication checks on all incoming commands
     */
    public void setCommandAuthenticator(CommandAuthenticator authenticator) {
        m_authenticator = authenticator;
        LOG.debug(CommI18NResourceKeys.COMMAND_PROCESSOR_AUTHENTICATOR_SET, authenticator);
    }

    /**
     * Adds the given listener to this object's list of command listeners. This listener will be called each and every
     * time a new command has been received by this object.
     *
     * @param listener
     */
    public void addCommandListener(CommandListener listener) {
        m_commandListeners.add(listener);
    }

    /**
     * Removes the given listener from this object's list of command listeners. This listener will no longer be called
     * when commands are received by this object.
     *
     * @param listener
     */
    public void removeCommandListener(CommandListener listener) {
        m_commandListeners.remove(listener);
    }

    /**
     * Returns the metrics object which contains all statistics for the command processor.
     * 
     * @return the object containing all the metric data - this is the live object and will be updated
     *         as more data is collected
     */
    public CommandProcessorMetrics getCommandProcessorMetrics() {
        return m_metrics;
    }

    /**
     * Invokes the {@link Command} that is found in the {@link InvocationRequest#getParameter() invocation parameter}.
     * Note that the {@link InvocationRequest#getSubsystem() subsystem} being invoked must be the subsystem where the
     * command service to be invoked is registered.
     *
     * @see ServerInvocationHandler#invoke(org.jboss.remoting.InvocationRequest)
     */
    public Object invoke(InvocationRequest invocation) throws Throwable {
        return handleIncomingInvocationRequest(null, invocation);
    }

    /**
     * Handles incoming stream data from a client request that used the JBoss/Remoting streaming API.
     *
     * @see StreamInvocationHandler#handleStream(InputStream, InvocationRequest)
     */
    public Object handleStream(InputStream in, InvocationRequest invocation) throws Throwable {
        return handleIncomingInvocationRequest(in, invocation);
    }

    /**
     * @see ServerInvocationHandler#addListener(InvokerCallbackHandler)
     */
    public void addListener(InvokerCallbackHandler callbackHandler) {
        // TODO not yet implemented - unsure if we want to support callbacks
    }

    /**
     * @see ServerInvocationHandler#removeListener(InvokerCallbackHandler)
     */
    public void removeListener(InvokerCallbackHandler callbackHandler) {
        // TODO not yet implemented - unsure if we want to support callbacks
    }

    /**
     * This handles incoming invocation requests - this is the common code that both {@link #invoke(InvocationRequest)}
     * and {@link #handleStream(InputStream, InvocationRequest)} will execute.
     *
     * <p>The design of this method is that it will always return a {@link CommandResponse}, no matter what the results.
     * This way, if the client gets an exception or doesn't get a {@link CommandResponse} back, it can be assured that
     * the exception was caused by a connection error or an error within the low-level comm layer.</p>
     *
     * @param  in         the input stream if this is a streaming request utilizing the JBoss/Remoting stream API (may
     *                    be <code>null</code> which means it isn't a streaming request)
     * @param  invocation the incoming request
     *
     * @return the response
     *
     * @see    #invoke(InvocationRequest)
     * @see    #handleStream(InputStream, InvocationRequest)
     */
    private Object handleIncomingInvocationRequest(InputStream in, InvocationRequest invocation) {
        Command cmd = null;
        CommandResponse ret_response = null;

        long elapsed = 0L; // will be the time in ms that it took to invoked the command service if we did invoke it

        try {
            // get the subsystem - find the command service in this subsystem that will execute our command
            String subsystem = invocation.getSubsystem();

            // get the Command the client wants to execute
            cmd = (Command) invocation.getParameter();
            IncomingCommandTrace.start(cmd);

            if (cmd != null) {
                notifyListenersOfReceivedCommand(cmd);

                // make sure the command is authenticated; if it is not, return immediately without further processing the command
                if (m_authenticator != null) {
                    if (!m_authenticator.isAuthenticated(cmd)) {
                        // We don't want to flood the logs with authentication errors if the command is
                        // the identify command since that is expected to fail when attempting to auto-detect
                        // servers that we aren't yet authorized to talk to yet. So we only log a warn
                        // and we only increment our getNumberFailedCommands counter if its not an identify command.
                        if ((cmd.getCommandType() == null)
                            || !cmd.getCommandType().getName().equals(IdentifyCommand.COMMAND_TYPE.getName())) {
                            LOG.warn(CommI18NResourceKeys.COMMAND_PROCESSOR_FAILED_AUTHENTICATION, cmd);
                            m_metrics.numberFailedCommands++;
                        }

                        String err = LOG
                            .getMsgString(CommI18NResourceKeys.COMMAND_PROCESSOR_FAILED_AUTHENTICATION, cmd);
                        ret_response = new GenericCommandResponse(null, false, null, new AuthenticationException(err));

                        notifyListenersOfProcessedCommand(cmd, ret_response);

                        return ret_response;
                    }
                }

                // get the command's type
                CommandType cmdType = cmd.getCommandType();

                // ask the directory what command service supports the command we want to execute
                CommandServiceDirectoryEntry entry = null;
                ObjectName cmdServiceName = null;

                entry = getCommandServiceDirectory().getCommandTypeProvider(subsystem, cmdType);

                if (entry != null) {
                    cmdServiceName = entry.getCommandServiceName();
                }

                if (cmdServiceName != null) {
                    // now delegate the execution of the command to the command service
                    CommandServiceMBean executor;

                    executor = (CommandServiceMBean) MBeanServerInvocationHandler.newProxyInstance(m_mBeanServer,
                        cmdServiceName, CommandServiceMBean.class, false);

                    LOG.debug(CommI18NResourceKeys.COMMAND_PROCESSOR_EXECUTING, cmd);
                    long start = System.currentTimeMillis();

                    ret_response = executor.execute(cmd, in, null);

                    elapsed = System.currentTimeMillis() - start;
                    LOG.debug(CommI18NResourceKeys.COMMAND_PROCESSOR_EXECUTED, ret_response);
                } else {
                    throw new InstanceNotFoundException(LOG.getMsgString(
                        CommI18NResourceKeys.COMMAND_PROCESSOR_UNSUPPORTED_COMMAND_TYPE, subsystem, cmdType));
                }
            } else {
                LOG.warn(CommI18NResourceKeys.COMMAND_PROCESSOR_MISSING_COMMAND);
                ret_response = new GenericCommandResponse(null, false, null, new Exception(LOG
                    .getMsgString(CommI18NResourceKeys.COMMAND_PROCESSOR_MISSING_COMMAND)));
            }
        } catch (Throwable t) {
            ret_response = new GenericCommandResponse(cmd, false, null, t);
        } finally {
            IncomingCommandTrace.finish(cmd, ret_response);

            // as per JBoss/Remoting docs, you must ensure you close the input stream
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable t) {
                }
            }
        }

        // finish our processing
        try {
            if (ret_response == null) {
                ret_response = new GenericCommandResponse(cmd, false, null, new IllegalStateException(
                    "results are null"));
            }

            updateMetrics(cmd, ret_response, elapsed);

            notifyListenersOfProcessedCommand(cmd, ret_response);
        } catch (Throwable t) {
            // some incredibly rare throwable just happened, just log it but return anyway
            LOG.warn(t, CommI18NResourceKeys.COMMAND_PROCESSOR_POST_PROCESSING_FAILURE, cmd);
        }

        return ret_response;
    }

    /**
     * Stores the metric data.
     * 
     * @param command the command that was executed (might be null in error conditions)
     * @param response the response that resulted in the command execution
     * @param elapsed the amount of milliseconds that it took to execute the command and get the response
     */
    private void updateMetrics(Command cmd, CommandResponse response, long elapsed) {
        boolean success = response.isSuccessful();
        CommandProcessorMetrics.UnsuccessfulReason unsuccessfulReason = null;

        // now that we processed the command, update the appropriate metrics
        m_metrics.writeLock();
        try {
            if (success) {
                long num = ++m_metrics.numberSuccessfulCommands;

                // calculate the running average - num is the current command count
                // this may not be accurate if we execute this code concurrently,
                // but its good enough for our simple monitoring needs
                long currentAvg = m_metrics.averageExecutionTime;
                currentAvg = (((num - 1) * currentAvg) + elapsed) / num;
                m_metrics.averageExecutionTime = currentAvg;
            } else {
                if (response.getException() instanceof NotPermittedException) {
                    m_metrics.numberDroppedCommands++;
                    unsuccessfulReason = UnsuccessfulReason.DROPPED;
                } else if (response.getException() instanceof NotProcessedException) {
                    m_metrics.numberNotProcessedCommands++;
                    unsuccessfulReason = UnsuccessfulReason.NOT_PROCESSED;
                } else {
                    m_metrics.numberFailedCommands++;
                    unsuccessfulReason = UnsuccessfulReason.FAILED;
                }
            }

            // cmd might be null under odd, error edge cases, have to just be protective here.
            if (cmd != null) {
                CommandType cmdType = cmd.getCommandType();
                m_metrics.addCallTimeData(cmdType.getName(), elapsed, unsuccessfulReason);
                if (cmd instanceof RemotePojoInvocationCommand) {
                    // add additional metrics for the individual pojo method that was invoked
                    RemotePojoInvocationCommand pojoCmd = (RemotePojoInvocationCommand) cmd;
                    String ifaceName = pojoCmd.getTargetInterfaceName();
                    ifaceName = ifaceName.substring(ifaceName.lastIndexOf('.') + 1);
                    String methodName = pojoCmd.getNameBasedInvocation().getMethodName();
                    m_metrics.addCallTimeData(ifaceName + '.' + methodName, elapsed, unsuccessfulReason);
                }
            }
        } finally {
            m_metrics.writeUnlock();
        }
        return;
    }

    private void notifyListenersOfReceivedCommand(Command command) {
        for (CommandListener listener : m_commandListeners) {
            // notice that we bubble up NotPermittedException or NotProcessedException and abort the command, but any other exception we just log and move on
            try {
                listener.receivedCommand(command);
            } catch (NotPermittedException npe) {
                throw npe;
            } catch (NotProcessedException npe) {
                throw npe;
            } catch (Throwable t) {
                LOG.warn(t, CommI18NResourceKeys.COMMAND_PROCESSOR_LISTENER_ERROR_RECEIVED, t);
            }
        }
    }

    /**
     * This will inform all command listeners that a command has finished and has the given response.
     *
     * <p>Note that this method ensures that it will not throw any exceptions.</p>
     *
     * @param command  the command that was processed
     * @param response the result of the command processing
     */
    private void notifyListenersOfProcessedCommand(Command command, CommandResponse response) {
        for (CommandListener listener : m_commandListeners) {
            try {
                // notice that on any exception we just log and move on
                listener.processedCommand(command, response);
            } catch (Throwable t) {
                LOG.warn(t, CommI18NResourceKeys.COMMAND_PROCESSOR_LISTENER_ERROR_PROCESSED, t);
            }
        }
    }

    /**
     * Returns a proxy to the {@link CommandServiceDirectoryMBean command service directory}. This will provide a
     * directory that returns the names of command services that support command types.
     *
     * @return proxy to the command service directory
     *
     * @throws Exception                 if failed to get the proxy
     * @throws InstanceNotFoundException the directory service is not registered
     */
    private CommandServiceDirectoryMBean getCommandServiceDirectory() throws Exception {
        if (m_directoryService == null) {
            // find the directory - there should be only one and we don't care what JMX domain it is in
            // in the future, we may want to have this directory in the same JMX domain as this invocation handler's subsystem
            ObjectName query = new ObjectName("*:" + KeyProperty.TYPE + "=" + KeyProperty.TYPE_DIRECTORY);
            Set names = m_mBeanServer.queryNames(query, null);

            if (names != null) {
                // we don't care if there happens to be more than one (there really should not be but...), use the first one
                ObjectName directoryName = (ObjectName) names.iterator().next();

                m_directoryService = (CommandServiceDirectoryMBean) MBeanServerInvocationHandler.newProxyInstance(
                    m_mBeanServer, directoryName, CommandServiceDirectoryMBean.class, false);
            } else {
                throw new InstanceNotFoundException(LOG.getMsgString(
                    CommI18NResourceKeys.COMMAND_PROCESSOR_NO_DIRECTORY, query));
            }
        }

        return m_directoryService;
    }
}