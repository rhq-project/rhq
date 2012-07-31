/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.plugins.hadoop;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.JMXServerComponent;

public class HadoopServiceComponent extends JMXServerComponent<ResourceComponent<?>> implements
    JMXComponent<ResourceComponent<?>>, MeasurementFacet, OperationFacet {
    
    private static final Log LOG = LogFactory.getLog(HadoopServiceComponent.class);

    /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    @Override
    public AvailabilityType getAvailability() {
        return getResourceContext().getNativeProcess().isRunning() ? AvailabilityType.UP: AvailabilityType.DOWN;
    }

    @Override
    public EmsConnection getEmsConnection() {
        EmsConnection conn = super.getEmsConnection(); // TODO: Customise this generated block
        if (LOG.isTraceEnabled()) {
            LOG.trace("EmsConnection is " + conn.toString());
        }
        return conn;

    }

    /**
     * Gather measurement data
     *  @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

        for (MeasurementScheduleRequest req : metrics) {
            String property = req.getName();
            String props[] = property.split("\\|");

            EmsConnection conn = getEmsConnection();
            EmsBean bean = conn.getBean(props[0]);
            if (bean != null) {
                bean.refreshAttributes();
                EmsAttribute att = bean.getAttribute(props[1]);
                if (att != null) {
                    Long val = (Long) att.getValue(); // TODO check for real type

                    MeasurementDataNumeric res = new MeasurementDataNumeric(req, Double.valueOf(val));
                    report.addData(res);
                } else
                    LOG.warn("Attribute " + props[1] + " not found");
            } else
                LOG.warn("MBean " + props[0] + " not found");
        }
    }

    /**
     * Invokes the passed operation on the managed resource
     * @param name Name of the operation
     * @param params The method parameters
     * @return An operation result
     * @see org.rhq.core.pluginapi.operation.OperationFacet
     */
    public OperationResult invokeOperation(String name, Configuration params) throws Exception {

        OperationResult res = new OperationResult();
        if ("dummyOperation".equals(name)) {
            // TODO implement me

        }
        return res;
    }

}
