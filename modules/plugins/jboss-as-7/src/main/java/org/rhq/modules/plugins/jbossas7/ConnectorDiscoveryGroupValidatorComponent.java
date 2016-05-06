/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7;

import static org.rhq.core.domain.configuration.ConfigurationUpdateStatus.FAILURE;
import static org.rhq.core.util.StringUtil.EMPTY_STRING;
import static org.rhq.core.util.StringUtil.isNotBlank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.rhq.modules.plugins.jbossas7.json.UndefineAttribute;
import org.rhq.modules.plugins.jbossas7.json.WriteAttribute;

/**
 * Resource backed by this component must define either a discovery group name <strong>or</strong> a connector. Resource
 * configuration update must happen in two steps:
 * <ol>
 *     <li>execute a composite operation to set one of these two attributes and unset the other one</li>
 *     <li>use the standard {@link ConfigurationWriteDelegate} for all other attributes</li>
 * </ol>
 *
 * @author Stefan Negrea
 */
public class ConnectorDiscoveryGroupValidatorComponent extends BaseComponent<ResourceComponent<?>> {
    private static final String DISCOVERY_GROUP_NAME = "discovery-group-name";

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        ResourceType resourceType = context.getResourceType();
        String resourceTypeName = resourceType.getName();
        resourceTypeName = BaseComponent.resourceTypeNameByRemovingProfileSuffix(resourceTypeName);

        ConfigurationUpdateHelper configurationUpdateHelper;
        if (resourceTypeName.equals("Connection Factory") || resourceTypeName.equals("Pooled Connection Factory")) {
            configurationUpdateHelper = new ConnectionFactoriesConfigurationUpdateHelper(getAddress(),
                report.getConfiguration());
        } else if (resourceTypeName.equals("Bridge") || resourceTypeName.equals("Cluster Connection")) {
            configurationUpdateHelper = new DefaultConfigurationUpdateHelper(address, report.getConfiguration());
        } else {
            report.setStatus(FAILURE);
            report.setErrorMessage(resourceType + " not supported");
            return;
        }

        if (!configurationUpdateHelper.isConnectorXorDiscoveryGroupNameConfigured()) {
            report.setStatus(FAILURE);
            report.setErrorMessage(configurationUpdateHelper.getErrorMessage());
        } else {
            // First update the connector and discovery group name attributes in a batch
            Operation batchOperation = configurationUpdateHelper.getBatchOperation();
            Result result = getASConnection().execute(batchOperation);
            if (!result.isSuccess()) {
                report.setStatus(FAILURE);
                report.setErrorMessage(result.getFailureDescription());
                return;
            }

            // Now update the rest of the configuration attributes
            ConfigurationDefinition configDefCopy = resourceType.getResourceConfigurationDefinition().copy();
            configDefCopy.getPropertyDefinitions().remove(DISCOVERY_GROUP_NAME);
            configDefCopy.getPropertyDefinitions().remove(configurationUpdateHelper.getConnectorPropertyName());
            ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDefCopy, getASConnection(),
                address);
            delegate.updateResourceConfiguration(report);
        }
    }

    private static abstract class ConfigurationUpdateHelper {
        final Configuration configuration;
        final boolean hasDiscoveryGroupName;
        final boolean hasConnector;
        final boolean hasNoneOfTheAlternatives;
        final String errorMessage;

        ConfigurationUpdateHelper(Configuration configuration) {
            this.configuration = configuration;
            hasDiscoveryGroupName = isNotBlank(this.configuration.getSimpleValue(DISCOVERY_GROUP_NAME));
            hasConnector = isConnectorPropertyDefined();
            hasNoneOfTheAlternatives = hasDiscoveryGroupName == hasConnector && hasConnector == false;
            errorMessage = getBeginningOfErrorMessage()
                + (hasNoneOfTheAlternatives ? " You provided none." : " You provided both.");
        }

        abstract String getConnectorPropertyName();

        abstract boolean isConnectorPropertyDefined();

        abstract String getBeginningOfErrorMessage();

        boolean isConnectorXorDiscoveryGroupNameConfigured() {
            return hasDiscoveryGroupName != hasConnector;
        }

        String getErrorMessage() {
            return errorMessage;
        }

        public abstract Operation getBatchOperation();

        boolean hasDiscoveryGroupName() {
            return hasDiscoveryGroupName;
        }
    }

    private static class ConnectionFactoriesConfigurationUpdateHelper extends ConfigurationUpdateHelper {
        static final String CONNECTOR_ATTRIBUTE = "connector";
        static final String CONNECTOR_PROPERTY = CONNECTOR_ATTRIBUTE + ":collapsed";

        Address address;

        ConnectionFactoriesConfigurationUpdateHelper(Address address, Configuration configuration) {
            super(configuration);
            this.address = address;
        }

        @Override
        String getConnectorPropertyName() {
            return CONNECTOR_PROPERTY;
        }

        @Override
        boolean isConnectorPropertyDefined() {
            PropertyMap connector = configuration.getMap(CONNECTOR_PROPERTY);
            if (connector != null) {
                return isNotBlank(connector.getSimpleValue("name:0", EMPTY_STRING));
            }
            return false;
        }

        @Override
        String getBeginningOfErrorMessage() {
            return "You need to provide either a " + CONNECTOR_ATTRIBUTE + " name OR a discovery-group-name.";
        }

        @Override
        public Operation getBatchOperation() {
            CompositeOperation compositeOperation = new CompositeOperation();
            if (hasDiscoveryGroupName()) {
                compositeOperation.addStep(new UndefineAttribute(address, CONNECTOR_ATTRIBUTE));
                compositeOperation.addStep(new WriteAttribute(address, DISCOVERY_GROUP_NAME, configuration
                    .getSimpleValue(DISCOVERY_GROUP_NAME)));
            } else {
                compositeOperation.addStep(new UndefineAttribute(address, DISCOVERY_GROUP_NAME));
                compositeOperation.addStep(new WriteAttribute(address, CONNECTOR_ATTRIBUTE, Collections.singletonMap(
                    configuration.getMap(CONNECTOR_PROPERTY).getSimpleValue("name:0", EMPTY_STRING), null)));
            }
            return compositeOperation;
        }
    }

    private static class DefaultConfigurationUpdateHelper extends ConfigurationUpdateHelper {
        static final String STATIC_CONNECTORS_ATTRIBUTE = "static-connectors";
        static final String STATIC_CONNECTORS_PROPERTY = STATIC_CONNECTORS_ATTRIBUTE + ":nullable";

        final Address address;

        public DefaultConfigurationUpdateHelper(Address address, Configuration configuration) {
            super(configuration);
            this.address = address;
        }

        @Override
        String getConnectorPropertyName() {
            return STATIC_CONNECTORS_PROPERTY;
        }

        @Override
        boolean isConnectorPropertyDefined() {
            PropertyList staticConnectors = configuration.getList(STATIC_CONNECTORS_PROPERTY);
            return staticConnectors != null && !staticConnectors.getList().isEmpty();
        }

        @Override
        String getBeginningOfErrorMessage() {
            return "You need to provide either static connectors name OR a discovery-group-name.";
        }

        @Override
        public Operation getBatchOperation() {
            CompositeOperation compositeOperation = new CompositeOperation();
            if (hasDiscoveryGroupName()) {
                compositeOperation.addStep(new UndefineAttribute(address, STATIC_CONNECTORS_ATTRIBUTE));
                compositeOperation.addStep(new WriteAttribute(address, DISCOVERY_GROUP_NAME, configuration
                    .getSimpleValue(DISCOVERY_GROUP_NAME)));
            } else {
                compositeOperation.addStep(new UndefineAttribute(address, DISCOVERY_GROUP_NAME));
                List<Property> propertyList = configuration.getList(STATIC_CONNECTORS_PROPERTY).getList();
                List<String> staticConnectors = new ArrayList<String>(propertyList.size());
                for (Property property : propertyList) {
                    if (property instanceof PropertySimple) {
                        PropertySimple propertySimple = (PropertySimple) property;
                        staticConnectors.add(propertySimple.getStringValue());
                    } else {
                        getLog().warn(property.getName() + " property has unexpected type: " + property.getClass());
                    }
                }
                compositeOperation.addStep(new WriteAttribute(address, STATIC_CONNECTORS_ATTRIBUTE, staticConnectors));
            }
            return compositeOperation;
        }
    }
}
