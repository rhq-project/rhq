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
package org.rhq.enterprise.gui.content;

import javax.faces.application.FacesMessage;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Collects the necessary data for requesting content for an artifact and performs the retrieve call.
 *
 * @author Jason Dobies
 */
public class RetrieveContentUIBean {
    // Attributes  --------------------------------------------

    private Integer selectedInstalledPackage;

    // Actions  --------------------------------------------

    public String retrieveContent() {
        // Collect the necessary information
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Resource resource = EnterpriseFacesContextUtility.getResource();

        // Send the request to the bean
        ContentManagerLocal contentManager = LookupUtil.getContentManager();

        try {
            contentManager.retrieveBitsFromResource(subject, resource.getId(), selectedInstalledPackage);
        } catch (Exception e) {
            String errorMessages = ThrowableUtil.getAllMessages(e);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Failed to send retrieve content request to the agent. Cause: " + errorMessages);
            return "successOrFailure";
        }

        // Sleep just enough to let "fast" operations complete before being redirected
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            // Let this thread be interrupted without user warning
        }

        // If we got this far, we didn't hit an error, so add a message for the request submission request
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
            "Retrieve content request successfully submitted to the agent.");
        return "successOrFailure";
    }

    // Public  --------------------------------------------

    public Integer getSelectedInstalledPackage() {
        return selectedInstalledPackage;
    }

    public void setSelectedInstalledPackage(Integer selectedInstalledPackage) {
        this.selectedInstalledPackage = selectedInstalledPackage;
    }
}