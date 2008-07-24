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

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.faces.application.FacesMessage;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.content.*;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jason Dobies
 */
public class DeletePackagesUIBean {

    private Resource resource;
    private String[] packageIdsToDelete;
    private List<InstalledPackage> packagesToDelete;
    private String notes;

    public String beginDeleteWorkflow() {
        getPackageIdsToDelete();
        return "beginDeleteWorkflow";
    }

    public String deleteSelectedInstalledPackages() {

        if (notes != null && notes.length() > 512) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Package notes must be 512 characters or less.");
            return null;
        }

        Subject subject = EnterpriseFacesContextUtility.getSubject();
        resource = EnterpriseFacesContextUtility.getResource();
        String[] selectedPackages =
            (String[])FacesContextUtility.getRequest().getSession().getAttribute("packageIdsToDelete");

        // Load installed packages for call to EJB
        Set<Integer> installedPackageIds = new HashSet<Integer>(selectedPackages.length);
        for (String installedPackageIdString : selectedPackages) {
            int deleteMeId = Integer.parseInt(installedPackageIdString);
            installedPackageIds.add(deleteMeId);
        }

        // Execute the delete
        try {
            ContentManagerLocal contentManager = LookupUtil.getContentManager();
            contentManager.deletePackages(subject, resource.getId(), installedPackageIds, notes);
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete packages. Cause: " + e);
        }

        // Sleep just enough to let "fast" operations complete before being redirected
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            // Let this thread be interrupted without user warning
        }

        return "successOrFailure";
    }


    public Resource getResource() {
        if (resource == null) {
            resource = EnterpriseFacesContextUtility.getResource();
        }

        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public List<InstalledPackage> getPackagesToDelete() {
        if (packagesToDelete == null) {

            if (getPackageIdsToDelete() == null)
                return Collections.EMPTY_LIST;

            packagesToDelete = new ArrayList<InstalledPackage>(packageIdsToDelete.length);

            ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();

            for (String installedPackageId : packageIdsToDelete) {
                int iInstalledPackageId = Integer.parseInt(installedPackageId);
                InstalledPackage installedPackage = contentUIManager.getInstalledPackage(iInstalledPackageId);
                packagesToDelete.add(installedPackage);
            }
        }

        return packagesToDelete;
    }

    public void setPackagesToDelete(List<InstalledPackage> packagesToDelete) {
        this.packagesToDelete = packagesToDelete;
    }

    public String[] getPackageIdsToDelete() {
        HttpServletRequest request = FacesContextUtility.getRequest();
        HttpSession session = request.getSession();

        if (request.getParameterValues("selectedPackages") != null) {
            packageIdsToDelete = request.getParameterValues("selectedPackages");
            session.setAttribute("packageIdsToDelete", packageIdsToDelete);
        }
        else {
            packageIdsToDelete = (String[])session.getAttribute("packageIdsToDelete");
        }

        return packageIdsToDelete;
    }

    public void setPackageIdsToDelete(String[] packageIdsToDelete) {
        this.packageIdsToDelete = packageIdsToDelete;
    }

    public String getNotes() {
        if (notes == null) {
            List<InstalledPackage> installedPackages = getPackagesToDelete();

            StringBuffer sb = new StringBuffer("Packages: ");
            int counter = 0;
            for (InstalledPackage installedPackage : installedPackages) {
                PackageVersion packageVersion = installedPackage.getPackageVersion();
                Package generalPackage = packageVersion.getGeneralPackage();

                String version = packageVersion.getDisplayVersion() != null ?
                    packageVersion.getDisplayVersion() : packageVersion.getVersion();

                String packageToAppend = generalPackage.getName() + " " + version;

                // Don't generate notes that would fail our own validation
                if (sb.toString().length() + packageToAppend.length() > 508) {

                    // If we're not at the last package yet, add ... to show there were more
                    if (counter != (installedPackages.size() - 1)) {
                        sb.append("...");
                        break;
                    }

                    // If we are at the last package, see if this one will fit, otherwise add ...
                    if (sb.toString().length() + packageToAppend.length() <= 511) {
                        sb.append(packageToAppend);
                    }
                    else {
                        sb.append("...");
                    }

                    break;
                }

                sb.append(packageToAppend);

                if (counter++ < (installedPackages.size() - 1))
                    sb.append(", ");
            }

            notes = sb.toString();
        }

        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
