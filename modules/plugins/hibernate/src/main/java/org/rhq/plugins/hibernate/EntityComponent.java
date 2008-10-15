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
package org.rhq.plugins.hibernate;

import java.util.Set;
import org.mc4j.ems.connection.bean.operation.EmsOperation;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * @author Greg Hinkle
 */
public class EntityComponent extends MBeanResourceComponent<MBeanResourceComponent> {
    private ResourceContext<MBeanResourceComponent> context;

    @Override
    public void start(ResourceContext<MBeanResourceComponent> context) {
        this.context = context;
        this.bean = context.getParentResourceComponent().getEmsBean();
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        EmsOperation operation = getEmsBean().getOperation("getEntityStatistics");
        Object entityStatistics = operation.invoke(context.getResourceKey());

        for (MeasurementScheduleRequest request : requests) {
            Object val = super.lookupAttributeProperty(entityStatistics, request.getName());

            report.addData(new MeasurementDataNumeric(request, ((Number) val).doubleValue()));
        }
    }
}