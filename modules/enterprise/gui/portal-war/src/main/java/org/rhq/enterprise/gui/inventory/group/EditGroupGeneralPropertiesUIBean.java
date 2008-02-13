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
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupUpdateException;
import org.rhq.enterprise.server.util.LookupUtil;

public class EditGroupGeneralPropertiesUIBean {
    public static final String MANAGED_BEAN_NAME = "EditGroupGeneralPropertiesUIBean";

    private static final String OUTCOME_SUCCESS = "success";
    private static final String OUTCOME_FAILURE = "failure";

    private String name;
    private String description;

    private ResourceGroupManagerLocal resourceGroupManager = LookupUtil.getResourceGroupManager();

    public EditGroupGeneralPropertiesUIBean() {
        ResourceGroup resourceGroup = EnterpriseFacesContextUtility.getResourceGroup();

        this.name = resourceGroup.getName();
        this.description = resourceGroup.getDescription();
    }

    public String begin() {
        return OUTCOME_SUCCESS;
    }

    public String update() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        ResourceGroup resourceGroup = EnterpriseFacesContextUtility.getResourceGroup();

        resourceGroup.setName(this.name);
        resourceGroup.setDescription(this.description);

        try {
            this.resourceGroupManager.updateResourceGroup(subject, resourceGroup);
        } catch (ResourceGroupUpdateException rgue) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Problem updating group: " + rgue.getMessage());
            return OUTCOME_FAILURE;
        }

        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "General properties updated.");
        return OUTCOME_SUCCESS;
    }

    public String cancel() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "General properties not updated.");
        return OUTCOME_SUCCESS;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}