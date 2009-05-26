 /*
  * Jopr Management Platform
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
package com.jboss.jbossnetwork.product.jbpm.handlers.test;

import com.jboss.jbossnetwork.product.jbpm.handlers.ControlActionFacade;
import org.rhq.core.pluginapi.operation.OperationServicesResult;
import org.rhq.core.pluginapi.operation.OperationServicesResultCode;

/**
 * @author Jason Dobies
 */
public class MockControlActionFacade implements ControlActionFacade {
    /**
     * If this is true, calls into the facade will throw an error.
     */
    private boolean throwError = false;

    /**
     * Indicates the result that should be returned from calls to a facade instance.
     */
    private OperationServicesResultCode resultCode;

    /**
     * Indicates if the facade should report if the server is running or not.
     */
    private boolean isRunning;

    public OperationServicesResult start() {
        return makeResult();
    }

    public OperationServicesResult stop() {
        if (!isRunning) {
            throw new IllegalStateException("The server is not running");
        }

        return makeResult();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isThrowError() {
        return throwError;
    }

    public void setThrowError(boolean throwError) {
        this.throwError = throwError;
    }

    public OperationServicesResultCode getResultCode() {
        return resultCode;
    }

    public void setResultCode(OperationServicesResultCode resultCode) {
        this.resultCode = resultCode;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    private OperationServicesResult makeResult() {
        if (throwError) {
            throw new RuntimeException("Mock exception");
        }

        OperationServicesResult result = new OperationServicesResult(resultCode);

        if (resultCode == OperationServicesResultCode.FAILURE) {
            result.setErrorStackTrace("Fake stack trace");
        }

        return result;
    }
}