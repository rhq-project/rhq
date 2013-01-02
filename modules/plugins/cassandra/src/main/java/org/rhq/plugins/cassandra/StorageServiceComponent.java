/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.plugins.cassandra;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UNKNOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.bean.operation.EmsOperation;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * @author John Sanda
 */
public class StorageServiceComponent extends ComplexConfigurationResourceComponent {

    private Log log = LogFactory.getLog(StorageServiceComponent.class);

    @Override
    public AvailabilityType getAvailability() {
        ResourceContext<?> context = getResourceContext();
        try {
            EmsBean emsBean = loadBean();
            if (emsBean == null) {
                log.warn("Unable to establish JMX connection to " + context.getResourceKey());
                return DOWN;
            }

            AvailabilityType availability = UP;

            EmsAttribute thriftEnabledAttr = emsBean.getAttribute("RPCServerRunning");
            Boolean thriftEnabled = (Boolean) thriftEnabledAttr.getValue();

            if (!thriftEnabled) {
                if (log.isWarnEnabled()) {
                    log.warn("Thrift RPC server is disabled for " + context.getResourceKey());
                }
                availability = DOWN;
            }

            EmsAttribute initializedAttr = emsBean.getAttribute("Initialized");
            Boolean initialized = (Boolean) initializedAttr.getValue();

            if (!initialized) {
                if (log.isWarnEnabled()) {
                    log.warn(context.getResourceKey() + " is not initialized");
                }
                availability = DOWN;
            }

            return availability;
        } catch (Exception e) {
            log.error("Unable to determine availability for " + context.getResourceKey(), e);
            return UNKNOWN;
        }
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if (name.equals("takeSnapshot")) {
            return takeSnapshot(parameters);
        } else if (name.equals("setLog4jLevel")) {
            return setLog4jLevel(parameters);
        }

        return super.invokeOperation(name, parameters);
    }

    private OperationResult takeSnapshot(Configuration parameters) {
        EmsBean emsBean = getEmsBean();
        EmsOperation operation = emsBean.getOperation("takeSnapshot", String.class, String[].class);
        String snapshotName = parameters.getSimpleValue("snapshotName");
        if (snapshotName == null || snapshotName.trim().isEmpty()) {
            snapshotName = System.currentTimeMillis() + "";
        }

        operation.invoke(snapshotName, new String[] {});

        return new OperationResult();
    }

    private OperationResult setLog4jLevel(Configuration parameters) {
        EmsBean emsBean = getEmsBean();
        EmsOperation operation = emsBean.getOperation("setLog4jLevel", String.class, String.class);

        String classQualifier = parameters.getSimpleValue("classQualifier");
        String level = parameters.getSimpleValue("level");

        operation.invoke(classQualifier, level);

        return new OperationResult();
    }
}
