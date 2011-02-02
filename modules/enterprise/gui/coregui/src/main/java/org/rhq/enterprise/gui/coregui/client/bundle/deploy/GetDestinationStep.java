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
package org.rhq.enterprise.gui.coregui.client.bundle.deploy;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.validator.IsIntegerValidator;
import com.smartgwt.client.widgets.form.validator.Validator;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.bundle.deploy.selection.SinglePlatformResourceGroupSelector;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

public class GetDestinationStep extends AbstractWizardStep {

    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();
    private final BundleDeployWizard wizard;
    private VLayout form;
    DynamicForm valForm = new LocatableDynamicForm("GetDestinationStepValForm");
    private SinglePlatformResourceGroupSelector selector;
    private BundleDestination dest = new BundleDestination();
    private boolean createInProgress = false;

    public GetDestinationStep(BundleDeployWizard wizard) {
        this.wizard = wizard;
    }

    public String getName() {
        return MSG.view_bundle_deployWizard_getDestStep();
    }

    public Canvas getCanvas(Locatable parent) {
        if (this.form == null) {
            if (parent != null) {
                this.form = new LocatableVLayout(parent.extendLocatorId("BundleDeployGetDest"));
            } else {
                this.form = new LocatableVLayout("BundleDeployGetDest");
            }

            this.valForm.setWidth100();
            this.valForm.setNumCols(2);
            this.valForm.setColWidths("50%", "*");

            final TextItem nameTextItem = new TextItem("name", MSG.view_bundle_deployWizard_getDest_name());
            nameTextItem.setWidth(300);
            nameTextItem.setRequired(true);
            nameTextItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    Object value = event.getValue();
                    if (value == null) {
                        value = "";
                    }
                    wizard.setSubtitle(value.toString());
                    dest.setName(value.toString());
                }
            });

            final TextAreaItem descriptionTextAreaItem = new TextAreaItem("description", MSG
                .view_bundle_deployWizard_getDest_desc());
            descriptionTextAreaItem.setWidth(300);
            descriptionTextAreaItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    Object value = event.getValue();
                    if (value == null) {
                        value = "";
                    }
                    dest.setDescription(value.toString());
                }
            });

            final TextItem deployDirTextItem = new TextItem("deployDir", MSG
                .view_bundle_deployWizard_getDest_deployDir());
            deployDirTextItem.setWidth(300);
            deployDirTextItem.setRequired(true);
            deployDirTextItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    Object value = event.getValue();
                    if (value == null) {
                        value = "";
                    }
                    dest.setDeployDir(value.toString());
                }
            });

            this.selector = new SinglePlatformResourceGroupSelector("group", MSG.common_title_resource_group());
            this.selector.setWidth(300);
            this.selector.setRequired(true);
            Validator validator = new IsIntegerValidator();
            validator.setErrorMessage(MSG.view_bundle_deployWizard_error_8());
            this.selector.setValidators(validator);

            this.valForm.setItems(nameTextItem, descriptionTextAreaItem, deployDirTextItem, selector);
            CanvasItem ci1 = new CanvasItem();
            ci1.setShowTitle(false);
            ci1.setCanvas(valForm);
            ci1.setDisabled(true);

            this.form.addMember(this.valForm);
        }

        return this.form;
    }

    public boolean nextPage() {

        if (!valForm.validate() || createInProgress) {
            return false;
        }

        // protect against multiple calls to create if the user clicks Next multiple times.
        createInProgress = true;

        // protect against re-execution of this step via the "Previous" button. If we had created
        // a dest previously it must be deleted before we try to create a new one.
        if (wizard.isNewDestination() && (null != wizard.getDestination())) {
            bundleServer.deleteBundleDestination(wizard.getDestination().getId(), //
                new AsyncCallback<Void>() {
                    public void onSuccess(Void voidReturn) {
                        createDestination();
                    }

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_bundle_deployWizard_error_9(), caught);
                        // try anyway and potentially fail again from there 
                        createDestination();
                    }
                });
        } else {
            createDestination();
        }

        return false;
    }

    // this will advance or decrement the step depending on creation success or failure 
    private void createDestination() {
        int selectedGroup = (Integer) this.valForm.getValue("group");

        bundleServer.createBundleDestination(wizard.getBundleId(), dest.getName(), dest.getDescription(), dest
            .getDeployDir(), selectedGroup, //
            new AsyncCallback<BundleDestination>() {
                public void onSuccess(BundleDestination result) {
                    wizard.setDestination(result);
                    wizard.setNewDestination(true);
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_bundle_deployWizard_destinationCreatedDetail_concise(result.getName()),
                            MSG.view_bundle_deployWizard_destinationCreatedDetail(result.getName(), result
                                .getDescription()), Severity.Info));
                    createInProgress = false;
                    wizard.getView().incrementStep();
                }

                public void onFailure(Throwable caught) {
                    String message = MSG.view_bundle_deployWizard_error_10();
                    wizard.getView().showMessage(message);
                    CoreGUI.getErrorHandler().handleError(message, caught);
                    createInProgress = false;
                    wizard.getView().decrementStep();
                }
            });
    }
}
