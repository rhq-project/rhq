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
package org.rhq.enterprise.gui.configuration.resource;

import javax.faces.application.FacesMessage;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.gui.configuration.ConfigurationMaskingUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

/**
 * @author Ian Springer
 */
public class ExistingResourceConfigurationUIBean extends AbstractResourceConfigurationUIBean {
    public static final String MANAGED_BEAN_NAME = "ExistingResourceConfigurationUIBean";

    // =========== actions ===========

    public String editConfiguration() {
        return SUCCESS_OUTCOME;
    }

    public String updateConfiguration() {
        ConfigurationMaskingUtility.unmaskConfiguration(getConfiguration(), getConfigurationDefinition());
        int resourceId = EnterpriseFacesContextUtility.getResource().getId();
        AbstractResourceConfigurationUpdate updateRequest = this.configurationManager.updateResourceConfiguration(
            EnterpriseFacesContextUtility.getSubject(), resourceId, getConfiguration());
        if (updateRequest.getStatus() == ConfigurationUpdateStatus.FAILURE) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Configuration update request with id "
                + updateRequest.getId() + " failed.", updateRequest.getErrorMessage());
            return FAILURE_OUTCOME;
        } else {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Configuration update request with id "
                + updateRequest.getId() + " sent to agent.");
            clearConfiguration();
            return SUCCESS_OUTCOME;
        }
    }

    public String finishAddMap() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Map added.");
        return SUCCESS_OUTCOME;
    }

    public String finishEditMap() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Map updated.");
        return SUCCESS_OUTCOME;
    }

    // =========== impls of superclass abstract methods ===========

    protected int getConfigurationDefinitionKey() {
        return EnterpriseFacesContextUtility.getResource().getResourceType().getId();
    }

    @Nullable
    protected ConfigurationDefinition lookupConfigurationDefinition() {
        int resourceTypeId = EnterpriseFacesContextUtility.getResource().getResourceType().getId();
        ConfigurationDefinition configurationDefinition = this.configurationManager
            .getResourceConfigurationDefinitionForResourceType(EnterpriseFacesContextUtility.getSubject(),
                resourceTypeId);
        return configurationDefinition;
    }

    protected int getConfigurationKey() {
        return EnterpriseFacesContextUtility.getResource().getId();
    }

    @Nullable
    protected Configuration lookupConfiguration() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        int resourceId = EnterpriseFacesContextUtility.getResource().getId();
        AbstractResourceConfigurationUpdate configurationUpdate = this.configurationManager
            .getLatestResourceConfigurationUpdate(subject, resourceId);
        Configuration configuration = (configurationUpdate != null) ? configurationUpdate.getConfiguration() : null;
        if (configuration != null) {
            ConfigurationMaskingUtility.maskConfiguration(configuration, getConfigurationDefinition());
        }

        return configuration;
    }
}