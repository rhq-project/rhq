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

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * {"outcome" : "success", "result" : "no metrics available", "compensating-operation" : null}
 * @author Heiko W. Rupp
 */
public class Result {

    private String outcome;
    private Object result;
    @JsonProperty("compensating-operation")
    private Object compensatingOperation;
    @JsonIgnore
    private boolean success = false;

    public Result() {

    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
        if (outcome.equalsIgnoreCase("success"))
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

    public Object getCompensatingOperation() {
        return compensatingOperation;
    }

    public void setCompensatingOperation(Object compensatingOperation) {
        this.compensatingOperation = compensatingOperation;
    }
}
