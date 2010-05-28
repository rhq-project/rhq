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
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * @author Jay Shaughnessy
 *
 */
public class GetDeploymentInfoStep implements WizardStep {

    private DynamicForm form;
    private final BundleDeployWizard wizard;
    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();

    public GetDeploymentInfoStep(BundleDeployWizard wizard) {
        this.wizard = wizard;
    }

    public String getName() {
        return "Provide Deployment Information";
    }

    public Canvas getCanvas() {
        if (form == null) {
            form = new DynamicForm();
            form.setWidth100();
            form.setNumCols(2);
            form.setColWidths("50%", "*");

            bundleServer.getBundleDeploymentName(wizard.getDestination().getId(), wizard.getBundleVersion().getId(),
                -1, //
                new AsyncCallback<String>() {

                    public void onSuccess(String result) {
                        final StaticTextItem nameTextItem = new StaticTextItem("name", "Deployment Name");
                        nameTextItem.setWidth(300);
                        wizard.setSubtitle(result);
                        nameTextItem.setValue(result);

                        final TextAreaItem descriptionTextAreaItem = new TextAreaItem("description",
                            "Deployment Description");
                        descriptionTextAreaItem.setWidth(300);
                        descriptionTextAreaItem.addChangedHandler(new ChangedHandler() {
                            public void onChanged(ChangedEvent event) {
                                Object value = event.getValue();
                                if (value == null) {
                                    value = "";
                                }
                                wizard.setNewDeploymentDescription(value.toString());
                            }
                        });

                        final CheckboxItem cleanDeploymentCBItem = new CheckboxItem("cleanDeployment",
                            "Clean Deployment? (wipe deploy directory on destination platform)");
                        cleanDeploymentCBItem.setValue(wizard.isCleanDeployment());
                        cleanDeploymentCBItem.addChangedHandler(new ChangedHandler() {
                            public void onChanged(ChangedEvent event) {
                                wizard.setCleanDeployment((Boolean) event.getValue());
                            }
                        });

                        form.setItems(nameTextItem, descriptionTextAreaItem, cleanDeploymentCBItem);

                    }

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to get deployment name.", caught);
                    }
                });
        }

        return form;
    }

    public boolean nextPage() {
        return form.validate();
    }
}
