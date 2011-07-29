/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7.json;

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Counterpart of a result JSON object like e.g.:
 * <pre>{"outcome" : "success", "result" : "no metrics available", "compensating-operation" : null}</pre>
 * @author Heiko W. Rupp
 */
public class Result {

    public static final String SUCCESS = "success";
    private String outcome;
    private Object result;
    @JsonProperty("failure-description")
    private /*List<Map<String, String>>*/Object failureDescription;
    @JsonIgnore
    private boolean success = false;
    @JsonProperty("rolled-back")
    private boolean rolledBack = false;

    /** Record throwsables during low level processing */
    @JsonIgnore
    private Throwable throwable;

    public Object getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Object responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    @JsonProperty("response-headers")
    private Object responseHeaders;

    public Result() {

    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
        if (outcome.equalsIgnoreCase(SUCCESS))
            success = true;
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getFailureDescription() {
        return failureDescription + ", rolled-back=" + rolledBack;
    }

    public void setFailureDescription(/*List<Map<String, String>>*/Object failureDescription) {
        this.failureDescription = failureDescription;
    }

    public boolean isRolledBack() {
        return rolledBack;
    }

    public void setRolledBack(boolean rolledBack) {
        this.rolledBack = rolledBack;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }


    @Override
    public String toString() {
        return "Result{" +
                "success=" + success +
                '}';
    }
}
