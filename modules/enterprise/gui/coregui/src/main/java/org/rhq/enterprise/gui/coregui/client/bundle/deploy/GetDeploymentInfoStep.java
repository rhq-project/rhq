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
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author Jay Shaughnessy
 *
 */
public class GetDeploymentInfoStep extends AbstractWizardStep {

    private DynamicForm form;
    private final BundleDeployWizard wizard;
    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();

    public GetDeploymentInfoStep(BundleDeployWizard wizard) {
        this.wizard = wizard;
    }

    public String getName() {
        return MSG.view_bundle_deployWizard_getInfoStep();
    }

    public Canvas getCanvas() {
        if (form == null) {
            form = new LocatableDynamicForm("BundleDeployGetDepInfo");
            form.setWidth100();
            form.setNumCols(2);
            form.setColWidths("50%", "*");

            bundleServer.getBundleDeploymentName(wizard.getDestination().getId(), wizard.getBundleVersion().getId(),
                -1, //
                new AsyncCallback<String>() {

                    public void onSuccess(String result) {
                        final StaticTextItem nameTextItem = new StaticTextItem("name", MSG
                            .view_bundle_deployWizard_getInfo_deploymentName());
                        nameTextItem.setWidth(300);
                        wizard.setSubtitle(result);
                        nameTextItem.setValue(result);

                        final TextAreaItem descriptionTextAreaItem = new TextAreaItem("description", MSG
                            .view_bundle_deployWizard_getInfo_deploymentDesc());
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

                        final CheckboxItem cleanDeploymentCBItem = new CheckboxItem("cleanDeployment", MSG
                            .view_bundle_deployWizard_getInfo_clean());
                        cleanDeploymentCBItem.setValue(wizard.isCleanDeployment());
                        cleanDeploymentCBItem.addChangedHandler(new ChangedHandler() {
                            public void onChanged(ChangedEvent event) {
                                wizard.setCleanDeployment((Boolean) event.getValue());
                            }
                        });

                        form.setItems(nameTextItem, descriptionTextAreaItem, cleanDeploymentCBItem);

                    }

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_bundle_deployWizard_error_7(), caught);
                    }
                });
        }

        return form;
    }

    public boolean nextPage() {
        return form.validate();
    }
}
