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
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;

/**
 * Component class for the JMS subsystem
 * @author Heiko W. Rupp
 */
public class JmsComponent extends DomainComponent {

    private final Log log = LogFactory.getLog(JmsComponent.class);

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {


        Configuration pConf = report.getPluginConfiguration();
        Configuration resConf = report.getResourceConfiguration();
        ConfigurationDefinition resConfDef = report.getResourceType().getResourceConfigurationDefinition();

        String type = pConf.getSimpleValue("path", "");

        List<PROPERTY_VALUE> address = pathToAddress(getPath());
        address.add(new PROPERTY_VALUE(type,report.getUserSpecifiedResourceName()));
        Operation op = new Operation("add",address);

        // Loop over the properties from the config and add them as properties to the op
        for (Map.Entry<String, Property> entry:  resConf.getAllProperties().entrySet()) {
            Property value = entry.getValue();
            if (value !=null) {
                String name = entry.getKey();


                if (value instanceof PropertySimple) {
                    PropertyDefinitionSimple propDef = (PropertyDefinitionSimple) resConfDef.get(name);
                    PropertySimple ps = (PropertySimple) value;
                    op.addAdditionalProperty(name, getObjectForProperty(ps,propDef));
                } else if (value instanceof PropertyList) {
                    PropertyList propertyList = (PropertyList) value;
                    List<Object> list = new ArrayList<Object>();
                    PropertyDefinitionList pd = resConfDef.getPropertyDefinitionList(name);
                    PropertyDefinitionSimple propDef = (PropertyDefinitionSimple) pd.getMemberDefinition();
                    for (Property p : propertyList.getList()) {

                        Object o = getObjectForProperty((PropertySimple) p, propDef);
                        list.add(o);
                    }
                    op.addAdditionalProperty(name,list);
                }
            }
        }
        ComplexResult res = (ComplexResult) getASConnection().execute(op,true);

        // TODO Currently this reports a failure even if it succeeds for jms

        if (res == null || !res.isSuccess()) {
            report.setStatus(CreateResourceStatus.FAILURE);
        } else {
            report.setStatus(CreateResourceStatus.SUCCESS);
            report.setResourceKey(address.toString());
            report.setResourceName(report.getUserSpecifiedResourceName());
        }

        System.out.println(report);
        return report;
    }

}
