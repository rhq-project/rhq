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
package org.rhq.coregui.client.inventory.resource.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.resource.CannotConnectToAgentException;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.wizard.WizardStep;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Jay Shaughnessy
 * @author Greg Hinkle
 */
public class ResourceFactoryCreateWizard extends AbstractResourceFactoryWizard {

    private PackageType newResourcePackageType;
    private Integer newResourcePackageVersionId;

    public ResourceFactoryCreateWizard(Resource parentResource, ResourceType childType, PackageType packageType) {

        super(parentResource, childType);
        this.newResourcePackageType = packageType;

        final ArrayList<WizardStep> steps = new ArrayList<WizardStep>();

        switch (childType.getCreationDataType()) {

        case CONTENT: {
            String archPrompt = packageType.isSupportsArchitecture() ? MSG.widget_resourceFactoryWizard_archPrompt()
                : null;

            ConfigurationDefinition deployTimeConfigDef = packageType.getDeploymentConfigurationDefinition();
            this.setNewResourceConfigurationDefinition(deployTimeConfigDef);
            Map<String, ConfigurationTemplate> templates = Collections.emptyMap();
            if (deployTimeConfigDef != null) {
                templates = deployTimeConfigDef.getTemplates();
            }

            steps.add(new ResourceFactoryInfoStep(ResourceFactoryCreateWizard.this, null, MSG
                .widget_resourceFactoryWizard_versionPrompt(), archPrompt, MSG
                .widget_resourceFactoryWizard_contentTemplatePrompt(), templates));

            steps.add(new ResourceFactoryPackageStep(ResourceFactoryCreateWizard.this));

            steps.add(new ResourceFactoryConfigurationStep(ResourceFactoryCreateWizard.this));

            setSteps(steps);

            break;
        }

        case CONFIGURATION: {

            ConfigurationDefinition resourceConfigDef = getChildType().getResourceConfigurationDefinition();
            this.setNewResourceConfigurationDefinition(resourceConfigDef);

            // Even if the type doesn't define multiple resource config templates, we still need to include the info
            // step, since it also prompts for the Resource name.
            Map<String, ConfigurationTemplate> templates = (resourceConfigDef != null) ?
                    resourceConfigDef.getTemplates() : Collections.<String, ConfigurationTemplate>emptyMap();
            steps.add(new ResourceFactoryInfoStep(ResourceFactoryCreateWizard.this, MSG
                .widget_resourceFactoryWizard_namePrompt(),
                MSG.widget_resourceFactoryWizard_configTemplatePrompt(), templates));

            steps.add(new ResourceFactoryConfigurationStep(ResourceFactoryCreateWizard.this));

            setSteps(steps);

            break;
        }

        }
    }

    public String getWindowTitle() {
        return MSG.widget_resourceFactoryWizard_createWizardWindowTitle();
    }

    public String getTitle() {
        return MSG.widget_resourceFactoryWizard_createWizardTitle(ResourceTypeUtility.displayName(getChildType()));
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
            Integer packageVersionId = this.getNewResourcePackageVersionId();

            if (null == packageVersionId) {
                CoreGUI.getErrorHandler().handleError(MSG.widget_resourceFactoryWizard_execute1());
                getView().closeDialog();
            }

            GWTServiceLookup.getResourceService().createResource(parentResourceId, createTypeId, (String) null,
                deployTimeConfiguration, packageVersionId, this.getNewResourceCreateTimeout(),
                new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        if (caught instanceof CannotConnectToAgentException) {
                            CoreGUI.getMessageCenter().notify(new Message(MSG.widget_resourceFactoryWizard_execute2(),
                                Message.Severity.Warning));
                        } else {
                            CoreGUI.getErrorHandler().handleError(MSG.widget_resourceFactoryWizard_execute3(), caught);
                        }

                        getView().closeDialog();
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.widget_resourceFactoryWizard_createSubmitType(ResourceTypeUtility
                                .displayName(getChildType())),
                                Message.Severity.Info));
                        getView().closeDialog();
                    }
                });

            break;
        }

        case CONFIGURATION: {
            final String newResourceName = this.getNewResourceName();
            Configuration resourceConfiguration = this.getNewResourceConfiguration();

            GWTServiceLookup.getResourceService().createResource(parentResourceId, createTypeId, newResourceName,
                resourceConfiguration, this.getNewResourceCreateTimeout(), new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.widget_resourceFactoryWizard_execute2(), caught);
                        getView().closeDialog();
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.widget_resourceFactoryWizard_createSubmit(newResourceName),
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

    public Integer getNewResourcePackageVersionId() {
        return newResourcePackageVersionId;
    }

    public void setNewResourcePackageVersionId(Integer newResourcePackageVersionId) {
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
                                        MSG.widget_resourceFactoryWizard_failedToGetType(), caught);
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

    @Override
    public void cancel() {
        super.cancel();

        if (null != this.newResourcePackageVersionId) {

            GWTServiceLookup.getContentService().deletePackageVersion(this.newResourcePackageVersionId,
                new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.widget_resourceFactoryWizard_failedToDeleteVersion(),
                            caught);
                    }

                    public void onSuccess(Void ignore) {
                        // succeed silently
                    }
                });
        }
    }

}
