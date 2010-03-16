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
package org.rhq.enterprise.gui.coregui.client.bundle.create;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.core.domain.bundle.BundleType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * @author Greg Hinkle
 */
public class BundleInfoStep implements WizardStep {

    private DynamicForm form;
    private final BundleCreationWizard wizard;

    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();

    public BundleInfoStep(BundleCreationWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public Canvas getCanvas() {
        if (form == null) {
            form = new DynamicForm();
            form.setWidth100();
            form.setNumCols(2);
            form.setColWidths("50%", "*");

            final TextItem nameTextItem = new TextItem("name", "Name");
            nameTextItem.setRequired(true);
            nameTextItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    Object value = event.getValue();
                    if (value == null) {
                        value = "";
                    }
                    wizard.setSubtitle(value.toString());
                    wizard.setBundleName(value.toString());
                    enableNextButtonWhenAppropriate();
                }
            });

            final TextItem versionTextItem = new TextItem("version", "Initial Version");
            versionTextItem.setValue("1.0");
            wizard.setBundleVersionString("1.0");
            versionTextItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    Object value = event.getValue();
                    if (value == null) {
                        value = "";
                    }
                    wizard.setBundleVersionString(value.toString());
                    enableNextButtonWhenAppropriate();
                }
            });

            final TextAreaItem descriptionTextAreaItem = new TextAreaItem("description", "Description");
            descriptionTextAreaItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    Object value = event.getValue();
                    if (value == null) {
                        value = "";
                    }
                    wizard.setBundleDescription(value.toString());
                    enableNextButtonWhenAppropriate();
                }
            });

            form.setItems(nameTextItem, versionTextItem, descriptionTextAreaItem);

            // TODO: we should get all bundle types in a drop down menu and let the user pick
            //       for now assume we always get one (the filetemplate one) and use it
            bundleServer.getBundleTypes(new AsyncCallback<ArrayList<BundleType>>() {
                public void onSuccess(ArrayList<BundleType> result) {
                    wizard.setBundleType(result.get(0));
                    enableNextButtonWhenAppropriate();
                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("No bundle types available: " + caught.getMessage(), caught);
                }
            });
        }
        return form;
    }

    public boolean nextPage() {
        return form.validate();
    }

    public String getName() {
        return "Provide Bundle Information";
    }

    public boolean isNextEnabled() {
        return isNotEmpty(this.wizard.getBundleName()) && isNotEmpty(this.wizard.getBundleVersionString())
            && this.wizard.getBundleType() != null;
    }

    public boolean isPreviousEnabled() {
        return false;
    }

    private void enableNextButtonWhenAppropriate() {
        this.wizard.getView().getNextButton().setDisabled(!isNextEnabled());
    }

    private boolean isNotEmpty(String s) {
        return (s != null && s.trim().length() > 0);
    }
}
