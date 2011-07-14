/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.util.async;

import java.util.Map;

/**
 *
 * @author Ian Springer
 */
public class CompoundAsyncOperationException extends Exception {

    private Map<AsyncOperation,Object> successResults;
    private Map<AsyncOperation,Throwable> failureThrowables;

    public CompoundAsyncOperationException(Map<AsyncOperation, Object> successResults,
                                           Map<AsyncOperation, Throwable> failureThrowables) {
        super("One or more child operations failed.");
        this.successResults = successResults;
        this.failureThrowables = failureThrowables;
    }

    public Map<AsyncOperation, Object> getSuccessResults() {
        return successResults;
    }

    public Map<AsyncOperation, Throwable> getFailureThrowables() {
        return failureThrowables;
    }

}
