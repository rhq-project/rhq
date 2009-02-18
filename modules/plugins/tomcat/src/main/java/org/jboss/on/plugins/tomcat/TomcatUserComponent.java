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

package org.jboss.on.plugins.tomcat;

import java.util.Arrays;
import java.util.Set;

import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * Manage a Tomcat User
 * 
 * @author Jay Shaughnessy
 */
public class TomcatUserComponent extends MBeanResourceComponent<TomcatUserDatabaseComponent> implements DeleteResourceFacet {

    public static final String PROPERTY_FULL_NAME = "fullName";
    public static final String PROPERTY_PASSWORD = "password";
    public static final String PROPERTY_USERNAME = "username";
    public static final String RESOURCE_TYPE_NAME = "Tomcat User";

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) {
        TomcatUserDatabaseComponent parentComponent = getResourceContext().getParentResourceComponent();
        parentComponent.getEmsConnection(); // first make sure the connection is loaded

        for (MeasurementScheduleRequest request : metrics) {
            String name = request.getName();

            String attributeName = name.substring(name.lastIndexOf(':') + 1);

            try {
                EmsAttribute attribute = getEmsBean().getAttribute(attributeName);

                Object valueObject = attribute.refresh();

                // currently all metrics are traits so we can make the assumption
                String[] vals = (String[]) valueObject;
                MeasurementDataTrait mdt = new MeasurementDataTrait(request, Arrays.toString(vals));
                report.addData(mdt);
            } catch (Exception e) {
                log.error("Failed to obtain measurement [" + name + "]", e);
            }
        }
    }

    public void deleteResource() throws Exception {
        Configuration opConfig = new Configuration();
        ResourceContext<TomcatUserDatabaseComponent> resourceContext = getResourceContext();
        opConfig.put(resourceContext.getPluginConfiguration().getSimple(PROPERTY_USERNAME));
        resourceContext.getParentResourceComponent().invokeOperation("removeUser", opConfig);
    }

}
