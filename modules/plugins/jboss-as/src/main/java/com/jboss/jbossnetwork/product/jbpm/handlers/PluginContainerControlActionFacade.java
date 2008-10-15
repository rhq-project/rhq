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

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationServices;
import org.rhq.core.pluginapi.operation.OperationServicesResult;
import org.rhq.plugins.jbossas.JBossASServerComponent;

/**
 * Implementation of the {@link ControlActionFacade} that connects to the plugin container to perform its calls.
 *
 * @author Jason Dobies
 */
public class PluginContainerControlActionFacade implements ControlActionFacade {
    /**
     * Time, in seconds, passed to the operation services to wait before timing out.
     */
    private static final long CONTROL_ACTION_TIMEOUT = 60 * 10;

    private OperationContext operationContext;
    private OperationServices operationServices;
    private JBossASServerComponent serverComponent;

    public PluginContainerControlActionFacade(OperationContext operationContext, JBossASServerComponent serverComponent) {
        this.operationContext = operationContext;
        this.serverComponent = serverComponent;
        this.operationServices = operationContext.getOperationServices();
    }

    public OperationServicesResult start() {
        OperationServicesResult result = operationServices.invokeOperation(operationContext, "start", null,
            CONTROL_ACTION_TIMEOUT);
        return result;
    }

    public OperationServicesResult stop() {
        if (!isRunning()) {
            throw new IllegalStateException("The server is not running.");
        }

        OperationServicesResult result = operationServices.invokeOperation(operationContext, "shutdown_via_jmx", null,
            CONTROL_ACTION_TIMEOUT);
        return result;
    }

    public boolean isRunning() {
        return serverComponent.getAvailability() == AvailabilityType.UP;
    }
}