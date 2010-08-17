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

public class GetDestinationStep extends AbstractWizardStep {

    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();
    private final BundleDeployWizard wizard;
    private VLayout form;
    DynamicForm valForm = new DynamicForm();
    private SinglePlatformResourceGroupSelector selector;
    private BundleDestination dest = new BundleDestination();
    private boolean createInProgress = false;

    public GetDestinationStep(BundleDeployWizard wizard) {
        this.wizard = wizard;
    }

    public String getName() {
        return "New Destination";
    }

    public Canvas getCanvas() {
        if (this.form == null) {
            this.form = new VLayout();

            this.valForm.setWidth100();
            this.valForm.setNumCols(2);
            this.valForm.setColWidths("50%", "*");

            final TextItem nameTextItem = new TextItem("name", "Destination Name");
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

            final TextAreaItem descriptionTextAreaItem = new TextAreaItem("description", "Destination Description");
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

            final TextItem deployDirTextItem = new TextItem("deployDir",
                "Root Deployment Directory (on destination platforms)");
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

            this.selector = new SinglePlatformResourceGroupSelector("group", "Resource Group");
            this.selector.setWidth(300);
            this.selector.setRequired(true);
            Validator validator = new IsIntegerValidator();
            validator.setErrorMessage("You must select a valid resource group from the drop down");
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
                        CoreGUI.getErrorHandler().handleError(
                            "Failed to delete new destination in nextPage: " + caught.getMessage(), caught);
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
                        new Message("Created destination [" + result.getName() + "] description ["
                            + result.getDescription() + "]", Severity.Info));
                    createInProgress = false;
                    wizard.getView().incrementStep();
                }

                public void onFailure(Throwable caught) {
                    String message = "Failed to create destination, it may already exist. (Note, for an existing destination deploy from the Destination view)";
                    wizard.getView().showMessage(message);
                    CoreGUI.getErrorHandler().handleError(message + ": " + caught.getMessage(), caught);
                    createInProgress = false;
                    wizard.getView().decrementStep();
                }
            });
    }
}
