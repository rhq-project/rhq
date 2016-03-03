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
package org.rhq.modules.plugins.wildfly10;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;

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
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.Operation;
import org.rhq.modules.plugins.wildfly10.json.Result;

public class CreateResourceDelegate extends ConfigurationWriteDelegate implements CreateChildResourceFacet {

    public CreateResourceDelegate(ConfigurationDefinition configDef, ASConnection connection, Address address) {
        super(configDef, connection, address);
        createChildRequested = true;
    }

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {
        report.setStatus(CreateResourceStatus.INVALID_CONFIGURATION);
        Address createAddress = new Address(this.address);

        String path = report.getPluginConfiguration().getSimpleValue("path", "");
        String resourceName;
        if (!path.contains("=")) {
            //this is not a singleton subsystem
            //resources like  example=test1 and example=test2 can be created
            resourceName = report.getUserSpecifiedResourceName();
        } else {
            //this is a request to create a true singleton subsystem
            //both the path and the name are set at resource level configuration
            resourceName = path.substring(path.indexOf('=') + 1);
            path = path.substring(0, path.indexOf('='));
        }

        createAddress.add(path, resourceName);

        Operation op = new Operation("add", createAddress);
        for (Property prop : report.getResourceConfiguration().getProperties()) {
            SimpleEntry<String, ?> entry = null;

            boolean isEntryEligible = true;
            if (prop instanceof PropertySimple) {
                PropertySimple propertySimple = (PropertySimple) prop;
                PropertyDefinitionSimple propertyDefinition = this.configurationDefinition
                    .getPropertyDefinitionSimple(propertySimple.getName());

                if (propertyDefinition == null
                    || (!propertyDefinition.isRequired() && propertySimple.getStringValue() == null)) {
                    isEntryEligible = false;
                } else {
                    entry = preparePropertySimple(propertySimple, propertyDefinition);
                }
            } else if (prop instanceof PropertyList) {
                PropertyList propertyList = (PropertyList) prop;
                PropertyDefinitionList propertyDefinition = this.configurationDefinition
                    .getPropertyDefinitionList(propertyList.getName());

                if (!propertyDefinition.isRequired() && propertyList.getList().size() == 0) {
                    isEntryEligible = false;
                } else {
                    entry = preparePropertyList(propertyList, propertyDefinition);
                }
            } else if (prop instanceof PropertyMap) {
                PropertyMap propertyMap = (PropertyMap) prop;
                PropertyDefinitionMap propertyDefinition = this.configurationDefinition
                    .getPropertyDefinitionMap(propertyMap.getName());

                if (!propertyDefinition.isRequired() && propertyMap.getMap().size() == 0) {
                    isEntryEligible = false;
                } else {
                    entry = preparePropertyMap(propertyMap, propertyDefinition);
                    isEntryEligible = !((Map<String, Object>) entry.getValue()).isEmpty();
                }
            }

            if (isEntryEligible) {
                op.addAdditionalProperty(entry.getKey(), entry.getValue());
            }
        }

        Result result = this.connection.execute(op);
        if (result.isSuccess()) {
            report.setStatus(CreateResourceStatus.SUCCESS);
            report.setResourceKey(createAddress.getPath());
            report.setResourceName(report.getUserSpecifiedResourceName());
        } else {
            report.setStatus(CreateResourceStatus.FAILURE);
            report.setErrorMessage(result.getFailureDescription());
        }

        return report;
    }
}
