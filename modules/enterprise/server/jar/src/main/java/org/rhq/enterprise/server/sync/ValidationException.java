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

package org.rhq.enterprise.server.sync;

import java.util.Set;

import org.rhq.core.domain.sync.ConsistencyValidatorFailureReport;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ValidationException extends SynchronizationException {

    private static final long serialVersionUID = 1L;
    
    private Set<ConsistencyValidatorFailureReport> validationReports;
    
    public ValidationException(Set<ConsistencyValidatorFailureReport> validationReports) {
        this.validationReports = validationReports;
    }
    
    public ValidationException(String message, Set<ConsistencyValidatorFailureReport> validationReports) {
        super(message);
        this.validationReports = validationReports;
    }
        
    public ValidationException(String message, Throwable cause, Set<ConsistencyValidatorFailureReport> validationReports) {
        super(message, cause);
        this.validationReports = validationReports;
    }
        
    public ValidationException(Throwable cause, Set<ConsistencyValidatorFailureReport> validationReports) {
        super(cause);
        this.validationReports = validationReports;
    }
        
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(Throwable cause) {
        super(cause);
    }

    public Set<ConsistencyValidatorFailureReport> getValidationReports() {
        return validationReports;
    }
    
    @Override
    public String toString() {
        StringBuilder bld = new StringBuilder(super.toString());
        
        if (validationReports != null && !validationReports.isEmpty()) {
            bld.append("\nReports of individual validators:\n");
            for(ConsistencyValidatorFailureReport r : validationReports) {
                bld.append("\n").append(r);
            }
        }
        
        return bld.toString();
    }
}
