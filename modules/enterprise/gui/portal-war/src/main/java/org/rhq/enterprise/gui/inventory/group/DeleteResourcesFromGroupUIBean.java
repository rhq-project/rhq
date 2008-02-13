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
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.util.LookupUtil;

public class DeleteResourcesFromGroupUIBean {
    public String removeSelectedResources() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        ResourceGroup group = EnterpriseFacesContextUtility.getResourceGroup();
        String[] selectedResources = FacesContextUtility.getRequest().getParameterValues("selectedResources");

        int i = 0;
        if ((selectedResources == null) || (selectedResources.length == 0)) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_WARN, "Nothing deleted as resource was selected");
            return "successOrFailure";
        }

        Integer[] selectedResourceInts = new Integer[selectedResources.length];
        for (String resourceIdString : selectedResources) {
            selectedResourceInts[i++] = Integer.parseInt(resourceIdString);
        }

        try {
            LookupUtil.getResourceGroupManager().removeResourcesFromGroup(subject, group.getId(), selectedResourceInts);

            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Removed the selected resources");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, ThrowableUtil.getAllMessages(e));
        }

        return "successOrFailure";
    }
}