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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import org.apache.commons.fileupload.FileItem;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
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
    // Constants  --------------------------------------------

    public static final String MANAGED_BEAN_NAME = "CreateNewPackageChildResourceUIBean";

    private static final String OUTCOME_SUCCESS_OR_FAILURE = "successOrFailure";
    private static final String OUTCOME_SUCCESS = "success";

    // Attributes  --------------------------------------------

    private ResourceType resourceType;
    private PackageType packageType;

    private String resourceName;
    private String packageName;
    private String packageVersion;
    private String architecture;

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

        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
        ConfigurationDefinition pluginConfigurationDefinition = configurationManager
            .getPluginConfigurationDefinitionForResourceType(user, resourceType.getId());

        Configuration pluginConfiguration = null;
        if (pluginConfigurationDefinition != null) {
            pluginConfiguration = pluginConfigurationDefinition.getDefaultTemplate().getConfiguration();
        }

        InputStream packageContentStream;
        try {
            FileItem fileItem = (FileItem) FacesContextUtility.getRequest().getAttribute("uploadForm:uploadFile");
            packageContentStream = fileItem.getInputStream();
        } catch (IOException e) {
            String errorMessages = ThrowableUtil.getAllMessages(e);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to retrieve the input stream. Cause: "
                + errorMessages);
            return OUTCOME_SUCCESS_OR_FAILURE;
        }

        // Collect data for create call
        Resource parentResource = EnterpriseFacesContextUtility.getResource();
        Configuration deployTimeConfiguration = getConfiguration();
        ConfigurationMaskingUtility.unmaskConfiguration(deployTimeConfiguration, getConfigurationDefinition());
        int architectureId = Integer.parseInt(architecture);

        try {
            ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();
            resourceFactoryManager.createResource(user, parentResource.getId(), getResourceTypeId(), getResourceName(),
                pluginConfiguration, packageName, packageVersion, architectureId, deployTimeConfiguration,
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
        return OUTCOME_SUCCESS_OR_FAILURE;
    }

    public String cancel() {
        return OUTCOME_SUCCESS;
    }

    // Other Methods  --------------------------------------------

    private ResourceType lookupResourceType() {
        ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        ResourceType resourceType = null;
        try {
            resourceType = resourceTypeManager.getResourceTypeById(subject, getResourceTypeId());
        } catch (ResourceTypeNotFoundException e) {
            throw new IllegalStateException(e); // generally should never happen
        }

        return resourceType;
    }

    private PackageType lookupPackageType() {
        ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
        PackageType packageType = contentUIManager.getResourceCreationPackageType(this.resourceType.getId());
        return packageType;
    }

    protected ConfigurationDefinition lookupConfigurationDefinition() {
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
            .getId(), pageControl);

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
        return packageType;
    }

    public void setPackageType(PackageType packageType) {
        this.packageType = packageType;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceName() {
        return this.resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageVersion() {
        return packageVersion;
    }

    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        return configurationDefinition;
    }

    public Configuration getConfiguration() {
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