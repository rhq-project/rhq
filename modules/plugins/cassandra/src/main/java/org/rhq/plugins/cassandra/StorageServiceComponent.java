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
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * @author John Sanda
 */
public class StorageServiceComponent extends MBeanResourceComponent {

    private Log log = LogFactory.getLog(StorageServiceComponent.class);

    @Override
    public AvailabilityType getAvailability() {
        ResourceContext context = getResourceContext();
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
        if (name.equals("takeKeyspaceSnapshot")) {
            return takeSnapshot(parameters);
        }

        if (name.equals("setLog4jLevel")) {
            return setLog4jLevel(parameters);
        }

        return super.invokeOperation(name, parameters);
    }

    private OperationResult takeSnapshot(Configuration params) {
        EmsBean emsBean = getEmsBean();
        EmsOperation operation = emsBean.getOperation("takeSnapshot", String.class, String[].class);

        String snapshotName = params.getSimpleValue("snapshotName");
        String keyspaceName = params.getSimpleValue("keyspaceName");

        operation.invoke(snapshotName, new String[] {keyspaceName});

        return new OperationResult();
    }

    private OperationResult setLog4jLevel(Configuration params) {
        EmsBean emsBean = getEmsBean();
        EmsOperation operation = emsBean.getOperation("setLog4jLevel", String.class, String.class);

        String classQualifier = params.getSimpleValue("classQualifier");
        String level = params.getSimpleValue("level");

        operation.invoke(classQualifier, level);

        return new OperationResult();
    }

    @Override
    public Configuration loadResourceConfiguration() {
        Configuration config = super.loadResourceConfiguration();
        EmsBean emsBean = getEmsBean();
        EmsAttribute attribute = emsBean.getAttribute("AllDataFileLocations");

        PropertyList list = new PropertyList("dataFileLocations");
        String[] dirs = (String[]) attribute.getValue();
        for (String dir : dirs) {
            list.add(new PropertySimple("directory", dir));
        }
        config.put(list);

        return config;
    }
}
