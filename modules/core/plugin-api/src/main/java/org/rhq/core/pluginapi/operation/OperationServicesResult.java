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
package org.rhq.core.pluginapi.operation;

import org.rhq.core.domain.configuration.Configuration;

/**
 * Transfer object between the plugin container and the plugin. Indicates the result of a plugin executing an operation
 * through the plugin container.
 *
 * @author Jason Dobies
 */
public class OperationServicesResult {
    private OperationServicesResultCode resultCode;
    private Configuration complexResults;
    private String errorStackTrace;

    public OperationServicesResult(OperationServicesResultCode resultCode) {
        this.resultCode = resultCode;
    }

    public OperationServicesResultCode getResultCode() {
        return resultCode;
    }

    public void setResultCode(OperationServicesResultCode resultCode) {
        this.resultCode = resultCode;
    }

    public Configuration getComplexResults() {
        return complexResults;
    }

    public void setComplexResults(Configuration complexResults) {
        this.complexResults = complexResults;
    }

    public String getErrorStackTrace() {
        return errorStackTrace;
    }

    public void setErrorStackTrace(String errorStackTrace) {
        this.errorStackTrace = errorStackTrace;
    }
}