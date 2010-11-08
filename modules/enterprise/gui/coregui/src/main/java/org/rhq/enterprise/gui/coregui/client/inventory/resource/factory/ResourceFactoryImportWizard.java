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
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
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
public class ResourceFactoryImportWizard extends AbstractResourceFactoryWizard {

    private ResourceFactoryInfoStep infoStep;
    private ResourceFactoryConfigurationStep configurationStep;

    public ResourceFactoryImportWizard(Resource parentResource, ResourceType childType) {

        super(parentResource, childType);
        this.setNewResourceConfigurationDefinition(getChildType().getPluginConfigurationDefinition());

        ArrayList<WizardStep> steps = new ArrayList<WizardStep>();

        // skip the info step if the type has only the default template to offer for user selection.
        Map<String, ConfigurationTemplate> templates = childType.getPluginConfigurationDefinition().getTemplates();
        if (templates.size() > 1) {
            this.infoStep = new ResourceFactoryInfoStep(this, null, "Connection Settings Templates (Choose One):",
                templates);
            steps.add(infoStep);
        }

        configurationStep = new ResourceFactoryConfigurationStep(this);
        steps.add(configurationStep);

        setSteps(steps);

    }

    public String getWindowTitle() {
        return "Resource Import Wizard";
    }

    public String getTitle() {
        return "Import Resource of Type: " + getChildType().getName();
    }

    public void execute() {

        int parentResourceId = getParentResource().getId();
        int createTypeId = getChildType().getId();
        Configuration newConfiguration = this.getNewResourceConfiguration();

        GWTServiceLookup.getResourceService().manuallyAddResource(createTypeId, parentResourceId, newConfiguration,
            new AsyncCallback<Resource>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to manually import resource", caught);
                    getView().closeDialog();
                }

                public void onSuccess(Resource result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message("Submitted request to manually import [" + getChildType().getName() + "]",
                            Message.Severity.Info));
                    getView().closeDialog();
                }
            });
    }

    public static void showImportWizard(final Resource parentResource, ResourceType childType) {
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(childType.getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.pluginConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(ResourceType type) {
                    ResourceFactoryImportWizard wizard = new ResourceFactoryImportWizard(parentResource, type);
                    wizard.startWizard();
                }
            });
    }

}
