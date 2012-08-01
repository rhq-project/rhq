/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.common.jbossas.client.controller;

import org.jboss.dmr.ModelNode;

/**
 * Indicates a failed client request.
 *
 * @author John Mazzitelli
 */
public class FailureException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private static final String GENERIC_ERROR = "Failed request";

    public FailureException(ModelNode failureNode) {
        super(buildErrorMessage(GENERIC_ERROR, failureNode));
    }

    public FailureException(ModelNode failureNode, String errMsg) {
        super(buildErrorMessage(errMsg, failureNode));
    }

    public FailureException(ModelNode failureNode, Throwable cause) {
        super(buildErrorMessage(GENERIC_ERROR, failureNode), cause);
    }

    public FailureException(ModelNode failureNode, String errMsg, Throwable cause) {
        super(buildErrorMessage(errMsg, failureNode), cause);
    }

    public FailureException(String errMsg, Throwable cause) {
        super((errMsg != null) ? errMsg : GENERIC_ERROR, cause);
    }

    public FailureException(String errMsg) {
        super((errMsg != null) ? errMsg : GENERIC_ERROR);
    }

    public FailureException(Throwable cause) {
        super(GENERIC_ERROR, cause);
    }

    public FailureException() {
        super(GENERIC_ERROR);
    }

    private static final String buildErrorMessage(String errMsg, ModelNode failureNode) {
        if (errMsg == null) {
            errMsg = GENERIC_ERROR;
        }

        String description = JBossASClient.getFailureDescription(failureNode);
        if (description != null) {
            errMsg += ": " + description;
        }

        return errMsg;
    }
}
