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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.composite.PackageListItemComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.converter.SelectItemUtils;
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

    private SelectItem[] packageTypes;
    private String packageTypeFilter;

    private SelectItem[] packageVersions;
    private String packageVersionFilter;

    private ContentManagerLocal contentManager = LookupUtil.getContentManager();
    private ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();

    // Constructors  --------------------------------------------

    public ListPackagesUIBean() {
    }

    // Public  --------------------------------------------

    public String deleteSelectedInstalledPackages() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedPackages = FacesContextUtility.getRequest().getParameterValues("selectedPackages");

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

    /**
     * If the current resource is a content-backed resource, this call will return the name of the package type
     * that does the resource backing. This is used to disallow the user to undeploy that package.
     *
     * @return name of a package type if the resource is content-backed; <code>null</code> otherwise
     */
    public String getContentBackedResourceTypeName() {
        resource = EnterpriseFacesContextUtility.getResource();
        ResourceType resourceType = resource.getResourceType();

        if (resourceType.getCreationDataType() != ResourceCreationDataType.CONTENT)
            return null;

        ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
        List<PackageType> packageTypes = contentUIManager.getPackageTypes(resourceType.getId());
        for (PackageType type : packageTypes) {
            if (type.isCreationData())
                return type.getName();
        }

        return null;
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

            String packageTypeFilter = ListPackagesUIBean.this.getPackageTypeFilter();
            String packageVersionFilter = ListPackagesUIBean.this.getPackageVersionFilter();

            if (requestResource == null) {
                requestResource = resource; // request not associated with a resource - use the resource we used before
            } else {
                resource = requestResource; // request switched the resource this UI bean is using
            }

            PageList<PackageListItemComposite> pageList = contentUIManager.getInstalledPackages(subject,
                requestResource.getId(), packageTypeFilter, packageVersionFilter, pc);

            return pageList;
        }
    }

    public SelectItem[] getPackageTypes() {
        if (this.packageTypes == null) {
            List<IntegerOptionItem> items = contentUIManager.getInstalledPackageTypes(EnterpriseFacesContextUtility
                .getSubject(), EnterpriseFacesContextUtility.getResource().getId());
            this.packageTypes = SelectItemUtils.convertFromListOptionItem(items, true);
        }
        return this.packageTypes;
    }

    public void setPackageTypes(SelectItem[] packageTypes) {
        this.packageTypes = packageTypes;
    }

    public String getPackageTypeFilter() {
        if (packageTypeFilter == null) {
            packageTypeFilter = SelectItemUtils.getSelectItemFilter("contentForm:packageTypeFilter");
        }
        return packageTypeFilter;
    }

    public void setPackageTypeFilter(String packageTypeFilter) {
        this.packageTypeFilter = packageTypeFilter;
    }

    public SelectItem[] getPackageVersions() {
        if (this.packageVersions == null) {
            List<String> items = contentUIManager.getInstalledPackageVersions(EnterpriseFacesContextUtility
                .getSubject(), EnterpriseFacesContextUtility.getResource().getId());
            this.packageVersions = SelectItemUtils.convertFromListString(items, true);
        }
        return packageVersions;
    }

    public void setPackageVersions(SelectItem[] packageVersions) {
        this.packageVersions = packageVersions;
    }

    public String getPackageVersionFilter() {
        if (packageVersionFilter == null) {
            packageVersionFilter = SelectItemUtils.getSelectItemFilter("contentForm:packageVersionFilter");
        }
        return packageVersionFilter;
    }

    public void setPackageVersionFilter(String packageVersionFilter) {
        this.packageVersionFilter = packageVersionFilter;
    }
}