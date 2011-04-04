/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.persistence.NoResultException;

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
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Handles the workflow for creating a new package-backed Resource.
 *
 * The associated Facelets page is: /rhq/resource/inventory/create-package-1.xhtml
 *
 * @author Jason Dobies
 * @author Ian Springer
 */
public class CreateNewPackageChildResourceUIBean {
    // Constants  --------------------------------------------

    public static final String MANAGED_BEAN_NAME = "CreateNewPackageChildResourceUIBean";

    private static final String OUTCOME_SUCCESS_OR_FAILURE = "successOrFailure";
    // private static final String OUTCOME_SUCCESS = "success";
    private static final String OUTCOME_CANCEL = "cancel";

    private static final String DEFAULT_VERSION = "0";

    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(this.getClass());

    private ResourceType resourceType;
    private PackageType packageType;

    private String packageName;
    private String version = DEFAULT_VERSION;

    private Integer selectedArchitectureId;

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
     * Performs the creation of an package-backed Resource.
     *
     * @return outcome of the creation attempt
     */
    public String createResource() {
        Subject user = EnterpriseFacesContextUtility.getSubject();

        UploadNewChildPackageUIBean uploadUIBean;
        uploadUIBean = FacesContextUtility.getManagedBean(UploadNewChildPackageUIBean.class);
        UploadItem fileItem = uploadUIBean.getFileItem();

        //store information about uploaded file for packageDetails as most of it is already available
        Map<String, String> packageUploadDetails = new HashMap<String, String>();
        packageUploadDetails.put(ContentManagerLocal.UPLOAD_FILE_SIZE, String.valueOf(fileItem.getFileSize()));
        packageUploadDetails.put(ContentManagerLocal.UPLOAD_FILE_INSTALL_DATE, String.valueOf(System
            .currentTimeMillis()));
        packageUploadDetails.put(ContentManagerLocal.UPLOAD_OWNER, user.getName());
        packageUploadDetails.put(ContentManagerLocal.UPLOAD_FILE_NAME, fileItem.getFileName());

        try {//Easier to implement here than in server side bean. Shouldn't affect performance too much.
            packageUploadDetails.put(ContentManagerLocal.UPLOAD_MD5, new MessageDigestGenerator(
                MessageDigestGenerator.MD5).calcDigestString(fileItem.getFile()));
            packageUploadDetails.put(ContentManagerLocal.UPLOAD_SHA256, new MessageDigestGenerator(
                MessageDigestGenerator.SHA_256).calcDigestString(fileItem.getFile()));
        } catch (IOException e1) {
            log.warn("Error calculating file digest(s) : " + e1.getMessage());
            e1.printStackTrace();
        }

        // Validate
        if ((fileItem == null) || fileItem.getFile() == null) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "A package file must be uploaded");
            return null;
        }
        if ((getVersion() == null) || (getVersion().trim().length() == 0)) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "A package version must be specified.");
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

            if (isSupportsArchitecture()) {
                // pull in architecture selection
                selectedArchitectureId = getSelectedArchitectureId();
            }

            // Collect data for create call
            Resource parentResource = EnterpriseFacesContextUtility.getResource();
            Configuration deployTimeConfiguration = getConfiguration();
            ConfigurationMaskingUtility.unmaskConfiguration(deployTimeConfiguration, getConfigurationDefinition());
            String packageName = fileItem.getFileName();

            // some browsers (IE in particular) passes an absolute filename, we just want the name of the file, no paths
            if (packageName != null) {
                packageName = packageName.replace('\\', '/');
                if (packageName.length() > 2 && packageName.charAt(1) == ':') {
                    packageName = packageName.substring(2);
                }
                packageName = new File(packageName).getName();
            }

            try {
                ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();

                // RHQ-666 - Changed to not request the resource name from the user; simply pass null
                // JON 2.0 RC3 - use timestamp versioning; pass null for version
                //                resourceFactoryManager.createResource(user, parentResource.getId(), getResourceTypeId(), null,
                //                    pluginConfiguration, packageName, null, selectedArchitectureId, deployTimeConfiguration,
                //                    packageContentStream);
                if (packageUploadDetails != null) {
                    resourceFactoryManager.createResource(user, parentResource.getId(), getResourceTypeId(), null,
                        pluginConfiguration, packageName, getVersion(), selectedArchitectureId,
                        deployTimeConfiguration, packageContentStream, packageUploadDetails);
                } else {
                    resourceFactoryManager.createResource(user, parentResource.getId(), getResourceTypeId(), null,
                        pluginConfiguration, packageName, null, selectedArchitectureId, deployTimeConfiguration,
                        packageContentStream);

                }

            } catch (NoResultException nre) {
                //eat the exception.  Some of the queries return no results if no package yet exists which is fine.
            } catch (Exception e) {
                String errorMessages = ThrowableUtil.getAllMessages(e);
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "Failed to send create resource request to agent. Cause: " + errorMessages);
                log.error("Failed to create new child Resource of type [" + getResourceType() + "].", e);
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

    public boolean isSupportsArchitecture() {
        return packageType.isSupportsArchitecture();
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
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        ContentManagerLocal contentManager = LookupUtil.getContentManager();
        List<Architecture> architectures = contentManager.findArchitectures(subject);

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
        if (resourceType == null) {
            resourceType = lookupResourceType();
        }
        ContentManagerLocal contentManager = LookupUtil.getContentManager();
        PackageType packageType = contentManager.getResourceCreationPackageType(this.resourceType.getId());
        return packageType;
    }

    protected ConfigurationDefinition lookupConfigurationDefinition() {
        if (packageType == null) {
            packageType = lookupPackageType();
        }
        ConfigurationDefinition configurationDefinition = this.packageType.getDeploymentConfigurationDefinition();
        return configurationDefinition;
    }

    protected Configuration lookupConfiguration() {
        ConfigurationTemplate deployTimeConfigurationTemplates = null;
        ConfigurationDefinition configDef = getConfigurationDefinition();
        if (configDef != null) {
            deployTimeConfigurationTemplates = configDef.getDefaultTemplate();
        }
        Configuration deployTimeConfiguration = (deployTimeConfigurationTemplates != null) ? deployTimeConfigurationTemplates
            .createConfiguration()
            : new Configuration();
        if (deployTimeConfiguration != null) {
            ConfigurationMaskingUtility.maskConfiguration(deployTimeConfiguration, configDef);
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
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Resource parentResource = EnterpriseFacesContextUtility.getResource();

        ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();
        PageControl pageControl = new PageControl(0, 1000);
        PageList<CreateResourceHistory> pageList = resourceFactoryManager.findCreateChildResourceHistory(subject,
            parentResource.getId(), null, null, pageControl);

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
        return "This resource type does not expose deployment-time configuration values.";
    }

    public String getNullConfigurationMessage() {
        return "Unable to create an initial deployment-time configuration for resource being added.";
    }

    private int getResourceTypeId() {
        try {
            return EnterpriseFacesContextUtility.getResourceType().getId();
        } catch (Exception e) {
            return (Integer) FacesContextUtility.getFacesContext().getExternalContext().getRequestMap().get(
                ParamConstants.RESOURCE_TYPE_ID_PARAM);
        }
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public SelectItem[] getPackageTypes() {
        Resource resource = EnterpriseFacesContextUtility.getResource();

        ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
        List<PackageType> packageTypes = contentUIManager.getPackageTypes(resource.getResourceType().getId());

        SelectItem[] items = new SelectItem[packageTypes.size()];
        int itemCounter = 0;
        for (PackageType packageType : packageTypes) {
            SelectItem item = new SelectItem(packageType.getId(), packageType.getDisplayName());
            items[itemCounter++] = item;
        }

        return items;
    }
}