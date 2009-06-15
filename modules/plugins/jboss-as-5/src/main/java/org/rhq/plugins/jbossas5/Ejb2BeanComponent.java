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
package org.rhq.plugins.jbossas5;

import java.util.Set;

import org.jboss.managed.api.ManagedProperty;
import org.jboss.metatype.api.values.CompositeValue;
import org.jboss.metatype.api.values.SimpleValue;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * 
 * @author Lukas Krejci
 */
public class Ejb2BeanComponent extends ManagedComponentComponent implements OperationFacet {

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        super.getValues(report, metrics);
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if ("methodStats".equals(name)) {
            OperationResult result = new OperationResult();
            PropertyList methodList = new PropertyList("methods");
            result.getComplexResults().put(methodList);

            ManagedProperty stats = this.getManagedComponent().getProperty("DetypedInvocationStatistics");
            CompositeValue methodsStats = (CompositeValue) ((CompositeValue) stats.getValue()).get("methodStats");

            Set<String> methodNames = methodsStats.getMetaType().keySet();

            for (String methodName : methodNames) {
                CompositeValue methodStats = (CompositeValue) methodsStats.get(methodName);

                long count = Long.parseLong(((SimpleValue) methodStats.get("count")).getValue().toString());
                long totalTime = Long.parseLong(((SimpleValue) methodStats.get("totalTime")).getValue().toString());
                long minTime = Long.parseLong(((SimpleValue) methodStats.get("minTime")).getValue().toString());
                long maxTime = Long.parseLong(((SimpleValue) methodStats.get("maxTime")).getValue().toString());

                PropertyMap method = new PropertyMap("method", new PropertySimple("methodName", methodName),
                    new PropertySimple("count", count), new PropertySimple("minTime", minTime), new PropertySimple(
                        "maxTime", maxTime), new PropertySimple("totalTime", totalTime));
                methodList.add(method);
            }

            return result;
        }

        return super.invokeOperation(name, parameters);
    }
}
