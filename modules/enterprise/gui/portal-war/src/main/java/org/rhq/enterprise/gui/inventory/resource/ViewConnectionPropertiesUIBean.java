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
package org.rhq.enterprise.gui.inventory.resource;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.gui.configuration.ConfigurationMaskingUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A JSF managed bean that backs the Connection Properties section of the /rhq/resource/inventory/view.xhtml page.
 *
 * @author Ian Springer
 */
public class ViewConnectionPropertiesUIBean {
    public static final String MANAGED_BEAN_NAME = "ViewConnectionPropertiesUIBean";

    private static final String SUCCESS_OUTCOME = "success";

    private ConfigurationDefinition configurationDefinition;
    private Configuration configuration;

    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

    public ViewConnectionPropertiesUIBean() {
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
        int resourceTypeId = EnterpriseFacesContextUtility.getResource().getResourceType().getId();
        ConfigurationDefinition configurationDefinition = this.configurationManager
            .getPluginConfigurationDefinitionForResourceType(EnterpriseFacesContextUtility.getSubject(), resourceTypeId);
        return configurationDefinition;
    }

    @Nullable
    protected Configuration lookupConfiguration() {
        Configuration configuration = this.configurationManager.getCurrentPluginConfiguration(
            EnterpriseFacesContextUtility.getSubject(), EnterpriseFacesContextUtility.getResource().getId());
        if (configuration != null) {
            ConfigurationMaskingUtility.maskConfiguration(configuration, this.configurationDefinition);
        }

        return configuration;
    }

    public String editConfiguration() {
        return SUCCESS_OUTCOME;
    }

    public String getNullConfigurationDefinitionMessage() {
        return "Can not find this resource's plugin configuration definition.";
    }

    public String getNullConfigurationMessage() {
        return "This resource's plugin configuration has not been initialized.";
    }
}