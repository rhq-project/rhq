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
package org.rhq.enterprise.server.operation;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.server.operation.OperationServerService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.util.exception.ExceptionPackage;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is the remote POJO implementation that takes in messages from the agent saying the agent finished invoking a
 * particular operation on a managed resource. The agent will notify the server of either:
 *
 * <ul>
 *   <li>an operation successfully completed</li>
 *   <li>an operation was invoked but failed</li>
 *   <li>an operation was invoked but timed out before completing</li>
 * </ul>
 *
 * @author John Mazzitelli
 */
public class OperationServerServiceImpl implements OperationServerService {
    private static final Log LOG = LogFactory.getLog(OperationServerServiceImpl.class);

    public void operationFailed(String jobId, ExceptionPackage error, long invocationTime, long completionTime) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Operation invocation [" + jobId + "] failed with error [" + error + "] "
                + getFromStartToEndTimestampString(invocationTime, completionTime));
        }

        try {
            Subject superuser = LookupUtil.getSubjectManager().getOverlord();
            OperationManagerLocal operationManager = LookupUtil.getOperationManager();
            OperationHistory history = operationManager.getOperationHistoryByJobId(superuser, jobId);

            // I think this will only ever occur if the server-side timed this out but the long running
            // operation finally got back to us afterwards. We will still go ahead and
            // persist the failure data because, obviously, the operation really didn't time out.
            // I think, in reality, this condition will never occur (since the server-side will only ever
            // timeout ridiculously long-lived operations, which is typically only when an agent shutdown occurred).
            if (history.getStatus() != OperationRequestStatus.INPROGRESS) {
                LOG.debug("Was told an operation failed but, curiously, it was not in progress: " + "job-id=[" + jobId
                    + "], op-history=[" + history + "]");
            }

            if (error != null) {
                history.setErrorMessage(error.getStackTraceString());
            } else {
                history.setErrorMessage("Failed for an unknown reason at " + new Date(completionTime));
            }

            history.setStatus(OperationRequestStatus.FAILURE);
            operationManager.updateOperationHistory(superuser, history);
        } catch (Exception e) {
            LOG.error("Failed to update history from failed operation, jobId=[" + jobId + "]. Cause: " + e, e);
            LOG.error("The failed operation [" + jobId + "] had an error of: "
                + ((error != null) ? error.getStackTraceString() : "?"));
        }
    }

    public void operationSucceeded(String jobId, Configuration result, long invocationTime, long completionTime) {
        LOG.debug("Operation invocation [" + jobId + "] succeeded "
            + getFromStartToEndTimestampString(invocationTime, completionTime));

        try {
            Subject superuser = LookupUtil.getSubjectManager().getOverlord();
            OperationManagerLocal operationManager = LookupUtil.getOperationManager();
            ResourceOperationHistory history;

            history = (ResourceOperationHistory) operationManager.getOperationHistoryByJobId(superuser, jobId);

            // I think this will only ever occur if the server-side timed this out but the long running
            // operation finally got back to us afterwards. We will still go ahead and
            // persist the success data because, obviously, the operation really didn't time out.
            // I think, in reality, this condition will never occur (since the server-side will only ever
            // timeout ridiculously long-lived operations, which is typically only when an agent shutdown occurred).
            if (history.getStatus() != OperationRequestStatus.INPROGRESS) {
                LOG.debug("Was told an operation succeeded but, curiously, it was not in progress: " + "job-id=["
                    + jobId + "], op-history=[" + history + "]");
            }

            history.setErrorMessage(null);
            history.setResults(result);
            history.setStatus(OperationRequestStatus.SUCCESS);
            operationManager.updateOperationHistory(superuser, history);
        } catch (Exception e) {
            LOG.error("Failed to update history from successful operation, jobId=[" + jobId + "]. Cause: " + e, e);
            LOG.error("The successful operation [" + jobId + "] had results of: " + result);
        }
    }

    public void operationTimedOut(String jobId, long invocationTime, long timeoutTime) {
        LOG.debug("Operation invocation [" + jobId + "] failed due to a timeout "
            + getFromStartToEndTimestampString(invocationTime, timeoutTime));

        try {
            Subject superuser = LookupUtil.getSubjectManager().getOverlord();
            OperationManagerLocal operationManager = LookupUtil.getOperationManager();
            OperationHistory history = operationManager.getOperationHistoryByJobId(superuser, jobId);

            if (history.getStatus() == OperationRequestStatus.INPROGRESS) {
                history.setErrorMessage("Timed out");
                history.setStatus(OperationRequestStatus.FAILURE);
                operationManager.updateOperationHistory(superuser, history);
            } else {
                // if the operation was not in progress, the server side probably already timed it out
                LOG.warn("Was told to timeout an operation history but it was not in progress: " + "job-id=[" + jobId
                    + "], op-history=[" + history + "]");
            }
        } catch (Exception e) {
            LOG.error("Failed to update history from timed out operation, jobId=[" + jobId + "]. Cause: " + e, e);
        }
    }

    private String getFromStartToEndTimestampString(long startTime, long endTime) {
        Date startDate = new Date(startTime);
        Date endDate = new Date(endTime);

        return "(from " + startDate + " to " + endDate + ")";
    }
}