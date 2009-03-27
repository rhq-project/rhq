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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.richfaces.model.UploadItem;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.configuration.ConfigurationMaskingUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Handles the workflow for creating a new package-backed resource.
 *
 * @author Jason Dobies
 * @author Ian Springer
 */
public class CreateNewPackageChildResourceUIBean {
    private final Log log = LogFactory.getLog(this.getClass());

    // Constants  --------------------------------------------

    public static final String MANAGED_BEAN_NAME = "CreateNewPackageChildResourceUIBean";

    private static final String OUTCOME_SUCCESS_OR_FAILURE = "successOrFailure";
    private static final String OUTCOME_SUCCESS = "success";
    private static final String OUTCOME_CANCEL = "cancel";

    // Attributes  --------------------------------------------

    private ResourceType resourceType;
    private PackageType packageType;
    private String packageVersion;

    private int selectedArchitectureId;

    private CreateResourceHistory retryCreateItem;
    private ConfigurationDefinition configurationDefinition;
    private Configuration configuration;

    public CreateNewPackageChildResourceUIBean() {
        this.resourceType = lookupResourceType();
        this.packageType = lookupPackageType();
        this.configurationDefinition = lookupConfigurationDefinition();
        this.configuration = lookupConfiguration();
    }

    // Actions  --------------------------------------------

    /**
     * Performs the creation of an package-backed resource.
     *
     * @return outcome of the creation attempt
     */
    public String createResource() {
        Subject user = EnterpriseFacesContextUtility.getSubject();

        UploadNewChildPackageUIBean uploadUIBean;
        uploadUIBean = FacesContextUtility.getManagedBean(UploadNewChildPackageUIBean.class);
        UploadItem fileItem = uploadUIBean.getFileItem();

        // Validate
        if ((fileItem == null) || fileItem.getFile() == null) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "A package file must be uploaded");
            return null;
        }

        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
        ConfigurationDefinition pluginConfigurationDefinition = configurationManager
            .getPluginConfigurationDefinitionForResourceType(user, resourceType.getId());

        Configuration pluginConfiguration = null;
        if (pluginConfigurationDefinition != null) {
            pluginConfiguration = pluginConfigurationDefinition.getDefaultTemplate().getConfiguration();
        }

        try {
            InputStream packageContentStream;
            try {
                log.debug("Streaming new package bits from uploaded file: " + fileItem.getFile());
                packageContentStream = new FileInputStream(fileItem.getFile());
            } catch (IOException e) {
                String errorMessages = ThrowableUtil.getAllMessages(e);
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "Failed to retrieve the input stream. Cause: " + errorMessages);
                return OUTCOME_SUCCESS_OR_FAILURE;
            }

            // If the type does not support architectures, load the no architecture entity and use that
            if (!packageType.isSupportsArchitecture()) {
                ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
                Architecture noArchitecture = contentUIManager.getNoArchitecture();

                selectedArchitectureId = noArchitecture.getId();
            }

            // Collect data for create call
            Resource parentResource = EnterpriseFacesContextUtility.getResource();
            Configuration deployTimeConfiguration = getConfiguration();
            ConfigurationMaskingUtility.unmaskConfiguration(deployTimeConfiguration, getConfigurationDefinition());
            String packageName = fileItem.getFileName();

            // some browsers (IE in particular) passes an absolute filename, we just want the name of the file, no paths
            if (packageName != null) {
                packageName = new File(packageName).getName();
            }

            // For JON 2.0 RC3, no longer request the package version on a package-backed create, simply
            // use the timestamp. The timestamp will also be used when creating new packages of this type, so
            // we are effectively controlling the versioning for the user
            packageVersion = Long.toString(System.currentTimeMillis());

            try {
                ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();

                // RHQ-666 - Changed to not request the resource name from the user; simply pass null
                resourceFactoryManager.createResource(user, parentResource.getId(), getResourceTypeId(), null,
                    pluginConfiguration, packageName, packageVersion, selectedArchitectureId, deployTimeConfiguration,
                    packageContentStream);
            } catch (Exception e) {
                String errorMessages = ThrowableUtil.getAllMessages(e);
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "Failed to send create resource request to agent. Cause: " + errorMessages);
                return OUTCOME_SUCCESS_OR_FAILURE;
            }

            // If we got this far, there were no errors, so output a success message
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                "Create resource request successfully sent to the agent.");
        } finally {
            // clean up the temp file
            cleanup(uploadUIBean);
        }

        return OUTCOME_SUCCESS_OR_FAILURE;
    }

    private void cleanup(UploadNewChildPackageUIBean uploadUIBean) {
        if (uploadUIBean != null) {
            uploadUIBean.clear();
        }
        // clean out fields for next usage
        this.configuration = null;
        this.configurationDefinition = null;
        this.packageType = null;
        this.resourceType = null;
    }

    public String cancel() {
        UploadNewChildPackageUIBean uploadUIBean;
        uploadUIBean = FacesContextUtility.getManagedBean(UploadNewChildPackageUIBean.class);
        cleanup(uploadUIBean);
        return OUTCOME_CANCEL;
    }

    public SelectItem[] getArchitectures() {
        ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
        List<Architecture> architectures = contentUIManager.getArchitectures();

        SelectItem[] items = new SelectItem[architectures.size()];
        int itemCounter = 0;
        for (Architecture arch : architectures) {
            SelectItem item = new SelectItem(arch.getId(), arch.getName());
            items[itemCounter++] = item;
        }

        return items;
    }

    private ResourceType lookupResourceType() {
        ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        ResourceType resourceType;
        try {
            resourceType = resourceTypeManager.getResourceTypeById(subject, getResourceTypeId());
        } catch (ResourceTypeNotFoundException e) {
            throw new IllegalStateException(e); // generally should never happen
        }

        return resourceType;
    }

    private PackageType lookupPackageType() {
        if (resourceType == null)
            resourceType = lookupResourceType();
        ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
        PackageType packageType = contentUIManager.getResourceCreationPackageType(this.resourceType.getId());
        return packageType;
    }

    protected ConfigurationDefinition lookupConfigurationDefinition() {
        if (packageType == null)
            packageType = lookupPackageType();
        ConfigurationDefinition configurationDefinition = this.packageType.getDeploymentConfigurationDefinition();
        return configurationDefinition;
    }

    protected Configuration lookupConfiguration() {
        ConfigurationTemplate deployTimeConfigurationTemplates = getConfigurationDefinition().getDefaultTemplate();
        Configuration deployTimeConfiguration = (deployTimeConfigurationTemplates != null) ? deployTimeConfigurationTemplates
            .createConfiguration()
            : new Configuration();
        if (deployTimeConfiguration != null) {
            ConfigurationMaskingUtility.maskConfiguration(deployTimeConfiguration, getConfigurationDefinition());
        }

        return deployTimeConfiguration;
    }

    // Accessors  --------------------------------------------

    /**
     * Temporary hack to get around the bug in the richfaces data table that breaks links in each row. Ultimately, each
     * failed row will have a button to retry the create. Until that bug is fixed, use a separate dropdown with each
     * failed create to select the one to retry.
     *
     * @return list of failed create history requests
     */
    public SelectItem[] getFailedCreateHistory() {
        Resource parentResource = EnterpriseFacesContextUtility.getResource();

        ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();
        PageControl pageControl = new PageControl(0, 1000);
        PageList<CreateResourceHistory> pageList = resourceFactoryManager.getCreateChildResourceHistory(parentResource
            .getId(), null, null, pageControl);

        List<SelectItem> selectItemsList = new ArrayList<SelectItem>();
        for (CreateResourceHistory history : pageList) {
            if ((history.getStatus() == CreateResourceStatus.FAILURE)
                || (history.getStatus() == CreateResourceStatus.TIMED_OUT)) {
                selectItemsList.add(new SelectItem(history, history.getCreatedResourceName()));
            }
        }

        SelectItem[] items = selectItemsList.toArray(new SelectItem[selectItemsList.size()]);
        return items;
    }

    public CreateResourceHistory getRetryCreateItem() {
        return retryCreateItem;
    }

    public void setRetryCreateItem(CreateResourceHistory retryCreateItem) {
        this.retryCreateItem = retryCreateItem;
    }

    public PackageType getPackageType() {
        if (this.packageType == null)
            this.packageType = lookupPackageType();
        return packageType;
    }

    public void setPackageType(PackageType packageType) {
        this.packageType = packageType;
    }

    public ResourceType getResourceType() {
        if (this.resourceType == null)
            this.resourceType = lookupResourceType();
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getPackageVersion() {
        return packageVersion;
    }

    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
    }

    public int getSelectedArchitectureId() {
        return selectedArchitectureId;
    }

    public void setSelectedArchitectureId(int selectedArchitectureId) {
        this.selectedArchitectureId = selectedArchitectureId;
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        if (this.configurationDefinition == null)
            this.configurationDefinition = lookupConfigurationDefinition();
        return configurationDefinition;
    }

    public Configuration getConfiguration() {
        if (this.configuration == null)
            this.configuration = lookupConfiguration();
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public String getNullConfigurationDefinitionMessage() {
        return "This resource type does not expose deployment time configuration values.";
    }

    public String getNullConfigurationMessage() {
        return "Unable to create an initial deployment time configuration for resource being added.";
    }

    private int getResourceTypeId() {
        try {
            return EnterpriseFacesContextUtility.getResourceType().getId();
        } catch (Exception e) {
            return (Integer) FacesContextUtility.getFacesContext().getExternalContext().getRequestMap().get(
                ParamConstants.RESOURCE_TYPE_ID_PARAM);
        }
    }
}