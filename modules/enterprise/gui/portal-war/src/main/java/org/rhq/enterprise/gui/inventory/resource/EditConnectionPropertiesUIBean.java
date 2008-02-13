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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.gui.configuration.ConfigurationMaskingUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A JSF managed bean that backs the /rhq/resource/inventory/edit-connection.xhtml page.
 *
 * @author Ian Springer
 */
public class EditConnectionPropertiesUIBean extends ViewConnectionPropertiesUIBean {
    public static final String MANAGED_BEAN_NAME = "EditConnectionPropertiesUIBean";

    private static final String OUTCOME_SUCCESS = "success";
    private static final String OUTCOME_FAILURE = "failure";

    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

    public String begin() {
        return OUTCOME_SUCCESS;
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
            return OUTCOME_SUCCESS;
        } else {
            // non-null error message, means some exception was thrown, caught, and put into the update details
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Unable to update this resource's connection properties.", errorMessage);
            return OUTCOME_FAILURE;
        }
    }

    public String cancel() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Connection properties not updated.");
        return OUTCOME_SUCCESS;
    }
}