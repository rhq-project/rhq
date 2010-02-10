/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugin.nss;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * Allows the RHQ server to manage the Name Server Switch Configuration for a Linux Platform 
 * 
 * @author Adam Young
 */
public class NameServiceSwitchComponent implements ResourceComponent, OperationFacet,
    ResourceConfigurationFacet {
    private final Log log = LogFactory.getLog(NameServiceSwitchComponent.class);

    /**
     * Represents the resource configuration of the custom product being managed.
     */
    private Configuration resourceConfiguration;

    /**
     * All AMPS plugins are stateful - this context contains information that your resource component can use when
     * performing its processing.
     */
    private ResourceContext resourceContext;

    /**
     * This is called when your component has been started with the given context. You normally initialize some internal
     * state of your component as well as attempt to make a stateful connection to your managed resource.
     *
     * @see ResourceComponent#start(ResourceContext)
     */
    public void start(ResourceContext context) {
        resourceContext = context;
    }

    /**
     * This is called when the component is being stopped, usually due to the plugin container shutting down. You can
     * perform some cleanup here; though normally not much needs to be done here.
     *
     * @see ResourceComponent#stop()
     */
    public void stop() {
    }

    /**
     * All resource components must be able to tell the plugin container if the managed resource is available or not.
     * This method is called by the plugin container when it needs to know if the managed resource is actually up and
     * available.
     *
     * @see ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        // TODO: here you normally make some type of connection attempt to the managed resource
        //       to determine if it is really up and running.
        return AvailabilityType.UP;
    }


    /**
     * The plugin container will call this method when it wants to invoke an operation on your managed resource. Your
     * plugin will connect to the managed resource and invoke the analogous operation in your own custom way.
     *
     * @see OperationFacet#invokeOperation(String, Configuration)
     */
    public OperationResult invokeOperation(String name, Configuration configuration) {
        return null;
    }

    public Set<RawConfiguration> loadRawConfigurations() {
        // TODO Auto-generated method stub
        return null;
    }

    public Configuration loadStructuredConfiguration() {
        // TODO Auto-generated method stub
        return null;
    }

    public RawConfiguration mergeRawConfiguration(Configuration from, RawConfiguration to) {
        // TODO Auto-generated method stub
        return null;
    }

    public void mergeStructuredConfiguration(RawConfiguration from, Configuration to) {
        // TODO Auto-generated method stub
        
    }

    public void persistRawConfiguration(RawConfiguration rawConfiguration) {
        // TODO Auto-generated method stub
        
    }

    public void persistStructuredConfiguration(Configuration configuration) {
        // TODO Auto-generated method stub
        
    }

    public void validateRawConfiguration(RawConfiguration rawConfiguration) throws RuntimeException {
        // TODO Auto-generated method stub
        
    }

    public void validateStructuredConfiguration(Configuration configuration) {
        // TODO Auto-generated method stub
        
    }
}
