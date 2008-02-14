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

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The JSF managed bean for the Edit General Properties page (/rhq/resource/inventory/edit-general.xhtml), which is
 * linked off the General Properties section of the main Inventory page.
 *
 * @author Ian Springer
 */
public class EditGeneralPropertiesUIBean {
    public static final String MANAGED_BEAN_NAME = "EditGeneralPropertiesUIBean";

    private static final String OUTCOME_SUCCESS = "success";

    private String name;
    private String description;
    private String location;

    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

    public EditGeneralPropertiesUIBean() {
        Resource resource = EnterpriseFacesContextUtility.getResource();
        this.name = resource.getName();
        this.description = resource.getDescription();
        this.location = resource.getLocation();
    }

    public String begin() {
        return OUTCOME_SUCCESS;
    }

    public String update() {
        // Get an up-to-date copy of the Resource from the DB in case anything else has changed since the page was first
        // displayed.
        Resource resource = this.resourceManager.getResourceById(EnterpriseFacesContextUtility.getSubject(),
            EnterpriseFacesContextUtility.getResource().getId());
        resource.setName(this.name);
        resource.setDescription(this.description);
        resource.setLocation(this.location);
        this.resourceManager.updateResource(EnterpriseFacesContextUtility.getSubject(), resource);
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

    public String getLocation() {
        return this.location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}