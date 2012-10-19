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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.values.CollectionValue;
import org.jboss.metatype.api.values.CollectionValueSupport;
import org.jboss.metatype.api.values.CompositeValue;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.SimpleValue;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas5.serviceBinding.Util.PropertyDefinition;

public class SetComponent implements ResourceComponent<ManagerComponent>, ConfigurationFacet, DeleteResourceFacet, OperationFacet,
    MeasurementFacet {

    private final Log log = LogFactory.getLog(this.getClass());

    private static final String BINDING_PROPERTY = "binding";
    private static final String RESULTING_BINDINGS_PROPERTY = "resultingBindings";
    
    private static final String DISPLAY_BINDINGS_OPERATION_NAME = "displayBindings";
    
    private ResourceContext<ManagerComponent> context;

    public Configuration loadResourceConfiguration() throws Exception {

        Configuration configuration = new Configuration();

        CompositeValue bindingSet = getBindingSet();

        if (bindingSet == null) {
            throw new IllegalStateException("Could not find a binding set called " + context.getResourceKey());
        }

        for (PropertySimple prop : Util.getProperties(Arrays.asList(Util.BINDING_SET_SIMPLE_PROPERTIES), bindingSet)) {
            configuration.put(prop);
        }

        CollectionValue overrideBindings = (CollectionValue) bindingSet.get(Util.OVERRIDE_BINDINGS_PROPERTY);

        PropertyList overrideBindingsList = new PropertyList(Util.OVERRIDE_BINDINGS_PROPERTY);
        configuration.put(overrideBindingsList);

        for (MetaValue m : overrideBindings.getElements()) {
            CompositeValue binding = (CompositeValue) m;

            PropertyMap bindingMap = new PropertyMap(BINDING_PROPERTY);
            overrideBindingsList.add(bindingMap);

            for (PropertySimple prop : Util.getProperties(Arrays.asList(Util.BINDING_SET_OVERRIDE_PROPERTIES), binding)) {
                bindingMap.put(prop);
            }
        }
        return configuration;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        try {
            ManagerComponent managerResourceComponent = context.getParentResourceComponent();
            
            Configuration updatedConfiguration = report.getConfiguration();

            managerResourceComponent.checkValidity(updatedConfiguration);
            
            MetaType bindingSetValueMetaType = context.getParentResourceComponent().getBindingSetValueMetaType();

            CompositeValue currentBindingSet = Util.getBindingSetFromConfiguration(bindingSetValueMetaType,
                updatedConfiguration);

            //ok, now we can update the bindingSets property with the update set of binding sets
            ManagedComponent bindingManagerComponent = managerResourceComponent.getBindingManager();
            ManagedProperty bindingSetsProperty = bindingManagerComponent.getProperty(Util.BINDING_SETS_PROPERTY);
            String thisBindingSetName = managerResourceComponent.getBindingSetNameFromResourceKey(context.getResourceKey());

            //create new set of binding sets
            CollectionValue bindingSets = (CollectionValue) bindingSetsProperty.getValue();
            List<MetaValue> newBindingSets = Util.replaceWithNew(bindingSets, thisBindingSetName, currentBindingSet);

            CollectionValueSupport newBindingSetsValue = new CollectionValueSupport(bindingSets.getMetaType());
            newBindingSetsValue.setElements(newBindingSets.toArray(new MetaValue[newBindingSets.size()]));

            bindingSetsProperty.setValue(newBindingSetsValue);

            context.getParentResourceComponent().updateBindingManager(bindingManagerComponent);

            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (Exception e) {
            log.warn("Failed to update service binding set", e);
            report.setErrorMessageFromThrowable(e);
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
        }
    }

    public AvailabilityType getAvailability() {
        return getBindingSet() != null ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    public void start(ResourceContext<ManagerComponent> context) {
        this.context = context;
    }

    public void stop() {
    }

    public void deleteResource() throws Exception {
        //check that this binding set is not the active one
        String thisBindingSetName = context.getParentResourceComponent().getBindingSetNameFromResourceKey(
            context.getResourceKey());

        ManagedComponent bindingManagerComponent = context.getParentResourceComponent().getBindingManager();

        String activeBindingSetName = Util.getValue((SimpleValue) bindingManagerComponent.getProperty(
            Util.ACTIVE_BINDING_SET_NAME_PROPERTY).getValue(), String.class);

        if (thisBindingSetName.equals(activeBindingSetName)) {
            throw new IllegalStateException("Cannot delete currently active binding set.");
        }

        ManagedProperty bindingSetsProperty = bindingManagerComponent.getProperty(Util.BINDING_SETS_PROPERTY);

        //create new set of binding sets
        CollectionValue bindingSets = (CollectionValue) bindingSetsProperty.getValue();
        List<MetaValue> newBindingSets = Util.replaceWithNew(bindingSets, thisBindingSetName, null);

        CollectionValueSupport newBindingSetsValue = new CollectionValueSupport(bindingSets.getMetaType());
        newBindingSetsValue.setElements(newBindingSets.toArray(new MetaValue[newBindingSets.size()]));

        bindingSetsProperty.setValue(newBindingSetsValue);

        context.getParentResourceComponent().updateBindingManager(bindingManagerComponent);
    }

    
    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {

        if (!DISPLAY_BINDINGS_OPERATION_NAME.equals(name)) return null;
        
        OperationResult result = new OperationResult();
        
        Configuration currentConfiguration = loadResourceConfiguration();
        Configuration bindingManagerConfiguration = context.getParentResourceComponent().loadResourceConfiguration();
                
        Configuration bindings = result.getComplexResults();
        
        //populate the resulting binding map so that the users have summary what the
        //the bindings would look like if this binding set was active.
        PropertyList resultingBindings = new PropertyList(RESULTING_BINDINGS_PROPERTY);
        bindings.put(resultingBindings);
        
        int portOffset = currentConfiguration.getSimple(Util.PORT_OFFSET_PROPERTY).getIntegerValue();
        String defaultHostName = currentConfiguration.getSimple(Util.DEFAULT_HOST_NAME_PROPERTY).getStringValue();
        
        Map<String, PropertyMap> overridesMap = buildOverridesMap(currentConfiguration.getList(Util.OVERRIDE_BINDINGS_PROPERTY));
        
        for(Property p : bindingManagerConfiguration.getList(Util.STANDARD_BINDINGS_PROPERTY).getList()) {
            PropertyMap standardBinding = (PropertyMap) p;
            
            PropertyMap bindingMap = new PropertyMap(BINDING_PROPERTY);
            resultingBindings.add(bindingMap);
            
            for (PropertyDefinition def : Util.BINDING_SET_OVERRIDE_PROPERTIES) {
                Property equivalent = standardBinding.get(def.propertyName);
                if (equivalent != null) {
                    bindingMap.put(equivalent);
                }
            }
            
            //now update the port and host name in the result
            boolean fixedPort = standardBinding.getSimple(Util.FIXED_PORT_PROPERTY).getBooleanValue();
            boolean fixedHostName = standardBinding.getSimple(Util.FIXED_HOST_NAME_PROPERTY).getBooleanValue();
            int standardPort = standardBinding.getSimple(Util.PORT_PROPERTY).getIntegerValue();
            String standardHostName = standardBinding.getSimple(Util.HOST_NAME_PROPERTY).getStringValue();
            
            PropertySimple resultingPort = bindingMap.getSimple(Util.PORT_PROPERTY);
            PropertySimple resultingHostName = bindingMap.getSimple(Util.HOST_NAME_PROPERTY);
            
            int portToSet = fixedPort ? standardPort : (standardPort + portOffset);
            String hostNameToSet = fixedHostName ? standardHostName : defaultHostName;
            
            //try to find an override for this binding
            String fullyQualifiedName = standardBinding.getSimpleValue(Util.FULLY_QUALIFIED_NAME_PROPERTY, null);
            PropertyMap override = overridesMap.get(fullyQualifiedName);
            if (override != null) {
                //remove this binding from overrides since we want to end up only
                //with the new additions in it after we process the standard bindings
                overridesMap.remove(fullyQualifiedName);
                
                //and get the resulting port and host name from the override instead
                portToSet = fixedPort ? portToSet : override.getSimple(Util.PORT_PROPERTY).getIntegerValue();
                
                String overrideHostName = override.getSimpleValue(Util.HOST_NAME_PROPERTY, null);
                overrideHostName = overrideHostName == null ? defaultHostName : overrideHostName;
                
                hostNameToSet = fixedHostName ? hostNameToSet : overrideHostName;
                
                String overrideDescription = override.getSimpleValue(Util.DESCRIPTION_PROPERTY, null);
                if (overrideDescription != null) {
                    bindingMap.put(new PropertySimple(Util.DESCRIPTION_PROPERTY, overrideDescription));
                }
            }
            
            resultingPort.setIntegerValue(portToSet);   
            resultingHostName.setStringValue(hostNameToSet);
        }
        
        for(PropertyMap override : overridesMap.values()) {
            if (override.getSimpleValue(Util.HOST_NAME_PROPERTY, null) == null) {
                override.put(new PropertySimple(Util.HOST_NAME_PROPERTY, defaultHostName));
            }
            resultingBindings.add(override);
        }
        
        return result;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        Configuration config = loadResourceConfiguration();

        for (MeasurementScheduleRequest request : metrics) {
            String requestName = request.getName();
            if (requestName.equals(Util.NAME_PROPERTY) || requestName.equals(Util.DEFAULT_HOST_NAME_PROPERTY) ||
                requestName.equals(Util.PORT_OFFSET_PROPERTY)) {
                
                String value = config.getSimpleValue(requestName, null);
                report.addData(new MeasurementDataTrait(request, value));
            }
        }
    }
    
    private CompositeValue getBindingSet() {
        String bindingSetName = context.getParentResourceComponent().getBindingSetNameFromResourceKey(
            context.getResourceKey());

        ManagedComponent bindingManagerComponent = context.getParentResourceComponent().getBindingManager();

        CollectionValue bindingSets = (CollectionValue) bindingManagerComponent.getProperty(Util.BINDING_SETS_PROPERTY)
            .getValue();

        Iterator<MetaValue> it = bindingSets.iterator();

        while (it.hasNext()) {
            CompositeValue bindingSet = (CompositeValue) it.next();

            String currentName = Util.getValue(bindingSet, "name", String.class);
            if (bindingSetName.equals(currentName)) {
                return bindingSet;
            }
        }

        return null;
    }
    
    private Map<String, PropertyMap> buildOverridesMap(PropertyList overrides) {
        TreeMap<String, PropertyMap> ret = new TreeMap<String, PropertyMap>();
        
        for(Property p : overrides.getList()) {
            PropertyMap overrideMap = (PropertyMap) p;
            ret.put(overrideMap.getSimpleValue(Util.FULLY_QUALIFIED_NAME_PROPERTY, null), overrideMap);
        }
        return ret;
    }    
}
