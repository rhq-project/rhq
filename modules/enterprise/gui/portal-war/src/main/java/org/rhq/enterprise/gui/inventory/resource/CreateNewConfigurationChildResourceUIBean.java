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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIInput;
import javax.faces.model.SelectItem;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.gui.configuration.ConfigurationMaskingUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.configuration.resource.AbstractResourceConfigurationUIBean;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Handles the workflow for creating a new configuration-backed resource.
 *
 * @author Ian Springer
 * @author Jason Dobies
 */
public class CreateNewConfigurationChildResourceUIBean extends AbstractResourceConfigurationUIBean {
    // Constants  --------------------------------------------

    public static final String MANAGED_BEAN_NAME = "CreateNewConfigurationChildResourceUIBean";

    private static final String OUTCOME_SUCCESS_OR_FAILURE = "successOrFailure";

    // Attributes  --------------------------------------------

    private String selectedTemplateName;
    private Map<Integer, ResourceType> resourceTypeMap; // maps ResourceType ids to ResourceTypes
    private Map<Integer, String> resourceNameMap; // maps ResourceType ids to Resource names

    /**
     * Direct access to the form input, used to grab the local value of the resource type.
     */
    private UIInput resourceTypeInput;

    /**
     * Direct access to the form input, used to grab the value of the selected template for use in the configuration
     * loading before the
     */
    private UIInput selectedTemplateNameInput;

    public CreateNewConfigurationChildResourceUIBean() {
        this.resourceTypeMap = new Hashtable<Integer, ResourceType>();
        this.resourceNameMap = new Hashtable<Integer, String>();
    }

    // Actions  --------------------------------------------

    /**
     * Performs the creation of a configuration backed resource.
     *
     * @return outcome of the creation
     */
    public String createResource() {
        Subject user = EnterpriseFacesContextUtility.getSubject();
        Configuration pluginConfiguration = createPluginConfiguration(user);

        // Collect data for create call
        Resource parentResource = EnterpriseFacesContextUtility.getResource();
        Configuration resourceConfiguration = getConfiguration();
        ConfigurationMaskingUtility.unmaskConfiguration(resourceConfiguration, getConfigurationDefinition());

        try {
            ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();
            resourceFactoryManager.createResource(user, parentResource.getId(), getResourceTypeId(), getResourceName(),
                pluginConfiguration, resourceConfiguration);
        } catch (Exception e) {
            String errorMessages = ThrowableUtil.getAllMessages(e);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Failed to send create resource request to agent. Cause: " + errorMessages);
            clear();
            return OUTCOME_SUCCESS_OR_FAILURE;
        }

        // If we got this far, there were no errors, so output a success message
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
            "Create resource request successfully sent to the agent");
        clear();
        return OUTCOME_SUCCESS_OR_FAILURE;
    }

    public String selectTemplate() {
        return SUCCESS_OUTCOME;
    }

    public String cancel() {
        clear();
        return SUCCESS_OUTCOME;
    }

    public String finish() {
        return SUCCESS_OUTCOME;
    }

    // Other Methods  --------------------------------------------

    public List<SelectItem> getTemplateNames() {
        List<SelectItem> selectItems = new ArrayList<SelectItem>();
        for (ConfigurationTemplate template : getConfigurationDefinition().getTemplates().values()) {
            StringBuilder label = new StringBuilder(template.getName());
            if ((template.getDescription() != null) && !template.getDescription().equals("")
                && !template.getDescription().equals(template.getName())) {
                label.append(" - ").append(template.getDescription());
            }

            SelectItem selectItem = new SelectItem(template.getName(), label.toString());
            selectItems.add(selectItem);
        }

        return selectItems;
    }

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

    @Override
    protected ConfigurationDefinition lookupConfigurationDefinition() {
        Subject user = EnterpriseFacesContextUtility.getSubject();
        ConfigurationDefinition configurationDefinition = this.configurationManager
            .getResourceConfigurationDefinitionWithTemplatesForResourceType(user, getResourceTypeId());
        return configurationDefinition;
    }

    @Override
    @NotNull
    protected Configuration lookupConfiguration() {
        ConfigurationTemplate resourceConfigTemplate;

        // If the input is not present, nullify the previous selected template name if it was set
        if (selectedTemplateNameInput != null) {
            selectedTemplateName = (String) selectedTemplateNameInput.getValue();
        } else {
            selectedTemplateName = null;
        }

        if ((this.selectedTemplateName != null) && !selectedTemplateName.equals("")) {
            resourceConfigTemplate = getConfigurationDefinition().getTemplate(this.selectedTemplateName);
            if (resourceConfigTemplate == null) {
                throw new IllegalStateException("A template named '" + this.selectedTemplateName
                    + "' does not exist for " + getResourceType() + " resource configurations.");
            }
        } else {
            resourceConfigTemplate = getConfigurationDefinition().getDefaultTemplate();
        }

        Configuration resourceConfig = (resourceConfigTemplate != null) ? resourceConfigTemplate.createConfiguration()
            : new Configuration();
        ConfigurationUtility.normalizeConfiguration(resourceConfig, getConfigurationDefinition());
        ConfigurationMaskingUtility.maskConfiguration(resourceConfig, getConfigurationDefinition());

        return resourceConfig;
    }

    private void clear() {
        clearConfiguration();
        this.resourceTypeMap.remove(getResourceTypeId());
        this.resourceNameMap.remove(getResourceTypeId());
    }

    // Accessors  --------------------------------------------

    public ResourceType getResourceType() {
        int resourceTypeId = getResourceTypeId();
        if (!this.resourceTypeMap.containsKey(resourceTypeId)) {
            this.resourceTypeMap.put(resourceTypeId, lookupResourceType());
        }

        return this.resourceTypeMap.get(resourceTypeId);
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceTypeMap.put(getResourceTypeId(), resourceType);
    }

    public UIInput getResourceTypeInput() {
        return resourceTypeInput;
    }

    public void setResourceTypeInput(UIInput resourceTypeInput) {
        this.resourceTypeInput = resourceTypeInput;
    }

    public UIInput getSelectedTemplateNameInput() {
        return selectedTemplateNameInput;
    }

    public void setSelectedTemplateNameInput(UIInput selectedTemplateNameInput) {
        this.selectedTemplateNameInput = selectedTemplateNameInput;
    }

    public String getResourceName() {
        return this.resourceNameMap.get(getResourceTypeId());
    }

    public void setResourceName(String resourceName) {
        this.resourceNameMap.put(getResourceTypeId(), resourceName);
    }

    public String getSelectedTemplateName() {
        if (this.selectedTemplateName == null) {
            ConfigurationTemplate defaultTemplate = getConfigurationDefinition().getDefaultTemplate();
            this.selectedTemplateName = (defaultTemplate != null) ? defaultTemplate.getName() : null;
        }

        return selectedTemplateName;
    }

    public void setSelectedTemplateName(String selectedTemplateName) {
        this.selectedTemplateName = selectedTemplateName;

        // JBNADM-2191 - I'm not sure why the configurations are cached. Doing to locks the user into the first
        // template chosen. I added the clear call here so whenever the template name is set, it will clear out
        // the configuration so prevent the inconsistency. I could have checked to see if it's a new template
        // before calling clear, but I'm unsure of why they are cached in the first place.
        super.clearConfiguration();
    }

    @Override
    public String getNullConfigurationDefinitionMessage() {
        return "This resource does not expose a configuration.";
    }

    @Override
    public String getNullConfigurationMessage() {
        return "Unable to create an initial configuration for resource being added.";
    }

    @Override
    protected int getConfigurationDefinitionKey() {
        return getResourceTypeId();
    }

    @Override
    protected int getConfigurationKey() {
        return getResourceTypeId();
    }

    private int getResourceTypeId() {
        try {
            return EnterpriseFacesContextUtility.getResourceType().getId();
        } catch (Exception e) {
            return (Integer) FacesContextUtility.getFacesContext().getExternalContext().getRequestMap().get(
                ParamConstants.RESOURCE_TYPE_ID_PARAM);
        }
    }

    private Configuration createPluginConfiguration(Subject user) {
        ConfigurationDefinition pluginConfigDefinition = this.configurationManager
            .getPluginConfigurationDefinitionForResourceType(user, getResourceTypeId());
        Configuration pluginConfig = null;
        if (pluginConfigDefinition != null) {
            ConfigurationTemplate pluginConfigTemplate = pluginConfigDefinition.getDefaultTemplate();
            pluginConfig = (pluginConfigTemplate != null) ? pluginConfigTemplate.createConfiguration()
                : new Configuration();
            ConfigurationUtility.normalizeConfiguration(pluginConfig, pluginConfigDefinition);
        }

        return pluginConfig;
    }
}