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

import javax.faces.application.FacesMessage;

import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.gui.configuration.ConfigurationMaskingUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.configuration.AbstractConfigurationUIBean;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A JSF managed bean that backs the Connection Properties section of the /rhq/resource/inventory/view.xhtml page.
 *
 * @author Ian Springer
 */
public class ConnectionPropertiesUIBean extends AbstractConfigurationUIBean {
    public static final String MANAGED_BEAN_NAME = "ConnectionPropertiesUIBean";

    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

    public ConnectionPropertiesUIBean() {
        removeSessionScopedBeanIfInView("/rhq/resource/inventory/view-connection.xhtml",
            ConnectionPropertiesUIBean.class);
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
        Configuration configuration = this.configurationManager.getPluginConfiguration(EnterpriseFacesContextUtility
            .getSubject(), EnterpriseFacesContextUtility.getResource().getId());
        if (configuration != null) {
            ConfigurationMaskingUtility.maskConfiguration(configuration, getConfigurationDefinition());
        }
        return configuration;
    }

    protected int getConfigurationDefinitionKey() {
        return EnterpriseFacesContextUtility.getResource().getResourceType().getId();
    }

    protected int getConfigurationKey() {
        return EnterpriseFacesContextUtility.getResource().getId();
    }

    public String getNullConfigurationDefinitionMessage() {
        return "Can not find this resource's plugin configuration definition.";
    }

    public String getNullConfigurationMessage() {
        return "This resource's plugin configuration has not been initialized.";
    }

    public String edit() {
        return SUCCESS_OUTCOME;
    }

    public String history() {
        return SUCCESS_OUTCOME;
    }

    public String update() {
        ConfigurationMaskingUtility.unmaskConfiguration(getConfiguration(), getConfigurationDefinition());

        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Resource resource = EnterpriseFacesContextUtility.getResource();
        Configuration newConfiguration = getConfiguration();

        PluginConfigurationUpdate update = this.configurationManager.updatePluginConfiguration(subject, resource
            .getId(), newConfiguration);

        String errorMessage = update.getErrorMessage();
        if (errorMessage == null) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Connection properties updated.");
            clearConfiguration();
            return SUCCESS_OUTCOME;
        } else {
            // non-null error message, means some exception was thrown, caught, and put into the update details
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Unable to update this resource's connection properties.", errorMessage);
            return FAILURE_OUTCOME;
        }
    }

    public String cancel() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Connection properties not updated.");
        return SUCCESS_OUTCOME;
    }

    public String finish() {
        return SUCCESS_OUTCOME;
    }
}