/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5.helper;

import com.jboss.jbossnetwork.product.jbpm.handlers.ControlActionFacade;
import com.jboss.jbossnetwork.product.jbpm.handlers.PluginContainerControlActionFacade;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.operation.OperationServicesResult;
import org.rhq.core.pluginapi.operation.OperationServicesResultCode;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.plugins.jbossas5.ApplicationServerSupportedOperations;
import org.rhq.plugins.jbossas5.ApplicationServerComponent;

/**
 * Implementation of the {@link ControlActionFacade} that makes direct calls within the AS5 plugin. This implementation
 * does not provide any means for timing out the operation invocation. Ideally, the
 * {@link PluginContainerControlActionFacade} will be used, providing all the plugin container framework for the
 * potentially dangerous calls to start/stop the AS5 instance. This implementation circumvents any of the plugin
 * container code in favor of simply reusing the start/stop implementations elsewhere in the plugin.
 *
 * @author Jason Dobies
 * @author Ian Springer
 */
public class InPluginControlActionFacade implements ControlActionFacade {

    private ApplicationServerComponent serverComponent;

    private final Log log = LogFactory.getLog(this.getClass());

    public InPluginControlActionFacade(ApplicationServerComponent serverComponent) {
        this.serverComponent = serverComponent;
    }

    public OperationServicesResult start() {
        String operationName = ApplicationServerSupportedOperations.START.name().toLowerCase();
        try {
            this.serverComponent.invokeOperation(operationName, null);
        } catch (InterruptedException e) {
            log.error("Start interrupted.", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        OperationServicesResultCode code;

        if (serverComponent.getAvailability() == AvailabilityType.UP) {
            code = OperationServicesResultCode.SUCCESS;
        } else {
            code = OperationServicesResultCode.FAILURE;
        }

        OperationServicesResult result = new OperationServicesResult(code);

        return result;
    }

    public OperationServicesResult stop() {
        String operationName = ApplicationServerSupportedOperations.SHUTDOWN.name().toLowerCase();
        try {
            this.serverComponent.invokeOperation(operationName, null);
        } catch (InterruptedException e) {
            log.error("Shutdown interrupted.", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        OperationServicesResultCode code;

        if (this.serverComponent.getAvailability() == AvailabilityType.DOWN) {
            code = OperationServicesResultCode.SUCCESS;
        } else {
            code = OperationServicesResultCode.FAILURE;
        }

        OperationServicesResult result = new OperationServicesResult(code);

        return result;
    }

    public boolean isRunning() {
        return (this.serverComponent.getAvailability() == AvailabilityType.UP);
    }
}
