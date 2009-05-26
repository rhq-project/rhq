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
package com.jboss.jbossnetwork.product.jbpm.handlers;

import org.jbpm.graph.exe.ExecutionContext;
import org.rhq.core.pluginapi.operation.OperationServicesResult;
import org.rhq.core.pluginapi.operation.OperationServicesResultCode;

/**
 * JBPM handler which executes an action on the JBoss AS instance being manipulated.
 *
 * @author Jason Dobies
 */
public class ControlActionHandler extends BaseHandler {
    /**
     * Name of the action being performed on the JBoss AS server.
     */
    private String actionName;

    public void run(ExecutionContext executionContext) {
        ControlActionFacade facade =
            (ControlActionFacade)executionContext.getVariable(ContextVariables.CONTROL_ACTION_FACADE);
        ControlActions action;

        try {
            action = Enum.valueOf(ControlActions.class, actionName.toUpperCase());
        } catch (IllegalArgumentException e) {
            error(executionContext, null, "Invalid action specified [" + actionName + "] to be executed on server.",
                TRANSITION_ERROR);
            return;
        }

        OperationServicesResult result = null;
        Throwable operationError = null;
        try {
            switch (action) {
                case START: {
                    result = facade.start();
                    break;
                }

                case STOP: {
                    result = facade.stop();
                    break;
                }

                case STOPIFRUNNING: {
                    if (facade.isRunning()) {
                        result = facade.stop();
                    }

                    break;
                }

                case RESTART: {
                    result = facade.stop();
                    if (result.getResultCode() == OperationServicesResultCode.SUCCESS) {
                        result = facade.start();
                    }

                    break;
                }
            }
        } catch (Throwable e) {
            operationError = e;
        }

        OperationServicesResultCode code;
        if (result != null) {
            code = result.getResultCode();
        } else {
            // If the server is not running, the STOPIFRUNNING case won't execute something and we won't have a result.
            // Assume that's a success and continue.            
            code = (operationError == null) ? OperationServicesResultCode.SUCCESS : OperationServicesResultCode.FAILURE;
        }

        switch (code) {
            case SUCCESS: {
                complete(executionContext, "Successfully called [" + actionName + "] action on server.");
                break;
            }

            case FAILURE: {
                error(executionContext, operationError, "Failed calling [" + actionName + "] action on server.",
                    TRANSITION_ERROR);
                break;
            }

            case TIMED_OUT: {
                error(executionContext, operationError, "Action [" + actionName +
                    "] timed out while executing on server.", TRANSITION_ERROR);
                break;
            }
        }
    }

    public String getDescription() {
        return "Carry out [" + actionName + "] action on the server.";
    }

    protected void checkProperties() throws ActionHandlerException {
        HandlerUtils.checkIsSet("actionName", actionName);
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    /**
     * These correspond to the possible actions returned as part of the workflow. These should be an uppercase exact
     * match of the action name in the workflow. This allows for simple lookup and switching to determine which action
     * to execute.
     */
    private enum ControlActions {
        START, STOP, STOPIFRUNNING, RESTART
    }
}