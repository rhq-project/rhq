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
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Jay Shaughnessy
 * @author Greg Hinkle
 */
public class ResourceFactoryCreateWizard extends AbstractResourceFactoryWizard {

    private PackageType newResourcePackageType;
    private int newResourcePackageVersionId;

    public ResourceFactoryCreateWizard(Resource parentResource, ResourceType childType, PackageType packageType) {

        super(parentResource, childType);
        this.newResourcePackageType = packageType;

        final ArrayList<WizardStep> steps = new ArrayList<WizardStep>();

        switch (childType.getCreationDataType()) {

        case CONTENT: {
            String archPrompt = packageType.isSupportsArchitecture() ? "Package Architecture" : null;

            ConfigurationDefinition deployTimeConfigDef = packageType.getDeploymentConfigurationDefinition();
            this.setNewResourceConfigurationDefinition(deployTimeConfigDef);
            Map<String, ConfigurationTemplate> templates = deployTimeConfigDef.getTemplates();

            steps.add(new ResourceFactoryInfoStep(ResourceFactoryCreateWizard.this, null, "Package Version",
                archPrompt, "Deployment Time Configuration Templates (Choose One):", templates));

            steps.add(new ResourceFactoryPackageStep(ResourceFactoryCreateWizard.this));

            steps.add(new ResourceFactoryConfigurationStep(ResourceFactoryCreateWizard.this));

            setSteps(steps);

            break;
        }

        case CONFIGURATION: {

            ConfigurationDefinition resourceConfigDef = getChildType().getResourceConfigurationDefinition();
            this.setNewResourceConfigurationDefinition(resourceConfigDef);
            Map<String, ConfigurationTemplate> templates = resourceConfigDef.getTemplates();
            steps.add(new ResourceFactoryInfoStep(ResourceFactoryCreateWizard.this, "New Resource Name",
                "Resource Configuration Templates (Choose One):", templates));

            steps.add(new ResourceFactoryConfigurationStep(ResourceFactoryCreateWizard.this));

            setSteps(steps);

            break;
        }

        }
    }

    public String getWindowTitle() {
        return "Resource Create Wizard";
    }

    public String getTitle() {
        return "Create New Resource of Type: " + getChildType().getName();
    }

    public String getSubtitle() {
        return null;
    }

    public void execute() {

        int parentResourceId = getParentResource().getId();
        int createTypeId = getChildType().getId();

        switch (getChildType().getCreationDataType()) {

        case CONTENT: {
            Configuration deployTimeConfiguration = this.getNewResourceConfiguration();
            int packageVersionId = this.getNewResourcePackageVersionId();

            GWTServiceLookup.getResourceService().createResource(parentResourceId, createTypeId, (String) null,
                deployTimeConfiguration, packageVersionId, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to create new resource", caught);
                        getView().closeDialog();
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message("Submitted request to create new resource of type [" + getChildType().getName()
                                + "]", Message.Severity.Info));
                        getView().closeDialog();
                    }
                });

            break;
        }

        case CONFIGURATION: {
            final String newResourceName = this.getNewResourceName();
            Configuration resourceConfiguration = this.getNewResourceConfiguration();

            GWTServiceLookup.getResourceService().createResource(parentResourceId, createTypeId, newResourceName,
                resourceConfiguration, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to create new resource", caught);
                        getView().closeDialog();
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message("Submitted request to create new resource [" + newResourceName + "]",
                                Message.Severity.Info));
                        getView().closeDialog();
                    }
                });

            break;
        }
        }
    }

    public PackageType getNewResourcePackageType() {
        return newResourcePackageType;
    }

    public void setNewResourcePackageType(PackageType newResourcePackageType) {
        this.newResourcePackageType = newResourcePackageType;
    }

    public int getNewResourcePackageVersionId() {
        return newResourcePackageVersionId;
    }

    public void setNewResourcePackageVersionId(int newResourcePackageVersionId) {
        this.newResourcePackageVersionId = newResourcePackageVersionId;
    }

    public static void showCreateWizard(final Resource parentResource, ResourceType childType) {
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(childType.getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(final ResourceType loadedChildType) {

                    switch (loadedChildType.getCreationDataType()) {

                    case CONTENT: {

                        // get PackageType info before continuing
                        GWTServiceLookup.getContentService().getResourceCreationPackageType(loadedChildType.getId(),
                            new AsyncCallback<PackageType>() {
                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError(
                                        "Failed to get backing package type for new resource", caught);
                                }

                                public void onSuccess(PackageType result) {
                                    ResourceFactoryCreateWizard wizard = new ResourceFactoryCreateWizard(
                                        parentResource, loadedChildType, result);
                                    wizard.startWizard();
                                }
                            });

                        break;
                    }

                    case CONFIGURATION: {
                        ResourceFactoryCreateWizard wizard = new ResourceFactoryCreateWizard(parentResource,
                            loadedChildType, null);
                        wizard.startWizard();
                        break;
                    }
                    }
                }
            });
    }
}
