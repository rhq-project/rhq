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

import java.util.List;
import java.util.Set;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.agent.content.ContentAgentService;
import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.agent.discovery.InvalidPluginConfigurationClientException;
import org.rhq.core.clientapi.agent.inventory.CreateResourceRequest;
import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceRequest;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceResponse;
import org.rhq.core.clientapi.agent.inventory.ResourceFactoryAgentService;
import org.rhq.core.clientapi.agent.measurement.MeasurementAgentService;
import org.rhq.core.clientapi.agent.operation.CancelResults;
import org.rhq.core.clientapi.agent.operation.OperationAgentService;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.communications.command.annotation.Asynchronous;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.transfer.ContentDiscoveryReport;
import org.rhq.core.domain.content.transfer.DeletePackagesRequest;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesRequest;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.content.transfer.RetrievePackageBitsRequest;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.discovery.InventoryReport;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.agentclient.AgentClient;

public class TestAgentClient implements AgentClient, ContentAgentService, ResourceFactoryAgentService,
    ConfigurationAgentService, DiscoveryAgentService, MeasurementAgentService, OperationAgentService {
    private final Agent agent;
    private final TestServerCommunicationsService commService;

    public TestAgentClient(Agent agent, TestServerCommunicationsService commService) {
        this.agent = agent;
        this.commService = commService;
    }

    public Agent getAgent() {
        return this.agent;
    }

    public ContentAgentService getContentAgentService() {
        return (commService.contentService != null) ? commService.contentService : this;
    }

    public ResourceFactoryAgentService getResourceFactoryAgentService() {
        return (commService.resourceFactoryService != null) ? commService.resourceFactoryService : this;
    }

    public ConfigurationAgentService getConfigurationAgentService() {
        return (commService.configurationService != null) ? commService.configurationService : this;
    }

    public DiscoveryAgentService getDiscoveryAgentService() {
        return (commService.discoveryService != null) ? commService.discoveryService : this;
    }

    public MeasurementAgentService getMeasurementAgentService() {
        return (commService.measurementService != null) ? commService.measurementService : this;
    }

    public OperationAgentService getOperationAgentService() {
        return (commService.operationService != null) ? commService.operationService : this;
    }

    public void startSending() {
        return; // no-op
    }

    public void stopSending() {
        return; // no-op
    }

    public boolean ping(long timeoutMillis) {
        return true;
    }

    // provide no-ops for all agent services

    public Set<MeasurementData> getRealTimeMeasurementValue(int resourceId, DataType dataType, String... measurementNames) {
        return null;
    }

    public void scheduleCollection(Set<ResourceMeasurementScheduleRequest> resourceSchedules) {
    }

    public void unscheduleCollection(Set<Integer> resourceIds) {
    }

    public void updateCollection(Set<ResourceMeasurementScheduleRequest> resourceSchedules) {
    }

    public ConfigurationUpdateResponse executeUpdateResourceConfigurationImmediately(ConfigurationUpdateRequest request)
        throws PluginContainerException {
        return null;
    }

    public Configuration loadResourceConfiguration(int resourceId) throws PluginContainerException {
        return null;
    }

    public void updateResourceConfiguration(ConfigurationUpdateRequest request) {
    }

    public CancelResults cancelOperation(String jobId) {
        return null;
    }

    public void invokeOperation(String jobId, int resourceId, String operationName, Configuration parameters)
        throws PluginContainerException {
    }

    public void deletePackages(DeletePackagesRequest request) {
    }

    public void deployPackages(DeployPackagesRequest request) {
    }

    public ContentDiscoveryReport executeResourcePackageDiscoveryImmediately(int resourceId, String packageTypeName)
        throws PluginContainerException {
        return null;
    }

    public Set<ResourcePackageDetails> getLastDiscoveredResourcePackages(int resourceId) {
        return null;
    }

    public void retrievePackageBits(RetrievePackageBitsRequest request) {
    }

    public List<DeployPackageStep> translateInstallationSteps(int resourceId, ResourcePackageDetails packageDetails)
        throws PluginContainerException {
        return null;
    }

    public void disableServiceScans(int serverResourceId) {
    }

    public void enableServiceScans(int serverResourceId, Configuration config) {
    }

    public AvailabilityReport executeAvailabilityScanImmediately(boolean changedOnlyReport) {
        return null;
    }

    public InventoryReport executeServerScanImmediately() throws PluginContainerException {
        return null;
    }

    public InventoryReport executeServiceScanImmediately() throws PluginContainerException {
        return null;
    }

    public void executeServiceScanDeferred() {
        return;
    }

    public Availability getAvailability(Resource resource) {
        return null;
    }

    public Resource getPlatform() {
        return null;
    }

    public MergeResourceResponse manuallyAddResource(ResourceType resourceType, int parentResourceId,
        Configuration pluginConfiguration, int creatorSubjectId) throws InvalidPluginConfigurationClientException,
        PluginContainerException {
        return null;
    }

    public void removeResource(int resourceId) {
    }

    public void updatePluginConfiguration(int resourceId, Configuration newPluginConfiguration)
        throws InvalidPluginConfigurationClientException, PluginContainerException {
    }

    @Asynchronous(guaranteedDelivery = true)
    public void synchronizeInventory(ResourceSyncInfo syncInfo) {
        return;
    }

    public void createResource(CreateResourceRequest request) throws PluginContainerException {
    }

    public void deleteResource(DeleteResourceRequest request) throws PluginContainerException {
    }

    public CreateResourceResponse executeCreateResourceImmediately(CreateResourceRequest request)
        throws PluginContainerException {
        return null;
    }

    public DeleteResourceResponse executeDeleteResourceImmediately(DeleteResourceRequest request)
        throws PluginContainerException {
        return null;
    }
}