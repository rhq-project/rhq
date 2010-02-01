/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugin.pc;

import java.io.Serializable;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * Represents the results of a control operation invocation that a server plugin component performed.
 *
 * @author John Mazzitell
 */
public class ControlResults implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Configuration complexResults = new Configuration();
    private String errorMessage = null; // non-null indicates the invocation failed

    /**
     * Indicates if the control operation invocation was a success. If this returns <code>false</code>,
     * call {@link #getError()} to get the error message describing the error.
     * 
     * @return <code>true</code> if the invocation was a success; <code>false</code> on failure
     */
    public boolean isSuccess() {
        return this.errorMessage == null;
    }

    /**
     * Returns the {@link Configuration} object that is used to contain all the complex data that resulted from
     * the invocation. The returned object is not a copy, so you can use this object to populate the complex
     * results.
     *
     * @return the object that will contain the complex results
     */
    public Configuration getComplexResults() {
        return complexResults;
    }

    /**
     * Returns the error message that describes why the invocation failed. If the invocation was
     * a success, this will return a <code>null</code>.
     * 
     * @return error message if invocation failed; <code>null</code> if success
     */
    public String getError() {
        return errorMessage;
    }

    /**
     * If the invocation was a failure, call this method to mark it as such. The caller must provide
     * a non-null error message.
     * 
     * @param errorMessage error message describing the failure, must not be <code>null</code>
     */
    public void setError(String errorMessage) {
        if (errorMessage == null) {
            throw new NullPointerException("errorMessage == null");
        }
        this.errorMessage = errorMessage;
    }

    /**
     * Convienence method to indicate a failure due to an exception.
     * 
     * @param exception exception describing the failure, must not be <code>null</code>
     */
    public void setError(Throwable exception) {
        if (exception == null) {
            throw new NullPointerException("exception == null");
        }
        setError(ThrowableUtil.getAllMessages(exception));
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(this.getClass().getSimpleName());
        if (getError() != null) {
            str.append(" error=[").append(getError()).append("];");
        }
        str.append(" results=[").append(getComplexResults()).append("]");
        return str.toString();
    }
}
