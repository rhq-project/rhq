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
package org.rhq.enterprise.gui.operation.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.core.plugin.PluginReloadedException;
import org.rhq.enterprise.server.operation.GroupOperationSchedule;
import org.rhq.enterprise.server.operation.OperationDefinitionNotFoundException;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.operation.OperationSchedule;
import org.rhq.enterprise.server.operation.ResourceOperationSchedule;
import org.rhq.enterprise.server.util.LookupUtil;

public class OperationParameters {
    private Configuration configuration;
    private ConfigurationDefinition configurationDefinition;

    public OperationParameters(OperationSchedule schedule) {
        this.configuration = schedule.getParameters();

        String operationName = schedule.getOperationName();
        ResourceType type = null;

        if (schedule instanceof ResourceOperationSchedule) {
            Resource resource = ((ResourceOperationSchedule) schedule).getResource();
            type = resource.getResourceType();
        } else if (schedule instanceof GroupOperationSchedule) {
            ResourceGroup group = ((GroupOperationSchedule) schedule).getGroup();
            type = group.getResourceType();
        } else {
            throw new IllegalArgumentException("OperationParameters does not support objects of type "
                + schedule.getClass().getSimpleName());
        }

        OperationManagerLocal operationManager = LookupUtil.getOperationManager();
        OperationDefinition definition;

        try {
            definition = operationManager
                .getOperationDefinitionByResourceTypeAndName(type.getId(), operationName, true);
        } catch (OperationDefinitionNotFoundException odnfe) {
            throw new PluginReloadedException("The plugin for " + type.getName()
                + " has been updated since this schedule was created, and the operation " + operationName
                + " no longer exists");
        }

        this.configurationDefinition = definition.getParametersConfigurationDefinition();
    }

    public OperationParameters(OperationHistory history) {
        this.configuration = history.getParameters();
        this.configurationDefinition = history.getOperationDefinition().getParametersConfigurationDefinition();
    }

    @Nullable
    public ConfigurationDefinition getConfigurationDefinition() {
        return this.configurationDefinition;
    }

    public void setConfigurationDefinition(@NotNull ConfigurationDefinition configurationDefinition) {
        this.configurationDefinition = configurationDefinition;
    }

    @Nullable
    public Configuration getConfiguration() {
        return this.configuration;
    }

    public void setConfiguration(@NotNull Configuration configuration) {
        this.configuration = configuration;
    }

    public String getNullConfigurationDefinitionMessage() {
        return "This resource operation does not take any arguments.";
    }

    public String getNullConfigurationMessage() {
        return "There was an error looking up this operation's parameters.";
    }
}