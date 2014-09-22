/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.gui.inventory.resource;

import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.discovery.InvalidPluginConfigurationClientException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.resource.ImportResourceRequest;
import org.rhq.core.domain.resource.ImportResourceResponse;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.discovery.DiscoveryBossLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Ian Springer
 */
public class ManuallyAddChildResourceUIBean {
    public static final String MANAGED_BEAN_NAME = "ManuallyAddChildResourceUIBean";

    private static final String OUTCOME_BAD_TYPE = "badType";
    private static final String OUTCOME_SUCCESS = "success";
    private static final String OUTCOME_FAILURE = "failure";
    private static final String OUTCOME_MULTIPLE_TEMPLATES = "multipleTemplates";
    private static final String OUTCOME_SINGLE_TEMPLATE = "singleTemplate";

    private ResourceType type;
    private String name;
    private String description;
    private ConfigurationDefinition configurationDefinition;
    private Configuration configuration;
    private String selectedTemplateName;

    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
    private ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
    private DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();

    public ManuallyAddChildResourceUIBean() {
        this.type = lookupResourceType();
        if (!this.type.isSupportsManualAdd()) {
            throw new IllegalStateException(this.type.getName() + " resources cannot be manually added to inventory.");
        }

        lookupConfigurationAndDef();
    }

    private void lookupConfigurationAndDef() {
        this.configurationDefinition = lookupConfigurationDefinition();
        if (this.configurationDefinition != null) {
            this.configuration = lookupConfiguration();
            ConfigurationTemplate defaultTemplate = this.configurationDefinition.getDefaultTemplate();
            this.selectedTemplateName = (defaultTemplate != null) ? defaultTemplate.getName() : null;
        }
    }

    @Nullable
    public ConfigurationDefinition getConfigurationDefinition() {
        return this.configurationDefinition;
    }

    public void setConfigurationDefinition(@NotNull
    ConfigurationDefinition configurationDefinition) {
        this.configurationDefinition = configurationDefinition;
    }

    @Nullable
    public Configuration getConfiguration() {
        return this.configuration;
    }

    public void setConfiguration(@NotNull
    Configuration configuration) {
        this.configuration = configuration;
    }

    public List<SelectItem> getTemplateNames() {
        List<SelectItem> selectItems = new ArrayList<SelectItem>();
        for (ConfigurationTemplate template : this.configurationDefinition.getTemplates().values()) {
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

    public String startWorkflow() {
        try {
            this.type = lookupResourceType();
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Could not retrieve resource type.", e
                .getLocalizedMessage());
            return OUTCOME_BAD_TYPE;
        }

        if (this.configurationDefinition == null) {
            lookupConfigurationAndDef();
        }

        if (this.configurationDefinition == null) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "There are no connection properties defined for " + this.type);
            return OUTCOME_BAD_TYPE;
        }

        return (this.configurationDefinition.getTemplates().size() > 1) ? OUTCOME_MULTIPLE_TEMPLATES
            : OUTCOME_SINGLE_TEMPLATE;
    }

    public String selectTemplate() {
        lookupConfiguration();
        return OUTCOME_SUCCESS;
    }

    public String addResource() {
        ImportResourceResponse response = null;
        ImportResourceRequest request = new ImportResourceRequest(getType().getId(), EnterpriseFacesContextUtility
            .getResource().getId(), getConfiguration());
        try {
            response = discoveryBoss.manuallyAddResource(EnterpriseFacesContextUtility.getSubject(), request);
        } catch (InvalidPluginConfigurationClientException e) {
            FacesContextUtility
                .addMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "The Agent was unable to connect to the "
                        + getType()
                        + " managed resource using the supplied connection properties. Please check that the connection properties are correct and that the managed resource on the agent machine is online. ",
                    e);
        } catch (PluginContainerException e) {
            FacesContextUtility
                .addMessage(FacesMessage.SEVERITY_FATAL, "An unexpected error occurred in the Agent.", e);
        } catch (RuntimeException e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_FATAL, "Unable to connect to the Agent.", e);
        }

        String outcome;
        if (response == null) {
            outcome = OUTCOME_FAILURE;
        } else {
            Resource resource = LookupUtil.getResourceManager().getResourceById(
                EnterpriseFacesContextUtility.getSubject(), response.getResource().getId());
            if (response.isResourceAlreadyExisted()) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_WARN, "A " + getType().getName()
                    + " with the specified connection properties was already in inventory.");
            } else {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "A " + getType().getName()
                    + " has been added to inventory with the name '" + resource.getName() + "'.");
            }

            // Change the ResourceUIBean in the request context to the added resource.
            FacesContextUtility.setBean(new ResourceUIBean(resource));
            outcome = OUTCOME_SUCCESS;
        }

        cleanup();
        return outcome;
    }

    public String cancel() {
        cleanup();
        return OUTCOME_SUCCESS;
    }

    @Nullable
    protected ConfigurationDefinition lookupConfigurationDefinition() {
        Integer resourceTypeId = FacesContextUtility.getRequiredRequestParameter(ParamConstants.RESOURCE_TYPE_ID_PARAM,
            Integer.class);
        ConfigurationDefinition pluginConfigDefinition = this.configurationManager
            .getPluginConfigurationDefinitionForResourceType(EnterpriseFacesContextUtility.getSubject(), resourceTypeId);
        return pluginConfigDefinition;
    }

    @NotNull
    protected Configuration lookupConfiguration() {
        ConfigurationTemplate pluginConfigTemplate;
        if (this.selectedTemplateName != null) {
            pluginConfigTemplate = getConfigurationDefinition().getTemplate(this.selectedTemplateName);
            if (pluginConfigTemplate == null) {
                throw new IllegalStateException("A template named '" + this.selectedTemplateName
                    + "' does not exist for " + getType() + " resource connection properties.");
            }
        } else {
            pluginConfigTemplate = getConfigurationDefinition().getDefaultTemplate();
        }

        configuration = (pluginConfigTemplate != null) ? pluginConfigTemplate.createConfiguration()
            : new Configuration();

        return configuration;
    }

    public ResourceType getType() {
        return this.type;
    }

    private ResourceType lookupResourceType() {
        Integer resourceTypeId = FacesContextUtility.getRequiredRequestParameter(ParamConstants.RESOURCE_TYPE_ID_PARAM,
            Integer.class);
        try {
            return this.resourceTypeManager.getResourceTypeById(EnterpriseFacesContextUtility.getSubject(),
                resourceTypeId);
        } catch (ResourceTypeNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private void cleanup() {
       // clean out fields for next usage
       this.configuration = null;
       this.configurationDefinition = null;
       this.type = null;
       this.selectedTemplateName = null;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNullConfigurationDefinitionMessage() {
        return "Could not find this resource's plugin configuration definition.";
    }

    public String getNullConfigurationMessage() {
        return "This resource's default plugin configuration is null - this should never happen.";
    }

    public String getSelectedTemplateName() {
        return selectedTemplateName;
    }

    public void setSelectedTemplateName(String selectedTemplateName) {
        this.selectedTemplateName = selectedTemplateName;
    }
}
