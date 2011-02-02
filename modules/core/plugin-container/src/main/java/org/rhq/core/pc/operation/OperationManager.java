/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.pc.operation;

import java.util.EnumSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.clientapi.agent.operation.CancelResults;
import org.rhq.core.clientapi.agent.operation.OperationAgentService;
import org.rhq.core.clientapi.agent.operation.CancelResults.InterruptedState;
import org.rhq.core.clientapi.server.operation.OperationServerService;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.operation.OperationInvocation.Status;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pc.util.LoggingThreadFactory;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.util.exception.WrappedRemotingException;

/**
 * Manages the scheduling and invocation of operations for all resources across all plugins.
 *
 * <p>This is an agent service; its interface is made remotely accessible if this is deployed within the agent.</p>
 *
 * @author Ian Springer
 * @author John Mazzitelli
 */
public class OperationManager extends AgentService implements OperationAgentService, ContainerService {
    private static final String SENDER_THREAD_POOL_NAME = "OperationManager.invoker";

    private final Log log = LogFactory.getLog(OperationManager.class);

    private PluginContainerConfiguration configuration;
    private Timer timer;
    private OperationThreadPoolGateway operationGateway;

    public OperationManager() {
        super(OperationAgentService.class);
    }

    public void initialize() {
        timer = new Timer(SENDER_THREAD_POOL_NAME + ".timeout-timer");

        // read the javadoc on ThreadPoolExecutor and how max pool size is affected when using LinkedBlockingQueue
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(10000);
        LoggingThreadFactory threadFactory = new LoggingThreadFactory(SENDER_THREAD_POOL_NAME, true);
        int maxPoolSize = configuration.getOperationInvokerThreadPoolSize();
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, maxPoolSize, 1000, TimeUnit.MILLISECONDS, queue,
            threadFactory);
        operationGateway = new OperationThreadPoolGateway(threadPool);
    }

    /**
     * This will shutdown the operation thread pool and attempt to cancel operations already in progress. Note that when
     * this method returns, you are not guaranteed that all operations are finished. If there were one or more
     * long-lived operations that do not want to be canceled (that is, they ignore the thread interrupt they will
     * receive and do not terminate promptly), those operations will still be running when this returns.
     *
     * @see ContainerService#shutdown()
     */
    public void shutdown() {
        timer.cancel();
        operationGateway.shutdown();
    }

    public void setConfiguration(PluginContainerConfiguration configuration) {
        this.configuration = configuration;
    }

    public void invokeOperation(@NotNull final String jobId, final int resourceId, @NotNull final String operationName,
        @Nullable final Configuration parameterConfig) throws PluginContainerException {
        invokeOperation(jobId, resourceId, operationName, parameterConfig, getOperationServerService());
    }

    /**
     * Not tying this call to a particular {@link OperationServerService} implementation allows other internal classes
     * to call this method and receive the results, rather than having them sent to the server.
     */
    protected void invokeOperation(final String jobId, final int resourceId, final String operationName,
        final Configuration parameterConfig, final OperationServerService operationServerService)
        throws PluginContainerException {
        try {
            final OperationInvocation[] theJob = new OperationInvocation[1]; // need array so we can use it in the timer task
            final long invocationTime = System.currentTimeMillis();

            OperationDefinition operationDefinition = getOperationDefinition(resourceId, operationName);
            if (operationDefinition != null && operationDefinition.getParametersConfigurationDefinition() != null
                && parameterConfig != null) {
                ConfigurationUtility.normalizeConfiguration(parameterConfig, operationDefinition
                    .getParametersConfigurationDefinition());
            }

            // create our timer task that will force the operation invocation to time out if it takes too long to complete
            final long operationTimeout = getOperationTimeout(operationDefinition, parameterConfig);

            // ensure the facet method timeout is comfortably longer than the operation timeout
            long facetMethodTimeout = operationTimeout + (10 * 1000L);
            final OperationFacet operationComponent = getOperationFacet(resourceId, facetMethodTimeout);
            final TimerTask timerTask = new TimerTask() {
                // TIMER TASK THREAD - waits until the timeout time expires - if this is not canceled before the timeout hits,
                // the operation invocation thread is interrupted and the server will be told the operation has timed out.
                @Override
                public void run() {
                    if (theJob[0] != null) {
                        theJob[0].markAsTimedOut();
                    }
                }
            };

            timer.schedule(timerTask, operationTimeout);

            theJob[0] = new OperationInvocation(resourceId, invocationTime, timerTask, parameterConfig, jobId,
                operationName, operationComponent, operationServerService, operationGateway, operationDefinition);

            operationGateway.submit(theJob[0]);
        } catch (Exception e) {
            log.warn("Failed to submit operation invocation request", e);
            throw new PluginContainerException("Failed to submit invocation request. resource=[" + resourceId
                + "], operation=[" + operationName + "], jobId=[" + jobId + "]", new WrappedRemotingException(e));
        }

        return;
    }

    public CancelResults cancelOperation(String jobId) {
        OperationInvocation operation = operationGateway.getOperationInvocation(jobId);
        /*
         * there is a small window of time during which the user sees the operation as INPROGRESS (and thus available
         * for cancellation).  the user click on the cancel button, but by the time the request makes it down to the
         * agent the operation has completed (or failed).  in this case, asking for the operation from the gateway will
         * fail with a NullPointerException.  here, we assume the job has finished and return that to the caller.
         */
        if (operation == null) {
            return new CancelResults(InterruptedState.FINISHED);
        }
        EnumSet<Status> interruptedStatus = operation.markAsCanceled();

        // tell the caller what state the operation was in when it was canceled
        if (interruptedStatus.contains(Status.FINISHED)) {
            return new CancelResults(InterruptedState.FINISHED);
        } else if (interruptedStatus.contains(Status.QUEUED)) {
            return new CancelResults(InterruptedState.QUEUED);
        } else if (interruptedStatus.contains(Status.RUNNING)) {
            return new CancelResults(InterruptedState.RUNNING);
        }

        return new CancelResults(InterruptedState.UNKNOWN);
    }

    /**
     * Given a resource, this obtains that resource's OperationFacet interface. If it does not support the operation
     * facet, an exception is thrown. The resource does *not* need to be in the STARTED (i.e. connected) state.
     *
     * @param  resourceId identifies the resource that is to have the operation invoked on it
     *
     * @return the resource's operation facet component
     *
     * @throws PluginContainerException on error
     */
    protected OperationFacet getOperationFacet(int resourceId, long facetMethodTimeout) throws PluginContainerException {
        return ComponentUtil.getComponent(resourceId, OperationFacet.class, FacetLockType.WRITE, facetMethodTimeout,
            false, false);
    }

    /**
     * Given a resource ID, this obtains that resource's type.
     *
     * @param  resourceId identifies the resource whose type is to be returned
     *
     * @return the resource's type, if known
     *
     * @throws PluginContainerException if cannot determine the resource's type
     */
    protected ResourceType getResourceType(int resourceId) throws PluginContainerException {
        return ComponentUtil.getResourceType(resourceId);
    }

    /**
     * If the invocation passed in a {@link OperationDefinition#TIMEOUT_PARAM_NAME timeout property} in the
     * configuration, it is used. If that is not set, but the operation metadata defines a timeout, it is used. If
     * neither of those are set, the plugin container's default timeout is used. The timeouts are always specified in
     * seconds.
     *
     * @param  paramConfig
     *
     * @return the timeout to use
     *
     * @throws PluginContainerException if the timeout found was invalid
     */
    private long getOperationTimeout(OperationDefinition operationDefinition, Configuration paramConfig)
        throws PluginContainerException {
        // see if this particular invocation has overridden all timeout defaults with its own
        if (paramConfig != null) {
            PropertySimple timeoutProperty = paramConfig.getSimple(OperationDefinition.TIMEOUT_PARAM_NAME);
            if (timeoutProperty != null) {
                try {
                    paramConfig.remove(timeoutProperty.getName()); // we have to remove it since ResourceComponents are not expecting it
                    Long timeout = timeoutProperty.getLongValue();
                    if (timeout != null) {
                        return timeout * 1000L;
                    }
                } catch (Exception e) {
                    throw new PluginContainerException("The timeout specified in the configuration was invalid: "
                        + timeoutProperty);
                }
            }
        }

        // see if the operation metadata defines the timeout
        if ((operationDefinition != null) && (operationDefinition.getTimeout() != null)) {
            return operationDefinition.getTimeout().longValue() * 1000L;
        }

        // use the PC's default since we can't find it anywhere else
        return configuration.getOperationInvocationTimeout() * 1000L;
    }

    /**
     * Returns the operation definition for the operation with the specified name on the
     * {@link org.rhq.core.domain.resource.Resource} with the specified id.
     *
     * @param  resourceId    a <code>Resource</code> id
     * @param  operationName an operation name
     *
     * @return the operation definition for the operation with the specified name on the <code>Resource</code> with the
     *         specified id
     *
     * @throws PluginContainerException if the resource type could not be determined for the specified <code>
     *                                  Resource</code> id
     */
    @Nullable
    private OperationDefinition getOperationDefinition(int resourceId, String operationName)
        throws PluginContainerException {
        ResourceType resourceType = getResourceType(resourceId);
        Set<OperationDefinition> operationDefinitions = resourceType.getOperationDefinitions();
        if (operationDefinitions != null) {
            for (OperationDefinition operationDefinition : operationDefinitions) {
                if (operationDefinition.getName().equals(operationName)) {
                    return operationDefinition;
                }
            }
        }

        return null;
    }

    /**
     * If this manager can talk to a server-side {@link OperationServerService}, a proxy to that service is returned.
     *
     * @return the server-side proxy; <code>null</code> if this manager doesn't have a server to talk to
     */
    private OperationServerService getOperationServerService() {
        if (configuration.getServerServices() != null) {
            return configuration.getServerServices().getOperationServerService();
        }

        return null;
    }
}