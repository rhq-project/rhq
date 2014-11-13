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

package org.rhq.coregui.client.inventory.resource.factory;

import static org.rhq.coregui.client.CoreGUI.getErrorHandler;
import static org.rhq.coregui.client.CoreGUI.getMessageCenter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;

import com.google.gwt.core.client.Duration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.resource.CannotConnectToAgentException;
import org.rhq.core.domain.resource.ImportResourceRequest;
import org.rhq.core.domain.resource.ImportResourceResponse;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.coregui.client.components.wizard.WizardStep;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * @author Jay Shaughnessy 
 * @author Greg Hinkle
 */
public class ResourceFactoryImportWizard extends AbstractResourceFactoryWizard {

    public ResourceFactoryImportWizard(Resource parentResource, ResourceType childType) {

        super(parentResource, childType);

        ConfigurationDefinition childTypePluginConfigDef = getChildType().getPluginConfigurationDefinition();

        this.setNewResourceConfigurationDefinition(childTypePluginConfigDef);

        ArrayList<WizardStep> steps = new ArrayList<WizardStep>();

        // Skip the choose-template step if the type does not define a plugin config or does not define more than one
        // plugin config template.
        if (childTypePluginConfigDef != null) {
            Map<String, ConfigurationTemplate> templates = childTypePluginConfigDef.getTemplates();
            if (templates.size() > 1) {
                ResourceFactoryInfoStep infoStep = new ResourceFactoryInfoStep(this, null,
                    MSG.widget_resourceFactoryWizard_templatePrompt(), templates);
                steps.add(infoStep);
            }
        }

        ResourceFactoryConfigurationStep configurationStep = new ResourceFactoryConfigurationStep(this);
        steps.add(configurationStep);

        setSteps(steps);

    }

    @Override
    public String getWindowTitle() {
        return MSG.widget_resourceFactoryWizard_importWizardWindowTitle();
    }

    @Override
    public String getTitle() {
        return MSG.widget_resourceFactoryWizard_importWizardTitle(ResourceTypeUtility.displayName(getChildType()));
    }

    @Override
    public void execute() {
        getView().closeDialog();
        getMessageCenter().notify(
            new Message(MSG.widget_resourceFactoryWizard_importSubmitted(getChildType().getName()), Severity.Info));

        ImportResourceRequest request = new ImportResourceRequest(getChildType().getId(), getParentResource().getId(),
            getNewResourceConfiguration());

        ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService(300000);
        final Duration duration = new Duration();
        resourceService.manuallyAddResource(request, new AsyncCallback<ImportResourceResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                Timer timer = new Timer() {
                    @Override
                    public void run() {
                        if (caught instanceof CannotConnectToAgentException) {
                            getMessageCenter().notify(
                                new Message(MSG.widget_resourceFactoryWizard_importFailure2(), Severity.Warning));
                        } else {
                            getErrorHandler().handleError(MSG.widget_resourceFactoryWizard_importFailure(), caught);
                        }
                    }
                };
                // Delay the showing of the result to give the user some time to see the importSubmitted notif
                timer.schedule(Math.max(0, 3 * 1000 - duration.elapsedMillis()));
            }

            @Override
            public void onSuccess(final ImportResourceResponse result) {
                Timer timer = new Timer() {
                    @Override
                    public void run() {
                        boolean resourceAlreadyExisted = result.isResourceAlreadyExisted();
                        Resource resource = result.getResource();
                        String resourceTypeName = ResourceTypeUtility.displayName(resource.getResourceType());
                        String resourceKey = resource.getResourceKey();

                        String conciseMessage;
                        String detailedMessage;
                        Severity severity;
                        if (!resourceAlreadyExisted) {
                            conciseMessage = MSG.widget_resourceFactoryWizard_importSuccess(resourceTypeName,
                                resourceKey);
                            detailedMessage = null;
                            severity = Severity.Info;
                        } else {

                            severity = Severity.Warning;
                            conciseMessage = MSG.widget_resourceFactoryWizard_importFailureResource(resourceTypeName,
                                resourceKey);

                            switch (resource.getInventoryStatus()) {
                            case NEW:
                                detailedMessage = MSG
                                    .widget_resourceFactoryWizard_importFailureResourceInDiscoveryQueue();
                                break;
                            case IGNORED:
                                detailedMessage = MSG.widget_resourceFactoryWizard_importFailureResourceIgnored();
                                break;
                            case COMMITTED:
                                detailedMessage = MSG
                                    .widget_resourceFactoryWizard_importFailureResourceAlreadyInInventory();
                                break;
                            default:
                                detailedMessage = MSG
                                    .widget_resourceFactoryWizard_importFailureResourceWaitingForPurge();
                            }
                        }

                        getMessageCenter().notify(new Message(conciseMessage, detailedMessage, severity));
                    }
                };
                // Delay the showing of the result to give the user some time to see the importSubmitted notif
                timer.schedule(Math.max(0, 3 * 1000 - duration.elapsedMillis()));
            }
        });
    }

    public static void showImportWizard(final Resource parentResource, ResourceType childType) {
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(childType.getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.pluginConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {
                @Override
                public void onTypesLoaded(ResourceType type) {
                    ResourceFactoryImportWizard wizard = new ResourceFactoryImportWizard(parentResource, type);
                    wizard.startWizard();
                }
            });
    }
}
