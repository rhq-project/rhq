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
package org.rhq.plugins.jbossas5.serviceBinding;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.managed.api.RunState;
import org.jboss.metatype.api.types.CollectionMetaType;
import org.jboss.metatype.api.types.ImmutableCompositeMetaType;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.values.CollectionValue;
import org.jboss.metatype.api.values.CollectionValueSupport;
import org.jboss.metatype.api.values.CompositeValue;
import org.jboss.metatype.api.values.MapCompositeValueSupport;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.SimpleValue;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.plugins.jbossas5.ManagedComponentComponent;
import org.rhq.plugins.jbossas5.serviceBinding.Util.PropertyDefinition;

/**
 * Manager component for the Service Binding Manager.
 * 
 * @author Heiko W. Rupp
 * @author Lukas Krejci
 */
public class ManagerComponent extends ManagedComponentComponent implements CreateChildResourceFacet {

    private final Log log = LogFactory.getLog(this.getClass());

    private static final String BINDING_PROPERTY = "binding";
    private static final String STANDARD_BINDINGS_PROPERTY = "standardBindings";
    private static final String BINDING_SET_SERVICE_NAME = "Service Binding Set";
    private static final String RESOURCE_KEY_SEPARATOR = "!)@(#*";

    private static final PropertyDefinition[] STANDARD_BINDING_PROPERTIES = {
        new PropertyDefinition("serviceName", String.class), new PropertyDefinition("bindingName", String.class),
        new PropertyDefinition("port", Integer.class), new PropertyDefinition("hostname", String.class),
        new PropertyDefinition("description", String.class),
        new PropertyDefinition("fullyQualifiedName", String.class),
        new PropertyDefinition("fixedHostName", Boolean.class), new PropertyDefinition("fixedPort", Boolean.class) };

    private ManagedComponent bindingManagerComponent;

    @Override
    public AvailabilityType getAvailability() {
        RunState runState = getManagedComponent().getRunState();
        return (runState == RunState.RUNNING || runState == RunState.UNKNOWN) ? AvailabilityType.UP
            : AvailabilityType.DOWN;
    }

    // ConfigurationFacet -----------------------------

    @Override
    public Configuration loadResourceConfiguration() {
        bindingManagerComponent = getManagedComponent();

        Configuration configuration = new Configuration();

        String activeBindingSetName = Util.getValue((SimpleValue) bindingManagerComponent.getProperty(
            Util.ACTIVE_BINDING_SET_NAME_PROPERTY).getValue(), String.class);

        configuration.put(new PropertySimple(Util.ACTIVE_BINDING_SET_NAME_PROPERTY, activeBindingSetName));

        CollectionValue standardBindings = (CollectionValue) bindingManagerComponent.getProperty(
            STANDARD_BINDINGS_PROPERTY).getValue();

        PropertyList bindings = new PropertyList(STANDARD_BINDINGS_PROPERTY);
        configuration.put(bindings);

        for (MetaValue b : standardBindings.getElements()) {
            CompositeValue binding = (CompositeValue) b;

            PropertyMap bindingMap = new PropertyMap(BINDING_PROPERTY);
            bindings.add(bindingMap);

            for (PropertySimple prop : Util.getProperties(Arrays.asList(STANDARD_BINDING_PROPERTIES), binding)) {
                bindingMap.put(prop);
            }
        }

        return configuration;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport configurationUpdateReport) {
        try {
            Configuration updatedConfiguration = configurationUpdateReport.getConfiguration();

            ManagedComponent bindingManagerComponent = getBindingManager();

            //check that provided active binding set name is valid
            ManagedProperty bindingSetsProperty = bindingManagerComponent.getProperty(Util.BINDING_SETS_PROPERTY);
            CollectionValue bindingSets = (CollectionValue) bindingSetsProperty.getValue();
            String updatedActiveBindingSetName = updatedConfiguration.getSimpleValue(
                Util.ACTIVE_BINDING_SET_NAME_PROPERTY, null);

            if (updatedActiveBindingSetName == null || updatedActiveBindingSetName.trim().isEmpty()) {
                configurationUpdateReport.setErrorMessage("Active binding set name must be set.");
                configurationUpdateReport.setStatus(ConfigurationUpdateStatus.FAILURE);
            }

            boolean found = false;
            Iterator<MetaValue> it = bindingSets.iterator();
            while (it.hasNext()) {
                CompositeValue bindingSet = (CompositeValue) it.next();
                String bindingSetName = Util.getValue(bindingSet, "name", String.class);

                if (updatedActiveBindingSetName.equals(bindingSetName)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                configurationUpdateReport
                    .setErrorMessage("A binding set with provided name does not exists. Cannot set it as active.");
                configurationUpdateReport.setStatus(ConfigurationUpdateStatus.FAILURE);
                return;
            }

            bindingManagerComponent.getProperty(Util.ACTIVE_BINDING_SET_NAME_PROPERTY).setValue(
                Util.wrap(updatedConfiguration.getSimple(Util.ACTIVE_BINDING_SET_NAME_PROPERTY), String.class));

            PropertyList standardBindingsList = updatedConfiguration.getList(STANDARD_BINDINGS_PROPERTY);

            MetaValue[] standarBindingsArray = new MetaValue[standardBindingsList.getList().size()];
            CollectionValueSupport standardBindingsValue = new CollectionValueSupport(
                (CollectionMetaType) bindingManagerComponent.getProperty(STANDARD_BINDINGS_PROPERTY).getMetaType());
            standardBindingsValue.setElements(standarBindingsArray);

            ImmutableCompositeMetaType bindingMetaType = (ImmutableCompositeMetaType) standardBindingsValue
                .getMetaType().getElementType();

            int i = 0;
            for (Property p : standardBindingsList.getList()) {
                PropertyMap standardBindingMap = (PropertyMap) p;

                MapCompositeValueSupport binding = new MapCompositeValueSupport(bindingMetaType);

                for (PropertyDefinition def : STANDARD_BINDING_PROPERTIES) {
                    binding.put(def.propertyName, Util.wrap(standardBindingMap.getSimple(def.propertyName),
                        def.propertyType));
                }

                standarBindingsArray[i++] = binding;
            }

            bindingManagerComponent.getProperty(STANDARD_BINDINGS_PROPERTY).setValue(standardBindingsValue);

            updateBindingManager();

            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (Exception e) {
            log.warn("Failed to update SBM configuration", e);
            configurationUpdateReport.setErrorMessageFromThrowable(e);
            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.FAILURE);
        }
    }

    // CreateChildResourceFacet ----------------------------------------------

    public CreateResourceReport createResource(CreateResourceReport report) {
        try {
            if (BINDING_SET_SERVICE_NAME.equals(report.getResourceType().getName())) {
                Configuration bindingConfiguration = report.getResourceConfiguration();

                CompositeValue newBindingSet = Util.getBindingSetFromConfiguration(getBindingSetValueMetaType(),
                    bindingConfiguration);

                //ok, now we can update the bindingSets property with the update set of binding sets
                ManagedComponent bindingManagerComponent = getBindingManager();
                ManagedProperty bindingSetsProperty = bindingManagerComponent.getProperty(Util.BINDING_SETS_PROPERTY);

                //check that the provided binding set name is unique
                CollectionValue bindingSets = (CollectionValue) bindingSetsProperty.getValue();
                String newBindingSetName = Util.getValue(newBindingSet, "name", String.class);

                Iterator<MetaValue> it = bindingSets.iterator();
                while (it.hasNext()) {
                    CompositeValue bindingSet = (CompositeValue) it.next();
                    String bindingSetName = Util.getValue(bindingSet, "name", String.class);

                    if (newBindingSetName.equals(bindingSetName)) {
                        report.setErrorMessage("A binding set with provided name already exists.");
                        report.setStatus(CreateResourceStatus.FAILURE);
                        return report;
                    }
                }

                int newIndex = bindingSets.getSize();
                MetaValue[] newBindingSets = Arrays.copyOf(bindingSets.getElements(), newIndex + 1);
                newBindingSets[newIndex] = newBindingSet;

                CollectionValueSupport newBindingSetsValue = new CollectionValueSupport(bindingSets.getMetaType());
                newBindingSetsValue.setElements(newBindingSets);

                bindingSetsProperty.setValue(newBindingSetsValue);

                updateBindingManager();

                report.setResourceKey(getBindingSetResourceKey(newBindingSetName));
                report.setResourceName(newBindingSetName);
                report.setStatus(CreateResourceStatus.SUCCESS);
            }
        } catch (Exception e) {
            log.warn("Failed to create service " + report.getResourceType().getName(), e);
            report.setException(e);
            report.setStatus(CreateResourceStatus.FAILURE);
        }
        return report;
    }

    public MetaType getBindingSetValueMetaType() {
        CollectionMetaType bindingSetsMetaType = (CollectionMetaType) getBindingManager().getProperty(
            Util.BINDING_SETS_PROPERTY).getMetaType();

        return bindingSetsMetaType.getElementType();
    }

    public ManagedComponent getBindingManager() {
        if (bindingManagerComponent == null) {
            loadResourceConfiguration();
        }
        return bindingManagerComponent;
    }

    public void updateBindingManager() throws Exception {
        ManagementView managementView = getConnection().getManagementView();
        managementView.updateComponent(getBindingManager());
        managementView.load();
    }

    public String getBindingSetResourceKey(String bindingSetName) {
        return getResourceContext().getResourceKey() + RESOURCE_KEY_SEPARATOR + bindingSetName;
    }

    public String getBindingSetNameFromResourceKey(String resourceKey) {
        int separatorIdx = resourceKey.lastIndexOf(RESOURCE_KEY_SEPARATOR);
        return resourceKey.substring(separatorIdx + RESOURCE_KEY_SEPARATOR.length());
    }
}
