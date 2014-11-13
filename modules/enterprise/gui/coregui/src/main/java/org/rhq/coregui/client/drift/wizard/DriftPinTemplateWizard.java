/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.coregui.client.drift.wizard;

import java.util.ArrayList;
import java.util.EnumSet;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.criteria.DriftDefinitionCriteria;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.components.wizard.WizardStep;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Jay Shaughnessy
 */
public class DriftPinTemplateWizard extends AbstractDriftPinTemplateWizard {

    public DriftPinTemplateWizard(ResourceType resourceType, DriftDefinition driftDef, int snapshotVersion) {

        super(resourceType, driftDef, snapshotVersion);

        final ArrayList<WizardStep> steps = new ArrayList<WizardStep>();

        steps.add(new DriftPinTemplateWizardInfoStep(DriftPinTemplateWizard.this));
        steps.add(new DriftPinTemplateWizardConfigStep(DriftPinTemplateWizard.this));

        setSteps(steps);
    }

    @Override
    public String getWindowTitle() {
        return MSG.view_drift_wizard_pinTemplate_windowTitle();
    }

    @Override
    public String getTitle() {
        return MSG.view_drift_wizard_pinTemplate_title(String.valueOf(getSnapshotVersion()), getSnapshotDriftDef()
            .getName(), ResourceTypeUtility.displayName(getResourceType()));
    }

    @Override
    public String getSubtitle() {
        return null;
    }

    @Override
    public void execute() {
        if (isCreateTemplate()) {
            GWTServiceLookup.getDriftService().createTemplate(getResourceType().getId(), getNewDriftDefinition(),
                new AsyncCallback<DriftDefinitionTemplate>() {

                    public void onSuccess(DriftDefinitionTemplate result) {

                        pinTemplate(result);
                    }

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_drift_wizard_addDef_failure(getSelectedTemplate().getName()), caught);
                        getView().closeDialog();
                    }

                });

        } else {

            pinTemplate(getSelectedTemplate());
        }
    }

    private void pinTemplate(final DriftDefinitionTemplate template) {

        GWTServiceLookup.getDriftService().pinTemplate(template.getId(), getSnapshotDriftDef().getId(),
            getSnapshotVersion(), new AsyncCallback<Void>() {

                public void onSuccess(Void result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_drift_wizard_pinTemplate_success(template.getName()),
                            Message.Severity.Info));
                    getView().closeDialog();
                    CoreGUI.goToView(LinkManager.getDriftDefinitionsLink(getSnapshotDriftDef().getResource().getId()),
                        true);
                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_drift_wizard_pinTemplate_failure(template.getName()), caught);
                    getView().closeDialog();
                }
            });

    }

    public static void showWizard(final int snapshpotDriftDefId, final int snapshotVersion) {

        // get the relevant DriftDefinition
        DriftDefinitionCriteria ddc = new DriftDefinitionCriteria();
        ddc.addFilterId(snapshpotDriftDefId);
        ddc.fetchResource(true);
        ddc.fetchConfiguration(true);

        GWTServiceLookup.getDriftService().findDriftDefinitionsByCriteria(ddc,
            new AsyncCallback<PageList<DriftDefinition>>() {

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_drift_wizard_pinTemplate_failure("Invalid Snapshot"), caught);
                }

                @Override
                public void onSuccess(PageList<DriftDefinition> result) {
                    if (result.isEmpty()) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_drift_wizard_pinTemplate_failure("Invalid Snapshot"));
                    }

                    // get the relevant ResourceType, including the type's potential drift templates to pin 
                    final DriftDefinition driftDef = result.get(0);
                    final Resource resource = driftDef.getResource();

                    ResourceTypeRepository.Cache.getInstance().getResourceTypes(resource.getResourceType().getId(),
                        EnumSet.of(ResourceTypeRepository.MetadataType.driftDefinitionTemplates),
                        new ResourceTypeRepository.TypeLoadedCallback() {

                            public void onTypesLoaded(ResourceType type) {
                                DriftPinTemplateWizard wizard = new DriftPinTemplateWizard(type, driftDef,
                                    snapshotVersion);
                                wizard.startWizard();
                            }
                        });
                }
            });
    }

    @Override
    public void cancel() {
        super.cancel();
    }

}
