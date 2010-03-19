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
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * @author John Mazzitelli
 */
public class BundleInfoStep implements WizardStep {

    private DynamicForm form;
    private final BundleCreationWizard wizard;

    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();
    private TextItem nameTextItem;
    private TextItem versionTextItem;
    private TextAreaItem descriptionTextAreaItem;

    public BundleInfoStep(BundleCreationWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public Canvas getCanvas() {
        if (form == null) {
            form = new DynamicForm();
            form.setPadding(20);
            form.setWidth100();
            form.setNumCols(2);

            nameTextItem = new TextItem("name", "Name");
            nameTextItem.setRequired(true);
            nameTextItem.setTitleAlign(Alignment.LEFT);
            nameTextItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    String value = form.getValueAsString("name");
                    wizard.setSubtitle(value);
                }
            });


            final TextItem previousVersionTextItem = new TextItem("previousVersion", "Previous Version");
            previousVersionTextItem.setTitleAlign(Alignment.LEFT);

            versionTextItem = new TextItem("version");
            
            versionTextItem.setTitleAlign(Alignment.LEFT);

            descriptionTextAreaItem = new TextAreaItem("description", "Description");
            descriptionTextAreaItem.setTitleOrientation(TitleOrientation.TOP);
            descriptionTextAreaItem.setColSpan(2);
            descriptionTextAreaItem.setWidth(300);

            form.setItems(nameTextItem, previousVersionTextItem, versionTextItem, descriptionTextAreaItem);


            BundleVersion initialBundleVersion = wizard.getBundleVersion();
            if (initialBundleVersion != null) {
                form.setValue("name", initialBundleVersion.getName());
                nameTextItem.setDisabled(true);

                form.setValue("previousVersion", initialBundleVersion.getVersion());
                previousVersionTextItem.setDisabled(true);

                versionTextItem.setTitle("New Version");
                String versionSuggestion = autoIncrementVersion(initialBundleVersion.getVersion());
                form.setValue("version", versionSuggestion);

                form.setValue("description", initialBundleVersion.getDescription());

            } else {
                previousVersionTextItem.setVisible(Boolean.FALSE);
                versionTextItem.setTitle("Initial Version");
                form.setValue("version", "1.0");
            }

            if (wizard.getBundleType() == null) {
                // TODO: we should get all bundle types in a drop down menu and let the user pick
                //       for now assume we always get one (the filetemplate one) and use it
                bundleServer.getAllBundleTypes(new AsyncCallback<ArrayList<BundleType>>() {
                    public void onSuccess(ArrayList<BundleType> result) {
                        wizard.setBundleType(result.get(0));
                    }

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("No bundle types available", caught);
                    }
                });
            }

        } else {
            if (wizard.getBundleVersion() != null) {
                // we are traversing back to this step - don't allow changes if we've already created the bundle version
                nameTextItem.setDisabled(Boolean.TRUE);
                versionTextItem.setDisabled(Boolean.TRUE);
                descriptionTextAreaItem.setDisabled(Boolean.TRUE);
            }
        }


        form.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent itemChangedEvent) {
                enableNextButtonWhenAppropriate();
            }
        });

        return form;
    }

    public boolean nextPage() {
        if (form.validate()) {
            wizard.setBundleName(form.getValueAsString("name"));
            wizard.setBundleVersionString(form.getValueAsString("version"));
            wizard.setBundleDescription(form.getValueAsString("description"));

            return true;
        }
        return false;
    }

    public String getName() {
        return "Provide Bundle Information";
    }


    private void enableNextButtonWhenAppropriate() {
        this.wizard.getView().getNextButton().setDisabled(!(form.validate() && this.wizard.getBundleType() != null));
    }


    private String autoIncrementVersion(String oldVersion) {
        String newVersion = "1.0";
        if (oldVersion != null && oldVersion.length() != 0) {
            String[] parts = oldVersion.split("[^a-zA-Z0-9]");
            String lastPart = parts[parts.length - 1];
            try {
                int lastNumber = Integer.parseInt(lastPart);
                newVersion = oldVersion.substring(0, oldVersion.length() - lastPart.length()) + (lastNumber + 1);
            } catch (NumberFormatException nfe) {
                newVersion = oldVersion + ".1";
            }
        }
        return newVersion;
    }
}
