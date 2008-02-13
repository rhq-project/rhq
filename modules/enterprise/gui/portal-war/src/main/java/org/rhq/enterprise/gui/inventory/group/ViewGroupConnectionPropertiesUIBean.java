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
package org.rhq.enterprise.gui.inventory.group;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.group.AbstractAggregateConfigurationUpdate;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.gui.configuration.ConfigurationMaskingUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ViewGroupConnectionPropertiesUIBean {
    public static final String MANAGED_BEAN_NAME = "ViewGroupConnectionPropertiesUIBean";

    private static final String SUCCESS_OUTCOME = "success";

    protected ConfigurationDefinition configurationDefinition;
    protected Configuration configuration;
    protected ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

    public ViewGroupConnectionPropertiesUIBean() {
        this.configurationDefinition = lookupConfigurationDefinition();
        if (this.configurationDefinition != null) {
            this.configuration = lookupConfiguration();
        }
    }

    @Nullable
    public ConfigurationDefinition getConfigurationDefinition() {
        return this.configurationDefinition;
    }

    public void setConfigurationDefinition(@NotNull
    ConfigurationDefinition configurationDefinition) {
        this.configurationDefinition = configurationDefinition;
    }

    @Nullable
    public Configuration getConfiguration() {
        return this.configuration;
    }

    public void setConfiguration(@NotNull
    Configuration configuration) {
        this.configuration = configuration;
    }

    @Nullable
    protected ConfigurationDefinition lookupConfigurationDefinition() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        ResourceGroup resourceGroup = EnterpriseFacesContextUtility.getResourceGroup();
        ResourceType resourceType = resourceGroup.getResourceType();

        ConfigurationDefinition configurationDefinition = null;
        configurationDefinition = this.configurationManager.getPluginConfigurationDefinitionForResourceType(subject,
            resourceType.getId());
        return configurationDefinition;
    }

    @Nullable
    protected Configuration lookupConfiguration() {
        ResourceGroup compatibleGroup = EnterpriseFacesContextUtility.getResourceGroup();
        List<Configuration> pluginConfigurations = null;
        Configuration aggregateConfiguration = null;

        try {
            pluginConfigurations = configurationManager.getPluginConfigurationsForCompatibleGroup(compatibleGroup);
            aggregateConfiguration = AbstractAggregateConfigurationUpdate
                .getAggregateConfiguration(pluginConfigurations);

            if (aggregateConfiguration != null) {
                ConfigurationMaskingUtility.maskConfiguration(aggregateConfiguration, this.configurationDefinition);
            }
        } catch (IllegalArgumentException iae) {
            // do nothing, let null bubble up so that this object finishes constructing itself
        }

        return aggregateConfiguration;
    }

    public String editConfiguration() {
        return SUCCESS_OUTCOME;
    }

    public String getNullConfigurationDefinitionMessage() {
        return "Can not find calculate an effective plugin configuration definition for this group.";
    }

    public String getNullConfigurationMessage() {
        return "Some resource in this group does not have its plugin configuration initialized.";
    }
}