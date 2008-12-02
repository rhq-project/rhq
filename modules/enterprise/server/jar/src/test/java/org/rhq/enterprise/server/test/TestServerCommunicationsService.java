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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jboss.remoting.InvokerLocator;

import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.content.ContentAgentService;
import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.agent.inventory.ResourceFactoryAgentService;
import org.rhq.core.clientapi.agent.measurement.MeasurementAgentService;
import org.rhq.core.clientapi.agent.operation.OperationAgentService;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.command.server.CommandProcessorMetrics.Calltime;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.core.comm.ServerConfiguration;

/**
 * Our test server comm service MBean, which mimics the same interface as the real service but provides some dummy agent
 * clients. Tests should have access to this service instance and should set one or more of the public fields that
 * define what the test services should be.
 */
public class TestServerCommunicationsService implements TestServerCommunicationsServiceMBean {
    public ContentAgentService contentService;
    public ResourceFactoryAgentService resourceFactoryService;
    public ConfigurationAgentService configurationService;
    public DiscoveryAgentService discoveryService;
    public MeasurementAgentService measurementService;
    public OperationAgentService operationService;

    private Map<Agent, AgentClient> agentClients = new HashMap<Agent, AgentClient>();

    public TestServerCommunicationsService() {
    }

    public AgentClient getKnownAgentClient(Agent agent) {
        AgentClient testClient = new TestAgentClient(agent, this);
        agentClients.put(agent, testClient);
        return testClient;
    }

    public void destroyKnownAgentClient(Agent agent) {
        agentClients.remove(agent);
    }

    public List<InvokerLocator> getAllKnownAgents() {
        return null;
    }

    public void addStartedAgent(Agent agent) {
    }

    public void removeDownedAgent(String endpoint) {
    }

    public boolean pingEndpoint(String endpoint, long timeoutMillis) {
        return true;
    }

    public Integer getGlobalConcurrencyLimit() {
        return 1;
    }

    public void setGlobalConcurrencyLimit(Integer maxConcurrency) {
    }

    public Integer getInventoryReportConcurrencyLimit() {
        return 1;
    }

    public void setInventoryReportConcurrencyLimit(Integer maxConcurrency) {
    }

    public Integer getAvailabilityReportConcurrencyLimit() {
        return 1;
    }

    public void setAvailabilityReportConcurrencyLimit(Integer maxConcurrency) {
    }

    public Integer getInventorySyncConcurrencyLimit() {
        return 1;
    }

    public void setInventorySyncConcurrencyLimit(Integer maxConcurrency) {
    }

    public Integer getContentReportConcurrencyLimit() {
        return 1;
    }

    public void setContentReportConcurrencyLimit(Integer maxConcurrency) {
    }

    public Integer getContentDownloadConcurrencyLimit() {
        return 1;
    }

    public void setContentDownloadConcurrencyLimit(Integer maxConcurrency) {
    }

    public Integer getMeasurementReportConcurrencyLimit() {
        return 1;
    }

    public void setMeasurementReportConcurrencyLimit(Integer maxConcurrency) {
    }

    public Integer getMeasurementScheduleRequestConcurrencyLimit() {
        return 1;
    }

    public void setMeasurementScheduleRequestConcurrencyLimit(Integer maxConcurrency) {
    }

    public void clear() {
    }

    public long getNumberDroppedCommandsReceived() {
        return 0;
    }

    public long getNumberNotProcessedCommandsReceived() {
        return 0;
    }

    public long getNumberFailedCommandsReceived() {
        return 0;
    }

    public long getNumberSuccessfulCommandsReceived() {
        return 0;
    }

    public long getNumberTotalCommandsReceived() {
        return 0;
    }

    public long getAverageExecutionTimeReceived() {
        return 0;
    }

    public Map<String, Calltime> getCallTimeDataReceived() {
        return null;
    }

    public ServerConfiguration getConfiguration() {
        return null;
    }

    public String getConfigurationFile() {
        return null;
    }

    public Properties getConfigurationOverrides() {
        return null;
    }

    public String getPreferencesNodeName() {
        return null;
    }

    public ServiceContainer getServiceContainer() {
        return new MockServiceContainer();
    }

    public ServiceContainer safeGetServiceContainer() {
        return new MockServiceContainer();
    }

    public String getStartedServerEndpoint() {
        return "http://TestServerCommunicationsService";
    }

    public ServerConfiguration reloadConfiguration() throws Exception {
        return null;
    }

    public void setConfigurationFile(String location) {
    }

    public void setConfigurationOverrides(Properties overrides) {
    }

    public void setPreferencesNodeName(String node) {
    }

    public void startCommunicationServices() throws Exception {
    }

    public void stop() throws Exception {
    }

    public boolean isStarted() {
        return true;
    }

    private class MockServiceContainer extends ServiceContainer {
        @Override
        public Long addRemoteInputStream(InputStream in) throws Exception {
            return new Long(1);
        }
    }
}