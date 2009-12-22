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
package org.rhq.core.pc.configuration;

import static org.rhq.core.domain.configuration.ConfigurationUpdateStatus.*;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;

/**
 * Performs the actual configuration of a resource by invoking the configuration facet of a resource component.
 *
 * @author Mark Spritzler
 */
public class UpdateResourceConfigurationRunner implements Runnable, Callable<ConfigurationUpdateResponse> {
    private final Log log = LogFactory.getLog(UpdateResourceConfigurationRunner.class);

    /**
     * request that was used to trigger this update.
     */
    private ConfigurationUpdateRequest request;

    private ConfigManagement configMgmt;

    /**
     * The resource component's facet that will perform the actual re-configuration of the resource.
     */
    private ConfigurationFacet configurationFacet;

    /**
     * The type of resource that is being reconfigured.
     */
    private ResourceType resourceType;

    /**
     * The object to be notified when the configuration is done. May be <code>null</code> if there is no external server
     * service to be notified.
     */
    private ConfigurationServerService configurationServerService;

    private ConfigurationUtilityService configUtilService = new ConfigurationUtilityServiceImpl();

    public UpdateResourceConfigurationRunner(ConfigurationServerService configurationServerService, ResourceType type,
        ConfigManagement configMgmt, ConfigurationUpdateRequest request) {
        this.configurationServerService = configurationServerService; // may be null
        this.resourceType = type;
        this.configMgmt = configMgmt;
        this.request = request;
    }

    ConfigurationUpdateRequest getRequest() {
        return request;
    }

    ConfigManagement getConfigMgmt() {
        return configMgmt;
    }

    ResourceType getResourceType() {
        return resourceType;
    }

    ConfigurationServerService getConfigurationServerService() {
        return configurationServerService;
    }

    ConfigurationUtilityService getConfigUtilService() {
        return configUtilService;
    }

    void setConfigUtilService(ConfigurationUtilityService service) {
        configUtilService = service;
    }

    public void run() {
        try {
            call();
        } catch (Exception e) {
            log.error("Error while chaining run to call", e);
        }
    }

    public ConfigurationUpdateResponse call() throws Exception {
        ConfigurationUpdateResponse response;
        int requestId = request.getConfigurationUpdateId();
        try {
            response = new ConfigurationUpdateResponse(requestId, request.getConfiguration(), SUCCESS, null);
            try {
                configMgmt.executeUpdate(request.getResourceId(), request.getConfiguration());
            } catch (UpdateInProgressException e) {
                response.setStatus(INPROGRESS);
                response.setErrorMessage("Configuration facet did not indicate success or failure - assuming failure.");
            } catch (ConfigurationUpdateException e) {
                response.setStatus(FAILURE);
                response.setErrorMessage(e.getMessage());
            }

            ConfigurationDefinition configurationDefinition = resourceType.getResourceConfigurationDefinition();

            // Normalize and validate the config.
            configUtilService.normalizeConfiguration(response.getConfiguration(), configurationDefinition);
            List<String> errorMessages = configUtilService.validateConfiguration(response.getConfiguration(),
                configurationDefinition);
            for (String errorMessage : errorMessages) {
                log.warn("Plugin Error: Invalid " + resourceType.getName() + " Resource configuration returned by "
                    + resourceType.getPlugin() + " plugin - " + errorMessage);
            }

            // If it was successful, there is no need to waste bandwidth and send back the entire configuration again.
            // Just set it to null - the remote client will see it was a success and assume the configuration that was
            // saved is the same configuration that was passed in via the request.
            if (response.getStatus() == ConfigurationUpdateStatus.SUCCESS) {
                response.setConfiguration(null);
            }
        } catch (Throwable t) {
            log.error("Plugin Error: Exception thrown while updating Resource configuration for "
                    + resourceType.getName() + " Resource with id [" + request.getResourceId() + "].", t);
            response = new ConfigurationUpdateResponse(requestId, request.getConfiguration(), t);            
        }

        if (this.configurationServerService != null) {
            this.configurationServerService.completeConfigurationUpdate(response);
        }

        return response;
    }
}
