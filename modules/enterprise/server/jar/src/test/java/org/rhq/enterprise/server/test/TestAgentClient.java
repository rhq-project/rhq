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

import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.content.ContentAgentService;
import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.agent.inventory.ResourceFactoryAgentService;
import org.rhq.core.clientapi.agent.measurement.MeasurementAgentService;
import org.rhq.core.clientapi.agent.operation.OperationAgentService;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.server.agentclient.AgentClient;

public class TestAgentClient implements AgentClient {
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
        return commService.contentService;
    }

    public ResourceFactoryAgentService getResourceFactoryAgentService() {
        return commService.resourceFactoryService;
    }

    public ConfigurationAgentService getConfigurationAgentService() {
        return commService.configurationService;
    }

    public DiscoveryAgentService getDiscoveryAgentService() {
        return commService.discoveryService;
    }

    public MeasurementAgentService getMeasurementAgentService() {
        return commService.measurementService;
    }

    public OperationAgentService getOperationAgentService() {
        return commService.operationService;
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
}