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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * JSF Bean for handling deleting resources.
 *
 * @author Jason Dobies
 */
public class DeleteResourcesUIBean {
    // Constants  --------------------------------------------

    private static final int NUM_DISPLAYED_FAILURES = 3;

    // Actions  --------------------------------------------

    public String deleteSelectedResources() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedResources = FacesContextUtility.getRequest().getParameterValues("selectedResources");

        ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();

        // Holds the results of each call
        List<String> successes = new ArrayList<String>();
        Map<String, String> failures = new HashMap<String, String>();

        // Attempt delete for each selected resource
        if (selectedResources == null) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_WARN, "No resources selected for deletion");
            return "successOrFailure";
        }

        for (String resourceIdString : selectedResources) {
            try {
                int resourceId = Integer.parseInt(resourceIdString);
                resourceFactoryManager.deleteResource(subject, resourceId);
                successes.add(resourceIdString);
            } catch (Exception e) {
                String errorMessages = ThrowableUtil.getAllMessages(e, true);
                failures.put(resourceIdString, errorMessages);
            }
        }

        // If there was at least one success, wrap all into the same success message
        if (successes.size() > 0) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted the following resources: "
                + StringUtility.getListAsDelimitedString(successes));
        }

        // For each failure, show the reason
        // Cap this so it doesn't grow out of control (assumes most deletes will be of the same resource type, in which
        // case chances are the reasons each delete failed should be the same)
        int errorCount = 0;
        List<String> additionalErrorIds = new ArrayList<String>();
        for (String errorResourceId : failures.keySet()) {
            if (errorCount < NUM_DISPLAYED_FAILURES) {
                String errorMessage = failures.get(errorResourceId);
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete resource: "
                    + errorResourceId + ". Cause: " + errorMessage);
            } else {
                additionalErrorIds.add(errorResourceId);
            }

            errorCount++;
        }

        // If the cap was passed, display another error message informing the user of which other resources failed
        if (additionalErrorIds.size() > 0) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Full errors not shown for resources that failed to delete: "
                    + StringUtility.getListAsDelimitedString(additionalErrorIds));
        }

        return "successOrFailure";
    }
}