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
package org.custom.plugin;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
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
 * This can be the start of your own custom plugin's server component. Review the javadoc for {@link ResourceComponent}
 * and all the facet interfaces to learn what you can do in your resource component. This component has a lot of methods
 * in it because it implements all possible facets. If your resource does not support, for example, configuration, you
 * can remove the {@link ConfigurationFacet} from the <code>implements</code> clause and remove all method
 * implementations that that facet required.
 *
 * <p>You should not only read the javadoc in each of this class' methods, but you should also read the javadocs linked
 * by their "see" javadoc tags since those additional javadocs will contain a good deal of additional information you
 * will need to know.</p>
 *
 * @author John Mazzitelli
 */
public class CustomPluginServerComponent implements ResourceComponent, MeasurementFacet, OperationFacet,
    ConfigurationFacet, ContentFacet, DeleteResourceFacet, CreateChildResourceFacet {
    private final Log log = LogFactory.getLog(CustomPluginServerComponent.class);

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
     * The plugin container will call this method when your resource component has been scheduled to collect some
     * measurements now. It is within this method that you actually talk to the managed resource and collect the
     * measurement data that is has emitted.
     *
     * @see MeasurementFacet#getValues(MeasurementReport, Set)
     */
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        for (MeasurementScheduleRequest request : requests) {
            String name = request.getName();

            // TODO: based on the request information, you must collect the requested measurement(s)
            //       you can use the name of the measurement to determine what you actually need to collect
            try {
                Number value = new Integer(1); // dummy measurement value - this should come from the managed resource
                report.addData(new MeasurementDataNumeric(request, value.doubleValue()));
            } catch (Exception e) {
                log.error("Failed to obtain measurement [" + name + "]. Cause: " + e);
            }
        }

        return;
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

    /**
     * The plugin container will call this method and it needs to obtain the current configuration of the managed
     * resource. Your plugin will obtain the managed resource's configuration in your own custom way and populate the
     * returned Configuration object with the managed resource's configuration property values.
     *
     * @see ConfigurationFacet#loadResourceConfiguration()
     */
    public Configuration loadResourceConfiguration() {
        // here we simulate the loading of the managed resource's configuration

        if (resourceConfiguration == null) {
            // for this example, we will create a simple dummy configuration to start with.
            // note that it is empty, so we're assuming there are no required configs in the plugin descriptor.
            resourceConfiguration = new Configuration();
        }

        Configuration config = resourceConfiguration;

        return config;
    }

    /**
     * The plugin container will call this method when it has a new configuration for your managed resource. Your plugin
     * will re-configure the managed resource in your own custom way, setting its configuration based on the new values
     * of the given configuration.
     *
     * @see ConfigurationFacet#updateResourceConfiguration(ConfigurationUpdateReport)
     */
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // this simulates the plugin taking the new configuration and reconfiguring the managed resource
        resourceConfiguration = report.getConfiguration().deepCopy();

        report.setStatus(ConfigurationUpdateStatus.SUCCESS);
    }

    /**
     * When this is called, the plugin is responsible for scanning its managed resource and look for content that need
     * to be managed for that resource. This method should only discover packages of the given package type.
     *
     * @see ContentFacet#discoverDeployedPackages(PackageType)
     */
    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        return null;
    }

    /**
     * The plugin container calls this method when new packages need to be deployed/installed on resources.
     *
     * @see ContentFacet#deployPackages(Set, ContentServices)
     */
    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        return null;
    }

    /**
     * When a remote client wants to see the actual data content for an installed package, this method will be called.
     * This method must return a stream of data containing the full content of the package.
     *
     * @see ContentFacet#retrievePackageBits(ResourcePackageDetails)
     */
    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        return null;
    }

    /**
     * This is the method that is used when the component has to create the installation steps and their results.
     *
     * @see ContentFacet#generateInstallationSteps(ResourcePackageDetails)
     */
    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return null;
    }

    /**
     * This is called when the actual content of packages should be deleted from the managed resource.
     *
     * @see ContentFacet#removePackages(Set)
     */
    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        return null;
    }

    /**
     * When called, the plugin container is asking the plugin to create a new managed resource. The new resource's
     * details need to be added to the given report.
     *
     * @see CreateChildResourceFacet#createResource(CreateResourceReport)
     */
    public CreateResourceReport createResource(CreateResourceReport report) {
        return null;
    }

    /**
     * When called, the plugin container is asking the plugin to delete a managed resource.
     *
     * @see DeleteResourceFacet#deleteResource()
     */
    public void deleteResource() {
    }
}