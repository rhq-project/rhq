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
package org.rhq.enterprise.server.test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.bundle.BundleAgentService;
import org.rhq.core.clientapi.agent.bundle.BundlePurgeRequest;
import org.rhq.core.clientapi.agent.bundle.BundlePurgeResponse;
import org.rhq.core.clientapi.agent.bundle.BundleScheduleRequest;
import org.rhq.core.clientapi.agent.bundle.BundleScheduleResponse;
import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.agent.content.ContentAgentService;
import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.agent.discovery.InvalidPluginConfigurationClientException;
import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.clientapi.agent.inventory.CreateResourceRequest;
import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceRequest;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceResponse;
import org.rhq.core.clientapi.agent.inventory.ResourceFactoryAgentService;
import org.rhq.core.clientapi.agent.measurement.MeasurementAgentService;
import org.rhq.core.clientapi.agent.operation.CancelResults;
import org.rhq.core.clientapi.agent.operation.OperationAgentService;
import org.rhq.core.clientapi.agent.support.SupportAgentService;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.clientapi.server.content.ContentDiscoveryReport;
import org.rhq.core.clientapi.server.content.DeletePackagesRequest;
import org.rhq.core.clientapi.server.content.DeployPackagesRequest;
import org.rhq.core.clientapi.server.content.RetrievePackageBitsRequest;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.communications.command.annotation.Asynchronous;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataRequest;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.agentclient.AgentClient;

public class TestAgentClient implements AgentClient, BundleAgentService, DriftAgentService, ContentAgentService,
    ResourceFactoryAgentService, ConfigurationAgentService, DiscoveryAgentService, MeasurementAgentService,
    OperationAgentService, SupportAgentService {
    private final Agent agent;
    private final TestServerCommunicationsService commService;

    public TestAgentClient(Agent agent, TestServerCommunicationsService commService) {
        this.agent = agent;
        this.commService = commService;
    }

    @Override
    public Agent getAgent() {
        return this.agent;
    }

    @Override
    public BundleAgentService getBundleAgentService() {
        return (commService.bundleService != null) ? commService.bundleService : this;
    }

    @Override
    public ContentAgentService getContentAgentService() {
        return (commService.contentService != null) ? commService.contentService : this;
    }

    @Override
    public ResourceFactoryAgentService getResourceFactoryAgentService() {
        return (commService.resourceFactoryService != null) ? commService.resourceFactoryService : this;
    }

    @Override
    public ConfigurationAgentService getConfigurationAgentService() {
        return (commService.configurationService != null) ? commService.configurationService : this;
    }

    @Override
    public DiscoveryAgentService getDiscoveryAgentService() {
        return (commService.discoveryService != null) ? commService.discoveryService : this;
    }

    @Override
    public MeasurementAgentService getMeasurementAgentService() {
        return (commService.measurementService != null) ? commService.measurementService : this;
    }

    @Override
    public OperationAgentService getOperationAgentService() {
        return (commService.operationService != null) ? commService.operationService : this;
    }

    @Override
    public SupportAgentService getSupportAgentService() {
        return (commService.supportService != null) ? commService.supportService : this;
    }

    @Override
    public void startSending() {
        return; // no-op
    }

    @Override
    public void stopSending() {
        return; // no-op
    }

    @Override
    public boolean ping(long timeoutMillis) {
        return true;
    }

    // provide no-ops for all agent services

    @Override
    public Set<MeasurementData> getRealTimeMeasurementValue(int resourceId, List<MeasurementDataRequest> requests) {
        return null;
    }

    @Override
    public void scheduleCollection(Set<ResourceMeasurementScheduleRequest> resourceSchedules) {
    }

    @Override
    public void unscheduleCollection(Set<Integer> resourceIds) {
    }

    @Override
    public void updateCollection(Set<ResourceMeasurementScheduleRequest> resourceSchedules) {
    }

    @Override
    public Map<String, Object> getMeasurementScheduleInfoForResource(int resourceId) {
        return null;
    }

    @Override
    public ConfigurationUpdateResponse executeUpdateResourceConfigurationImmediately(ConfigurationUpdateRequest request)
        throws PluginContainerException {
        return null;
    }

    @Override
    public Configuration loadResourceConfiguration(int resourceId) throws PluginContainerException {
        return null;
    }

    @Override
    public Configuration merge(Configuration configuration, int resourceId, boolean fromStructured)
        throws PluginContainerException {
        return null;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateRequest request) {
    }

    @Override
    public CancelResults cancelOperation(String jobId) {
        return null;
    }

    @Override
    public void invokeOperation(String jobId, int resourceId, String operationName, Configuration parameters)
        throws PluginContainerException {
    }

    @Override
    public void deletePackages(DeletePackagesRequest request) {
    }

    @Override
    public void deployPackages(DeployPackagesRequest request) {
    }

    @Override
    public ContentDiscoveryReport executeResourcePackageDiscoveryImmediately(int resourceId, String packageTypeName)
        throws PluginContainerException {
        return null;
    }

    @Override
    public Set<ResourcePackageDetails> getLastDiscoveredResourcePackages(int resourceId) {
        return null;
    }

    @Override
    public void retrievePackageBits(RetrievePackageBitsRequest request) {
    }

    @Override
    public List<DeployPackageStep> translateInstallationSteps(int resourceId, ResourcePackageDetails packageDetails)
        throws PluginContainerException {
        return null;
    }

    @Override
    public void disableServiceScans(int serverResourceId) {
    }

    @Override
    public void enableServiceScans(int serverResourceId, Configuration config) {
    }

    @Override
    public AvailabilityReport executeAvailabilityScanImmediately(boolean changedOnlyReport) {
        return null;
    }

    @Override
    public InventoryReport executeServerScanImmediately() throws PluginContainerException {
        return null;
    }

    @Override
    public InventoryReport executeServiceScanImmediately() throws PluginContainerException {
        return null;
    }

    @Override
    public void executeServiceScanDeferred() {
        return;
    }

    @Override
    public Availability getCurrentAvailability(Resource resource) {
        return null;
    }

    @Override
    public Resource getPlatform() {
        return null;
    }

    @Override
    public MergeResourceResponse manuallyAddResource(ResourceType resourceType, int parentResourceId,
        Configuration pluginConfiguration, int creatorSubjectId) throws InvalidPluginConfigurationClientException,
        PluginContainerException {
        return null;
    }

    @Override
    public void uninventoryResource(int resourceId) {
    }

    @Override
    public void updatePluginConfiguration(int resourceId, Configuration newPluginConfiguration)
        throws InvalidPluginConfigurationClientException, PluginContainerException {
    }

    @Asynchronous(guaranteedDelivery = true)
    @Override
    public void synchronizeInventory(ResourceSyncInfo syncInfo) {
        return;
    }

    @Override
    public void createResource(CreateResourceRequest request) throws PluginContainerException {
    }

    @Override
    public void deleteResource(DeleteResourceRequest request) throws PluginContainerException {
    }

    @Override
    public CreateResourceResponse executeCreateResourceImmediately(CreateResourceRequest request)
        throws PluginContainerException {
        return null;
    }

    @Override
    public DeleteResourceResponse executeDeleteResourceImmediately(DeleteResourceRequest request)
        throws PluginContainerException {
        return null;
    }

    @Override
    public InputStream getSnapshotReport(int resourceId, String name, String description) throws Exception {
        return null;
    }

    @Override
    public Configuration validate(Configuration configuration, int resourceId, boolean isStructured)
        throws PluginContainerException {
        return null;
    }

    @Override
    public BundleScheduleResponse schedule(BundleScheduleRequest request) {
        return new BundleScheduleResponse();
    }

    @Override
    public BundlePurgeResponse purge(BundlePurgeRequest request) {
        return new BundlePurgeResponse();
    }

    @Override
    public DriftAgentService getDriftAgentService() {
        return (commService.driftService != null) ? commService.driftService : this;
    }

    @Override
    public boolean requestDriftFiles(List<DriftFile> driftFiles) {
        return false;
    }

    @Override
    public void scheduleDriftDetection(int resourceId, DriftConfiguration driftConfiguration) {
    }

    @Override
    public void detectDrift(int resourceId, DriftConfiguration driftConfiguration) {
    }
}
