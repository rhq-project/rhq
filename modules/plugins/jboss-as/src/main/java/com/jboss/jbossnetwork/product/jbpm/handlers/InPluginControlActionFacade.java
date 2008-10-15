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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.operation.OperationServicesResult;
import org.rhq.core.pluginapi.operation.OperationServicesResultCode;
import org.rhq.plugins.jbossas.JBossASServerComponent;
import org.rhq.plugins.jbossas.JBossASServerOperationsDelegate;
import org.rhq.plugins.jbossas.JBossASServerSupportedOperations;

/**
 * Implementation of the {@link ControlActionFacade} that makes direct calls within the AS plugin. This implementation
 * does not provide any means for timing out the operation invocation. Ideally, the
 * {@link PluginContainerControlActionFacade} will be used, providing all the plugin container framework for the
 * potentially dangerous calls to start/stop the AS instance. This implementation circumvents any of the plugin
 * container code in favor of simply reusing the start/stop implementations elsewhere in the plugin.
 *
 * @author Jason Dobies
 */
public class InPluginControlActionFacade implements ControlActionFacade {

    private JBossASServerComponent serverComponent;
    private JBossASServerOperationsDelegate operationsDelegate;

    private final Log log = LogFactory.getLog(this.getClass());

    public InPluginControlActionFacade(JBossASServerComponent serverComponent) {
        this.serverComponent = serverComponent;
        operationsDelegate = serverComponent.getOperationsDelegate();
    }

    public OperationServicesResult start() {
        try {
            operationsDelegate.invoke(JBossASServerSupportedOperations.START, null);
        } catch (InterruptedException e) {
            log.error("Start interrupted", e);
        }

        OperationServicesResultCode code;

        if (serverComponent.getAvailability() == AvailabilityType.UP)
            code = OperationServicesResultCode.SUCCESS;
        else
            code = OperationServicesResultCode.FAILURE;

        OperationServicesResult result = new OperationServicesResult(code);

        return result;
    }

    public OperationServicesResult stop() {
        try {
            operationsDelegate.invoke(JBossASServerSupportedOperations.SHUTDOWN, null);
        } catch (InterruptedException e) {
            log.error("Shutdown interrupted", e);
        }

        OperationServicesResultCode code;

        if (serverComponent.getAvailability() == AvailabilityType.DOWN)
            code = OperationServicesResultCode.SUCCESS;
        else
            code = OperationServicesResultCode.FAILURE;

        OperationServicesResult result = new OperationServicesResult(code);

        return result;
    }

    public boolean isRunning() {
        return serverComponent.getAvailability() == AvailabilityType.UP;
    }
}
