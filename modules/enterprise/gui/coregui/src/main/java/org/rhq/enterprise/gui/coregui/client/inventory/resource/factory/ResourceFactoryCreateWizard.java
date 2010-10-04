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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.factory;

import java.util.ArrayList;
import java.util.EnumSet;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizard;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardView;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class ResourceFactoryCreateWizard extends AbstractWizard {

    private ConfigurationDefinition configurationDefinition;
    private Resource parentResource;
    private ResourceType createType;
    private boolean isImport; // manual add vs. create new child

    private WizardView view;

    private ConfigurationTemplateStep configurationTemplateStep;
    private ConfigurationStep configurationStep;

    public ResourceFactoryCreateWizard(Resource parentResource, ResourceType createType,
        ConfigurationDefinition configurationDefinition, boolean isImport) {
        this.parentResource = parentResource;
        this.createType = createType;
        this.configurationDefinition = configurationDefinition;
        this.isImport = isImport;

        assert parentResource != null;
        assert createType != null;
        assert configurationDefinition != null;

        ArrayList<WizardStep> steps = new ArrayList<WizardStep>();

        // skip the template step if this is an import (no resource name to be gathered) and the
        // type has only the default template to offer for user selection. 
        if (!(this.isImport && this.configurationDefinition.getTemplates().size() < 2)) {
            this.configurationTemplateStep = new ConfigurationTemplateStep(this);
            steps.add(configurationTemplateStep);
        }

        configurationStep = new ConfigurationStep(this);
        steps.add(configurationStep);

        setSteps(steps);
    }

    public String getWindowTitle() {
        return isImport() ? "Import Resource Wizard" : "Resource Create Wizard";
    }

    public String getTitle() {
        return (isImport() ? "Import Resource of Type: " : "Create New Resource of Type: ") + createType.getName();
    }

    public String getSubtitle() {
        return null;
    }

    public void execute() {

        int parentResourceId = parentResource.getId();
        int createTypeId = createType.getId();
        Configuration newConfiguration = configurationStep.getConfiguration();

        if (isImport) {
            GWTServiceLookup.getResourceService().manuallyAddResource(createTypeId, parentResourceId, newConfiguration,
                new AsyncCallback<Resource>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to manually add resource", caught);
                        view.closeDialog();
                    }

                    public void onSuccess(Resource result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message("Submitted request to manually add [" + createType.getName() + "]",
                                Message.Severity.Info));
                        view.closeDialog();
                    }
                });

        } else {
            String newResourceName = configurationTemplateStep.getResourceName();

            GWTServiceLookup.getResourceService().createResource(parentResourceId, createTypeId, newResourceName,
                newConfiguration, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to create new resource", caught);
                        view.closeDialog();
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message("Submitted request to create new resource ["
                                + configurationTemplateStep.getResourceName() + "]", Message.Severity.Info));
                        view.closeDialog();
                    }
                });
        }
    }

    public void display() {
        view = new WizardView(this);
        view.displayDialog();
    }

    public static void showCreateWizard(final Resource parentResource, ResourceType childType) {
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(childType.getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(ResourceType type) {
                    ResourceFactoryCreateWizard wizard = new ResourceFactoryCreateWizard(parentResource, type, type
                        .getResourceConfigurationDefinition(), false);
                    wizard.display();
                }
            });
    }

    public static void showImportWizard(final Resource parentResource, ResourceType childType) {
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(childType.getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.pluginConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(ResourceType type) {
                    ResourceFactoryCreateWizard wizard = new ResourceFactoryCreateWizard(parentResource, type, type
                        .getPluginConfigurationDefinition(), true);
                    wizard.display();
                }
            });
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        return configurationDefinition;
    }

    public Configuration getConfiguration() {
        return (null == configurationTemplateStep) ? this.configurationDefinition.getDefaultTemplate()
            .createConfiguration() : configurationTemplateStep.getConfiguration();
    }

    public Resource getParentResource() {
        return parentResource;
    }

    public ResourceType getCreateType() {
        return createType;
    }

    public boolean isImport() {
        return isImport;
    }

    public void cancel() {
        // TODO: revert back to original state
    }
}
