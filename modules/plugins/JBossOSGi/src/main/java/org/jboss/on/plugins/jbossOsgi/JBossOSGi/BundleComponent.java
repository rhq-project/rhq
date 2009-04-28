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
package org.jboss.on.plugins.jbossOsgi.JBossOSGi;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.bean.operation.EmsOperation;
import static org.osgi.framework.Bundle.ACTIVE;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * Component class for a single OSGi Bundle
 *
 * @author Heiko W. Rupp
 */
public class BundleComponent implements ResourceComponent<JBossOsgiServerComponent>, OperationFacet, MeasurementFacet {

    private final Log log = LogFactory.getLog(BundleComponent.class);

    ResourceContext context;
    JBossOsgiServerComponent parentComponent;

    public void start(ResourceContext<JBossOsgiServerComponent> resourceContext) throws Exception {
        context = resourceContext;
        parentComponent = resourceContext.getParentResourceComponent();
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {

        AvailabilityType res = AvailabilityType.DOWN;

        EmsBean bean = getBean();
        EmsAttribute att = bean.getAttribute("state");
        try {
            Integer code = (Integer) att.refresh();
            if (code != null && code == ACTIVE)
                res = AvailabilityType.UP;
        }
        catch (Exception e) {
            // Nothing to do here
        }

        return res;
    }


    public OperationResult invokeOperation(String name, Configuration params) throws  Exception {

        EmsBean bean = getBean();

        Object[] para ;
        if ("getProperty".equals(name)) {
            para = new Object[1];
            para[0] = params.getSimple("name").getStringValue();
        }
        else {
            para = new Object[0];
        }
        if (log.isDebugEnabled())
            log.debug("Trying to invoke operation [" + name + "] on [" + context.getResourceKey() + "]");
        EmsOperation ops = bean.getOperation(name);
        Object res = null;
        if (ops!=null) {
            res = ops.invoke(para);

        }
        OperationResult result = new OperationResult();
        if (res!=null)
            result.setSimpleResult((String) res); // TODO cast will be bad for other return types than string

        return result;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

        EmsBean bean = getBean();
        List<String> theMetrics = new ArrayList<String>(metrics.size());
        for (MeasurementScheduleRequest msr : metrics)
            theMetrics.add(msr.getName());

        bean.refreshAttributes(theMetrics);

        for (MeasurementScheduleRequest msr : metrics) {
            if (msr.getDataType() == DataType.MEASUREMENT) {
                Double value = (Double) bean.getAttribute(msr.getName()).getValue();
                MeasurementDataNumeric data = new MeasurementDataNumeric(msr,value);
                report.addData(data);
            }
            else if (msr.getDataType() == DataType.TRAIT) {
                String value = (String) bean.getAttribute(msr.getName()).getValue();
                MeasurementDataTrait data = new MeasurementDataTrait(msr,value);
                report.addData(data);
            }
        }
    }



    private EmsBean getBean() {
        EmsConnection conn = parentComponent.getEmsConnection();
        return conn.getBean(context.getResourceKey());
    }

}
