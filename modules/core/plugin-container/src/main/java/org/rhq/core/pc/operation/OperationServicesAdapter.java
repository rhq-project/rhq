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

import java.util.HashMap;
import java.util.Map;

import org.rhq.core.clientapi.server.operation.OperationServerService;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationServices;
import org.rhq.core.pluginapi.operation.OperationServicesResult;
import org.rhq.core.pluginapi.operation.OperationServicesResultCode;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.exception.ExceptionPackage;

/**
 * Bridge between the plugin's calls back into the PC from an agent-side plugin and the {@link OperationManager} itself.
 *
 * @author Jason Dobies
 */
public class OperationServicesAdapter implements OperationServices, OperationServerService {
    /**
     * Reference to the plugin's operation manager, which will do the actual work of invoking an operation.
     */
    private OperationManager operationManager;

    /**
     * The call to invoke the operation will block on this object until the response comes back from the
     * OperationManager.
     */
    private final Object callbackLock = new Object();

    /**
     * When an operation is completed, its result will be put in this map. When the thread that called the operation
     * wakes up, it will check this map for its job ID. If it is in here, it will know the job has finished and return
     * to the caller. If the job ID is not in this map, the operation is still going on and the thread will go back to
     * sleep.
     */
    private Map<String, OperationServicesResult> completedJobs = new HashMap<String, OperationServicesResult>();

    public OperationServicesAdapter(OperationManager operationManager) {
        this.operationManager = operationManager;
    }

    public OperationServicesResult invokeOperation(OperationContext context, String operationName,
        Configuration operationParameters, long timeout) {
        if (timeout < 1) {
            throw new IllegalArgumentException("timeout must be greater than zero");
        }

        OperationContextImpl contextImpl = (OperationContextImpl) context;

        // OperationManager will check the parameter configuration for the timeout, so if the plugin specified a
        // timeout, stuff it in the parameter config (making it if it doesn't exist)
        if (timeout > 0) {
            if (operationParameters == null) {
                operationParameters = new Configuration();
            }

            operationParameters.put(new PropertySimple(OperationDefinition.TIMEOUT_PARAM_NAME, timeout));
        }

        String jobId = "OperationServicesAdapter." + System.currentTimeMillis();
        synchronized (callbackLock) {
            try {
                operationManager.invokeOperation(jobId, contextImpl.getResourceId(), operationName,
                    operationParameters, this);

                // If the plugin executes multiple operations at the same time, the notify may be indicating a different
                // job has completed than the one being waited on.
                while (!completedJobs.containsKey(jobId)) {
                    callbackLock.wait();
                }
            } catch (Exception e) {
                OperationServicesResult result = new OperationServicesResult(OperationServicesResultCode.FAILURE);
                result.setErrorStackTrace(StringUtil.getStackTrace(e));

                return result;
            }
        }

        OperationServicesResult result = completedJobs.get(jobId);
        completedJobs.remove(jobId);

        return result;
    }

    public void operationSucceeded(String jobId, Configuration result, long invocationTime, long completionTime) {
        OperationServicesResult operationServicesResult = new OperationServicesResult(
            OperationServicesResultCode.SUCCESS);
        operationServicesResult.setComplexResults(result);

        completedJobs.put(jobId, operationServicesResult);
        synchronized (callbackLock) {
            callbackLock.notifyAll();
        }
    }

    public void operationFailed(String jobId, Configuration result, ExceptionPackage error, long invocationTime,
        long completionTime) {
        OperationServicesResult operationServicesResult = new OperationServicesResult(
            OperationServicesResultCode.FAILURE);
        operationServicesResult.setComplexResults(result);
        operationServicesResult.setErrorStackTrace(error.getStackTraceString());

        completedJobs.put(jobId, operationServicesResult);
        synchronized (callbackLock) {
            callbackLock.notifyAll();
        }
    }

    public void operationTimedOut(String jobId, long invocationTime, long timeoutTime) {
        OperationServicesResult operationServicesResult = new OperationServicesResult(
            OperationServicesResultCode.TIMED_OUT);

        completedJobs.put(jobId, operationServicesResult);
        synchronized (callbackLock) {
            callbackLock.notifyAll();
        }
    }
}