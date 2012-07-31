/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.core.pc.inventory.testplugin;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.bundle.BundleDeployRequest;
import org.rhq.core.pluginapi.bundle.BundleDeployResult;
import org.rhq.core.pluginapi.bundle.BundleFacet;
import org.rhq.core.pluginapi.bundle.BundlePurgeRequest;
import org.rhq.core.pluginapi.bundle.BundlePurgeResult;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.support.SnapshotReportRequest;
import org.rhq.core.pluginapi.support.SnapshotReportResults;
import org.rhq.core.pluginapi.support.SupportFacet;

/**
 * This resource component implements most (all?) of the facets so that all the plugin container subsystems can work
 * with it.
 *  
 * @author Ian Springer
 */
public class TestResourceComponent implements ResourceComponent<ResourceComponent<?>>, OperationFacet, ContentFacet,
    ConfigurationFacet, MeasurementFacet, DeleteResourceFacet, CreateChildResourceFacet, BundleFacet, SupportFacet {

    private ResourceContext<ResourceComponent<?>> resourceContext;

    @Override
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    @Override
    public void start(ResourceContext<ResourceComponent<?>> resourceContext) throws InvalidPluginConfigurationException,
        Exception {
        this.resourceContext = resourceContext;
    }

    @Override
    public void stop() {
        return;
    }

    @Override
    public SnapshotReportResults getSnapshotReport(SnapshotReportRequest request) throws Exception {
        return null;
    }

    @Override
    public BundleDeployResult deployBundle(BundleDeployRequest request) {
        return null;
    }

    @Override
    public BundlePurgeResult purgeBundle(BundlePurgeRequest request) {
        return null;
    }

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {
        return null;
    }

    @Override
    public void deleteResource() throws Exception {
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        return null;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
    }

    @Override
    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return null;
    }

    @Override
    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        return null;
    }

    @Override
    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        return null;
    }

    @Override
    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        return null;
    }

    @Override
    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        return null;
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {
        return null;
    }
}
