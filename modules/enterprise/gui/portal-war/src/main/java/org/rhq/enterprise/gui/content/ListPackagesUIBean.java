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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.InstalledPackageHistoryStatus;
import org.rhq.core.domain.content.composite.PackageListItemComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ListPackagesUIBean extends PagedDataTableUIBean {
    // Constants  --------------------------------------------

    public static final String MANAGED_BEAN_NAME = "ListPackagesUIBean";

    // Attributes  --------------------------------------------

    private Resource resource;
    private Integer selectedPackage;

    // Constructors  --------------------------------------------

    public ListPackagesUIBean() {
    }

    // Public  --------------------------------------------

    public String deleteSelectedInstalledPackages() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedPackages = FacesContextUtility.getRequest().getParameterValues("selectedPackages");

        ContentManagerLocal contentManager = LookupUtil.getContentManager();

        // Load installed packages for call to EJB
        Set<Integer> installedPackageIds = new HashSet<Integer>(selectedPackages.length);
        for (String installedPackageIdString : selectedPackages) {
            int deleteMeId = Integer.parseInt(installedPackageIdString);
            installedPackageIds.add(deleteMeId);
        }

        // Execute the delete
        try {
            contentManager.deletePackages(subject, resource.getId(), installedPackageIds);
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

    public List<SelectItem> getInstalledPackageStatusList() {
        List<SelectItem> items = new ArrayList<SelectItem>(5);
        items.add(new SelectItem(InstalledPackageHistoryStatus.INSTALLED.getDisplayName(),
            InstalledPackageHistoryStatus.INSTALLED.getDescription()));
        items.add(new SelectItem(InstalledPackageHistoryStatus.BEING_INSTALLED.getDisplayName(),
            InstalledPackageHistoryStatus.BEING_INSTALLED.getDescription()));
        items.add(new SelectItem(InstalledPackageHistoryStatus.FAILED.getDisplayName(),
            InstalledPackageHistoryStatus.FAILED.getDescription()));
        items.add(new SelectItem(InstalledPackageHistoryStatus.DELETED.getDisplayName(),
            InstalledPackageHistoryStatus.DELETED.getDescription()));
        return items;
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListInstalledPackagesDataModel(PageControlView.InstalledPackagesList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    public Integer getSelectedPackage() {
        return selectedPackage;
    }

    public void setSelectedPackage(Integer selectedPackage) {
        this.selectedPackage = selectedPackage;
    }

    // Inner Classes  --------------------------------------------

    /**
     * Data model for the resource's list of artifacts.
     */
    private class ListInstalledPackagesDataModel extends PagedListDataModel<PackageListItemComposite> {
        public ListInstalledPackagesDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<PackageListItemComposite> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            Resource requestResource = EnterpriseFacesContextUtility.getResourceIfExists();
            ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();

            if (requestResource == null) {
                requestResource = resource; // request not associated with a resource - use the resource we used before
            } else {
                resource = requestResource; // request switched the resource this UI bean is using
            }

            PageList<PackageListItemComposite> pageList = contentUIManager.getInstalledPackages(subject,
                requestResource.getId(), pc);

            return pageList;
        }
    }
}