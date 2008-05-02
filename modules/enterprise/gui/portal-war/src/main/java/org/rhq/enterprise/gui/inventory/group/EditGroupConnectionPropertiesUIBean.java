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

import javax.faces.application.FacesMessage;

import org.quartz.SchedulerException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.gui.configuration.ConfigurationMaskingUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class EditGroupConnectionPropertiesUIBean extends ViewGroupConnectionPropertiesUIBean {
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
        ResourceGroup resourceGroup = EnterpriseFacesContextUtility.getResourceGroup();
        Configuration aggregateConfiguration = getConfiguration();

        try {
            this.configurationManager.scheduleAggregatePluginConfigurationUpdate(subject, resourceGroup.getId(),
                aggregateConfiguration);

            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                "Group connection property update has been submitted for asynchronous processing.");
            return OUTCOME_SUCCESS;
        } catch (SchedulerException se) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "There was an error scheduling the group connection property update");
            return OUTCOME_FAILURE;
        }
    }

    public String cancel() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "No connection properties were updated.");
        return OUTCOME_SUCCESS;
    }
}