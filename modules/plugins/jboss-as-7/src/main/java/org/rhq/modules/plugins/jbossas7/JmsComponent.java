/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Component class for the JMS subsystem
 * @author Heiko W. Rupp
 */
public class JmsComponent extends DomainComponent {

    private final Log log = LogFactory.getLog(JmsComponent.class);

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {


        Configuration resConf = report.getResourceConfiguration();
        Configuration pConf = report.getPluginConfiguration();

        String type = pConf.getSimpleValue("path", "");

        List<PROPERTY_VALUE> address = pathToAddress(getPath());
        address.add(new PROPERTY_VALUE(type,report.getUserSpecifiedResourceName()));
        Operation op = new Operation("add",address);
        for (Map.Entry<String, Property> entry:  resConf.getAllProperties().entrySet()) {
            Property value = entry.getValue();
            if (value !=null) {

                if (value instanceof PropertySimple) {
                    PropertySimple ps = (PropertySimple) value;
                    op.addAdditionalProperty(entry.getKey(), ps.getStringValue()); // TODO determine real type
                } else if (value instanceof PropertyList) {
                    PropertyList propertyList = (PropertyList) value;
                    List<String> list = new ArrayList<String>();
                    for (Property p : propertyList.getList()) {
                        list.add(p.toString()); // TODO
                    }
                    op.addAdditionalProperty(entry.getKey(),list);
                }
            }
        }
        ComplexResult res = (ComplexResult) getASConnection().execute(op,true);

        if (res == null || !res.isSuccess()) {
            report.setStatus(CreateResourceStatus.FAILURE);
        } else {
            report.setStatus(CreateResourceStatus.SUCCESS);
            report.setResourceKey(address.toString()); // TODO ??
            report.setResourceName(report.getUserSpecifiedResourceName());
        }

        System.out.println(report);
        return report;
    }
}
