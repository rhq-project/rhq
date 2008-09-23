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
import java.util.Date;
import java.util.List;

import javax.faces.model.DataModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Bean used to handle artifact revision requests.
 *
 * @author Jason Dobies
 */
public class ListPackageHistoryUIBean extends PagedDataTableUIBean {
    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(this.getClass());

    private InstalledPackage currentPackage;

    // Public  --------------------------------------------

    public InstalledPackage getCurrentPackage() {
        if (currentPackage == null) {
            loadCurrentPackage();
        }

        return currentPackage;
    }

    /**
     * Wraps the package values side by side with an older package if one was selected so they can be displayed in the
     * same table.
     *
     * We no longer support comparing old and new packages since {@link InstalledPackage} no longer keeps any form
     * of historical data. This still remains in case we decide to port it later to support comparing versions.
     *
     * @return
     */
    public List<PackageTableDataValue> getPackageValues() {
        loadCurrentPackage();
        currentPackage = getCurrentPackage();

        if (currentPackage == null) {
            return null;
        }

        // See if an old package was requested
        String oldPackageIdString = FacesContextUtility.getRequest().getParameter("oldPackageId");

        return toCombinedValues(currentPackage, null);
    }

    /**
     * Converts the data in a package into a holder list usable in a data table.
     *
     * @param  current current data being displayed
     * @param  old     the old package version that is to be displayed side by side with the current
     *
     * @return list of holder objects describing the packages
     */
    private List<PackageTableDataValue> toCombinedValues(InstalledPackage current, InstalledPackage old) {
        List<PackageTableDataValue> results = new ArrayList<PackageTableDataValue>();

        results.add(new PackageTableDataValue("Name", current.getPackageVersion().getGeneralPackage().getName(), null));
        results.add(new PackageTableDataValue("Version", current.getPackageVersion().getDisplayVersion(),
            ((old != null) ? old.getPackageVersion().getDisplayVersion() : null)));
        results.add(new PackageTableDataValue("Architecture", current.getPackageVersion().getArchitecture().getName(),
            ((old != null) ? old.getPackageVersion().getArchitecture().getName() : null)));
        results.add(new PackageTableDataValue("File Name",
            (current.getPackageVersion().getFileSize() != null) ? current.getPackageVersion().getFileName() : null,
            (old != null) ? ((old.getPackageVersion().getFileSize() != null) ? old.getPackageVersion().getFileName()
                : null) : null));
        results.add(new PackageTableDataValue("File Size",
            (current.getPackageVersion().getFileSize() != null) ? current.getPackageVersion().getFileSize().toString()
                : null, (old != null) ? ((old.getPackageVersion().getFileSize() != null) ? old.getPackageVersion()
                .getFileSize().toString() : null) : null));
        results.add(new PackageTableDataValue("MD5", current.getPackageVersion().getMD5(), ((old != null) ? old
            .getPackageVersion().getMD5() : null)));
        results.add(new PackageTableDataValue("SHA256", current.getPackageVersion().getSHA256(), ((old != null) ? old
            .getPackageVersion().getSHA256() : null)));
        results.add(new PackageTableDataValue("Installation Date", dateToString(current.getInstallationDate()),
            dateToString(old.getInstallationDate())));
        results.add(new PackageTableDataValue("Owner", (current.getUser() != null) ? current.getUser().toString()
            : null, (old != null) ? ((old.getUser() != null) ? old.getUser().toString() : null) : null));

        // TODO: figure out how to know if the content is available
        /*
                results.add(new PackageTableDataValue("Content loaded to server?", Boolean.toString(current.getPackageVersion()
                    .getPackageBits() != null), ((old != null) ? Boolean
                    .toString(old.getPackageVersion().getPackageBits() != null) : null)));
        */

        // If there are no extra properties defined for this package type, we can stop here
        ConfigurationDefinition definition = current.getPackageVersion().getGeneralPackage().getPackageType()
            .getDeploymentConfigurationDefinition();
        if (definition == null) {
            return results;
        }

        /*
                Deployment configuration is no longer stored on the InstalledPackage. If we want this, we can have a query
                that looks for the configuration in the audit trail.

                Map<String, PropertyDefinition> propertyDefinitions = definition.getPropertyDefinitions();
                
                Configuration currentConfiguration = current.getDeploymentConfigurationValues();
                Configuration oldConfiguration = ((old != null) ? old.getDeploymentConfigurationValues() : null);

                Map<String, PropertySimple> currentProperties = ((currentConfiguration != null) ? currentConfiguration
                    .getSimpleProperties() : new HashMap<String, PropertySimple>());
                Map<String, PropertySimple> oldProperties = ((oldConfiguration != null) ? oldConfiguration
                    .getSimpleProperties() : new HashMap<String, PropertySimple>());

                for (String name : propertyDefinitions.keySet()) {
                    PropertyDefinitionSimple propertyDefinition = definition.getPropertyDefinitionSimple(name);
                    String propertyName = propertyDefinition.getDisplayName();

                    String currentPropertyValue;
                    String oldPropertyValue;

                    PropertySimple currentPropertySimple = currentProperties.get(name);
                    currentPropertyValue = ((currentPropertySimple != null) ? currentPropertySimple.getStringValue() : null);

                    PropertySimple oldPropertySimple = oldProperties.get(name);
                    oldPropertyValue = ((oldPropertySimple != null) ? oldPropertySimple.getStringValue() : null);

                    PackageTableDataValue packageTableDataValue = new PackageTableDataValue(propertyName, currentPropertyValue,
                        oldPropertyValue);
                    results.add(packageTableDataValue);
                }
        */

        results = new PageList<PackageTableDataValue>(results, results.size(), PageControl.getUnlimitedInstance());
        return results;
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListAllPackageVersionsDataModel(PageControlView.AllPackageVersionsList,
                "ListPackageHistoryUIBean");
        }

        return dataModel;
    }

    // Private  --------------------------------------------

    /**
     * Lazily loads the current package referenced in the request into this instance of the bean.
     */
    public void loadCurrentPackage() {
        if (currentPackage != null) {
            return;
        }

        int currentPackageId = Integer.parseInt(FacesContextUtility.getRequest().getParameter("currentPackageId"));

        ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
        currentPackage = contentUIManager.getInstalledPackage(currentPackageId);
    }

    /**
     * Converts the date into a string value, taking into account whether or not the date is null.
     *
     * @param  date being converted into a string
     *
     * @return string representation of the date if the date is not null; empty string otherwise
     */
    private String dateToString(Long date) {
        if (date == null) {
            return "";
        } else {
            return new Date(date).toString();
        }
    }

    // Inner Classes  --------------------------------------------

    /**
     * Holder class used to combine first class revision data (i.e. md5, size) with the artifact specific values (i.e.
     * the values in the optional Configuration instance). The data from each will be loaded into objects of this class
     * and displayed in the UI in the same fashion.
     */
    public class PackageTableDataValue {
        private String name;
        private String currentValue;
        private String oldValue;

        public PackageTableDataValue(String name, String currentValue, String oldValue) {
            this.name = name;
            this.currentValue = currentValue;
            this.oldValue = oldValue;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCurrentValue() {
            return currentValue;
        }

        public void setCurrentValue(String currentValue) {
            this.currentValue = currentValue;
        }

        public String getOldValue() {
            return oldValue;
        }

        public void setOldValue(String oldValue) {
            this.oldValue = oldValue;
        }
    }

    private class ListAllPackageVersionsDataModel extends PagedListDataModel<InstalledPackageHistory> {
        public ListAllPackageVersionsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<InstalledPackageHistory> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            Resource resource = EnterpriseFacesContextUtility.getResourceIfExists();
            int currentInstalledPackageId = Integer.parseInt(FacesContextUtility.getRequest().getParameter(
                "currentPackageId"));

            ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();

            // Load the current installed package so we can get the general package in question
            InstalledPackage currentInstalledPackage = contentUIManager.getInstalledPackage(currentInstalledPackageId);
            Package generalPackage = currentInstalledPackage.getPackageVersion().getGeneralPackage();

            PageList<InstalledPackageHistory> result = contentUIManager.getInstalledPackageHistory(subject, resource
                .getId(), generalPackage.getId(), pc);
            return result;
        }
    }
}