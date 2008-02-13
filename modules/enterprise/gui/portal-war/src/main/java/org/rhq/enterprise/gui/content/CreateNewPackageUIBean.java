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

import java.io.IOException;
import java.io.InputStream;
import javax.faces.application.FacesMessage;
import org.apache.commons.fileupload.FileItem;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Collects data necessary for creating an artifact and provides actions to perform the create.
 *
 * @author Jason Dobies
 */
public class CreateNewPackageUIBean {
    // Attributes  --------------------------------------------

    private PackageType packageType;

    private String packageName;

    private Integer selectedType;

    // Actions  --------------------------------------------

    public String createPackage() {
        // Collect the necessary information
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Resource resource = EnterpriseFacesContextUtility.getResource();

        String selectedTypeParameter = FacesContextUtility.getRequest().getParameter("selectedType");
        FileItem fileItem = (FileItem) FacesContextUtility.getRequest().getAttribute("uploadForm:uploadFile");

        // Validate
        if ((selectedTypeParameter == null) || selectedTypeParameter.equals("")) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "An artifact type must be selected");
            return null;
        }

        if ((packageName == null) || packageName.equals("")) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "An artifact name must be entered");
            return null;
        }

        if ((fileItem.getName() == null) || fileItem.getName().equals("")) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "An artifact file must be specified");
            return null;
        }

        // Read in the file
        selectedType = Integer.parseInt(selectedTypeParameter);

        InputStream artifactInputStream;

        try {
            artifactInputStream = fileItem.getInputStream();
        } catch (IOException e) {
            String errorMessages = ThrowableUtil.getAllMessages(e);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to retrieve the input stream. Cause: "
                + errorMessages);
            return "successOrFailure";
        }

        // Send the request to the bean
        ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();

        try {
            packageType = contentUIManager.getPackageType(selectedType);

            // TODO: jdob, Nov 21, 2007: There are now two concepts, creating a package (i.e. uploading it to the server) and deploying
            // Need to determine where the differences lie and finish this
        } catch (Exception e) {
            String errorMessages = ThrowableUtil.getAllMessages(e);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Failed to send create request to the agent. Cause: " + errorMessages);
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
            "Create request successfully submitted to the agent.");
        return "successOrFailure";
    }

    // Accessors  --------------------------------------------

    public Integer getSelectedType() {
        return selectedType;
    }

    public void setSelectedType(Integer selectedType) {
        this.selectedType = selectedType;
    }

    public PackageType getPackageType() {
        return packageType;
    }

    public void setPackageType(PackageType packageType) {
        this.packageType = packageType;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}