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

import java.util.AbstractMap.SimpleEntry;

import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

public class CreateResourceDelegate extends ConfigurationWriteDelegate implements CreateChildResourceFacet {

    public CreateResourceDelegate(ConfigurationDefinition configDef, ASConnection connection, Address address) {
        super(configDef, connection, address);
    }

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {
        report.setStatus(CreateResourceStatus.INVALID_CONFIGURATION);
        Address createAddress = new Address(this.getAddress());
        createAddress.add(report.getPluginConfiguration().getSimpleValue("path", ""),
            report.getUserSpecifiedResourceName());

        Operation op = new Operation("add", createAddress);
        for (Property prop : report.getResourceConfiguration().getProperties()) {
            SimpleEntry<String, ?> entry = null;

            if (prop instanceof PropertySimple) {
                PropertySimple propertySimple = (PropertySimple) prop;
                PropertyDefinitionSimple propertyDefinition = this.getConfigurationDefinition()
                    .getPropertyDefinitionSimple(propertySimple.getName());
                entry = preparePropertySimple(propertySimple, propertyDefinition);
            } else if (prop instanceof PropertyList) {
                PropertyList propertyList = (PropertyList) prop;
                PropertyDefinitionList propertyDefinition = this.getConfigurationDefinition()
                    .getPropertyDefinitionList(propertyList.getName());
                entry = preparePropertyList(propertyList, propertyDefinition);
            } else if (prop instanceof PropertyMap) {
                PropertyMap propertyMap = (PropertyMap) prop;
                PropertyDefinitionMap propertyDefinition = this.getConfigurationDefinition().getPropertyDefinitionMap(
                    propertyMap.getName());
                entry = preparePropertyMap(propertyMap, propertyDefinition);
            }

            op.addAdditionalProperty(entry.getKey(), entry.getValue());
        }

        Result result = this.getConnection().execute(op);
        if (result.isSuccess()) {
            report.setStatus(CreateResourceStatus.SUCCESS);
            report.setResourceKey(this.getAddress().getPath());
            report.setResourceName(report.getUserSpecifiedResourceName());
        } else {
            report.setStatus(CreateResourceStatus.FAILURE);
            report.setErrorMessage(result.getFailureDescription());
        }

        return report;
    }
}
