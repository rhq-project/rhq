/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.pc;

import org.rhq.core.clientapi.server.bundle.BundleServerService;
import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.clientapi.server.content.ContentServerService;
import org.rhq.core.clientapi.server.core.CoreServerService;
import org.rhq.core.clientapi.server.discovery.DiscoveryServerService;
import org.rhq.core.clientapi.server.drift.DriftServerService;
import org.rhq.core.clientapi.server.event.EventServerService;
import org.rhq.core.clientapi.server.inventory.ResourceFactoryServerService;
import org.rhq.core.clientapi.server.measurement.MeasurementServerService;
import org.rhq.core.clientapi.server.operation.OperationServerService;

/**
 * Provides access to remote pojo's representing the JON Server's client interfaces. These server service interfaces are
 * what the agent and its embedded plugin container uses to make remote calls into the JON Server.
 *
 * <p>Study these server service interfaces if you want to know every capability the JON Server exposes to its agents
 * and associated plugin containers.</p>
 *
 * @author Greg Hinkle
 */
public class ServerServices {
    private CoreServerService coreServerService;
    private DiscoveryServerService discoveryServerService;
    private MeasurementServerService measurementServerService;
    private ConfigurationServerService configurationServerService;
    private OperationServerService operationServerService;
    private ResourceFactoryServerService resourceFactoryServerService;
    private ContentServerService contentServerService;
    private EventServerService eventServerService;
    private BundleServerService bundleServerService;
    private DriftServerService driftServerService;

    public CoreServerService getCoreServerService() {
        return coreServerService;
    }

    public void setCoreServerService(CoreServerService coreServerService) {
        this.coreServerService = coreServerService;
    }

    public DiscoveryServerService getDiscoveryServerService() {
        return discoveryServerService;
    }

    public void setDiscoveryServerService(DiscoveryServerService discoveryServerService) {
        this.discoveryServerService = discoveryServerService;
    }

    public MeasurementServerService getMeasurementServerService() {
        return measurementServerService;
    }

    public void setMeasurementServerService(MeasurementServerService measurementServerService) {
        this.measurementServerService = measurementServerService;
    }

    public ConfigurationServerService getConfigurationServerService() {
        return configurationServerService;
    }

    public void setConfigurationServerService(ConfigurationServerService configurationServerService) {
        this.configurationServerService = configurationServerService;
    }

    public OperationServerService getOperationServerService() {
        return operationServerService;
    }

    public void setOperationServerService(OperationServerService operationServerService) {
        this.operationServerService = operationServerService;
    }

    public ResourceFactoryServerService getResourceFactoryServerService() {
        return resourceFactoryServerService;
    }

    public void setResourceFactoryServerService(ResourceFactoryServerService resourceFactoryServerService) {
        this.resourceFactoryServerService = resourceFactoryServerService;
    }

    public ContentServerService getContentServerService() {
        return contentServerService;
    }

    public void setContentServerService(ContentServerService contentServerService) {
        this.contentServerService = contentServerService;
    }

    public EventServerService getEventServerService() {
        return eventServerService;
    }

    public void setEventServerService(EventServerService eventServerService) {
        this.eventServerService = eventServerService;
    }

    public BundleServerService getBundleServerService() {
        return bundleServerService;
    }

    public void setBundleServerService(BundleServerService bundleServerService) {
        this.bundleServerService = bundleServerService;
    }

    public DriftServerService getDriftServerService() {
        return driftServerService;
    }

    public void setDriftServerService(DriftServerService service) {
        driftServerService = service;
    }

}