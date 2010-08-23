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
package org.rhq.enterprise.gui.coregui.client.bundle.revert;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author Jay Shaughnessy
 *
 */
public class GetRevertInfoStep extends AbstractWizardStep {

    private DynamicForm form;
    private final BundleRevertWizard wizard;
    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();

    public GetRevertInfoStep(BundleRevertWizard wizard) {
        this.wizard = wizard;
    }

    public String getName() {
        return "Provide Revert Information";
    }

    public Canvas getCanvas() {
        if (form == null) {
            form = new LocatableDynamicForm("BundleRevertGetRevertInfo");
            form.setWidth100();
            form.setNumCols(2);
            form.setColWidths("50%", "*");

            bundleServer.getBundleDeploymentName(wizard.getDestination().getId(), -1, wizard.getPreviousDeployment()
                .getId(), //
                new AsyncCallback<String>() {

                    public void onSuccess(String result) {
                        final StaticTextItem nameTextItem = new StaticTextItem("name", "Revert Deployment Name");
                        nameTextItem.setWidth(300);
                        wizard.setSubtitle(result);
                        nameTextItem.setValue(result);

                        final TextAreaItem descriptionTextAreaItem = new TextAreaItem("description",
                            "Revert Deployment Description");
                        descriptionTextAreaItem.setWidth(300);
                        String liveDesc = wizard.getLiveDeployment().getDescription();
                        liveDesc = (null == liveDesc) ? wizard.getLiveDeployment().getName() : liveDesc;
                        String prevDesc = wizard.getPreviousDeployment().getDescription();
                        prevDesc = (null == prevDesc) ? wizard.getPreviousDeployment().getName() : prevDesc;
                        wizard.setDeploymentDescription("[REVERT From]\n" + liveDesc + "\n\n[REVERT To]\n" + prevDesc);
                        descriptionTextAreaItem.setValue(wizard.getDeploymentDescription());
                        descriptionTextAreaItem.addChangedHandler(new ChangedHandler() {
                            public void onChanged(ChangedEvent event) {
                                Object value = event.getValue();
                                if (value == null) {
                                    value = "";
                                }
                                wizard.setDeploymentDescription(value.toString());
                            }
                        });

                        final CheckboxItem cleanDeploymentCBItem = new CheckboxItem("cleanDeployment",
                            "Clean Deployment? (wipe deploy directory prior to the revert deploy)");
                        cleanDeploymentCBItem.setValue(wizard.isCleanDeployment());
                        cleanDeploymentCBItem.addChangedHandler(new ChangedHandler() {
                            public void onChanged(ChangedEvent event) {
                                wizard.setCleanDeployment((Boolean) event.getValue());
                            }
                        });

                        form.setItems(nameTextItem, descriptionTextAreaItem, cleanDeploymentCBItem);
                    }

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to get revert deployment name.", caught);
                    }
                });
        }

        return form;
    }

    public boolean nextPage() {
        return form.validate();
    }
}
