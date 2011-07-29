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

package org.rhq.core.domain.sync;

import java.io.Serializable;

/**
 * A simple class representing the validation failure of a single validator.
 *
 * @author Lukas Krejci
 */
public class ConsistencyValidatorFailureReport implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String validatorClassName;
    private String errorMessage;
    
    public ConsistencyValidatorFailureReport() {
    }

    public ConsistencyValidatorFailureReport(String validatorClassName, String errorMessage) {
        this.validatorClassName = validatorClassName;
        this.errorMessage = errorMessage;
    }

    /**
     * @return the validatorClassName
     */
    public String getValidatorClassName() {
        return validatorClassName;
    }

    /**
     * @param validatorClassName the validatorClassName to set
     */
    public void setValidatorClassName(String validatorClassName) {
        this.validatorClassName = validatorClassName;
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @param errorMessage the errorMessage to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    @Override
    public String toString() {
        StringBuilder bld = new StringBuilder();
        
        bld.append("ConsistencyValidatorFailureReport[validator='")
            .append(validatorClassName).append("', message='")
            .append(errorMessage).append("']");
        
        return bld.toString();
    }
}
