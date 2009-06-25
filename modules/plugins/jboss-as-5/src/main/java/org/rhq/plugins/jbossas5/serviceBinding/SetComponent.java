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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

public class SetComponent implements ResourceComponent<ManagerComponent>, ConfigurationFacet, DeleteResourceFacet {

    private final Log log = LogFactory.getLog(this.getClass());

    private static final String BINDING_PROPERTY = "binding";

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

            byte[] bindAddress = Util.getValue((SimpleValue) binding.get(Util.BIND_ADDRESS_PROPERTY), byte[].class);

            if (bindAddress == null) {
                bindingMap.put(new PropertySimple(Util.BIND_ADDRESS_PROPERTY, null));
            } else {
                if (bindAddress.length == 4) {
                    Inet4Address addr = (Inet4Address) InetAddress.getByAddress(bindAddress);
                    bindingMap.put(new PropertySimple(Util.BIND_ADDRESS_PROPERTY, addr.getHostAddress()));
                } else if (bindAddress.length == 16) {
                    Inet6Address addr = (Inet6Address) InetAddress.getByAddress(bindAddress);
                    bindingMap.put(new PropertySimple(Util.BIND_ADDRESS_PROPERTY, addr.getHostAddress()));
                }
            }
        }
        return configuration;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        try {
            Configuration updatedConfiguration = report.getConfiguration();
            MetaType bindingSetValueMetaType = context.getParentResourceComponent().getBindingSetValueMetaType();

            CompositeValue currentBindingSet = Util.getBindingSetFromConfiguration(bindingSetValueMetaType,
                updatedConfiguration);

            //ok, now we can update the bindingSets property with the update set of binding sets
            ManagedComponent bindingManagerComponent = context.getParentResourceComponent().getBindingManager();
            ManagedProperty bindingSetsProperty = bindingManagerComponent.getProperty(Util.BINDING_SETS_PROPERTY);
            String thisBindingSetName = context.getResourceKey();

            //create new set of binding sets
            CollectionValue bindingSets = (CollectionValue) bindingSetsProperty.getValue();
            List<MetaValue> newBindingSets = Util.replaceWithNew(bindingSets, thisBindingSetName, currentBindingSet);

            CollectionValueSupport newBindingSetsValue = new CollectionValueSupport(bindingSets.getMetaType());
            newBindingSetsValue.setElements(newBindingSets.toArray(new MetaValue[newBindingSets.size()]));

            bindingSetsProperty.setValue(newBindingSetsValue);

            context.getParentResourceComponent().updateBindingManager();

            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (Exception e) {
            log.warn("Failed to update service binding set", e);
            report.setErrorMessageFromThrowable(e);
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
            return;
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

        context.getParentResourceComponent().updateBindingManager();
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
}
