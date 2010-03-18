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
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

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
            form.setWidth100();
            form.setNumCols(2);
            form.setColWidths("50%", "*");

            BundleVersion initialBundleVersion = wizard.getBundleVersion();

            nameTextItem = new TextItem("name", "Name");
            nameTextItem.setRequired(true);
            if (initialBundleVersion == null) {
                nameTextItem.setValue("");
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
            } else {
                nameTextItem.setValue(initialBundleVersion.getName());
                nameTextItem.setDisabled(true);
            }

            final TextItem previousVersionTextItem = new TextItem("previousVersion", "Previous Version");
            if (initialBundleVersion == null) {
                previousVersionTextItem.setVisible(Boolean.FALSE);
            } else {
                previousVersionTextItem.setValue(initialBundleVersion.getVersion());
                previousVersionTextItem.setDisabled(true);
            }

            versionTextItem = new TextItem("version");
            if (initialBundleVersion == null) {
                versionTextItem.setTitle("Initial Version");
                String versionSuggestion = "1.0";
                versionTextItem.setValue(versionSuggestion);
                wizard.setBundleVersionString(versionSuggestion);
            } else {
                versionTextItem.setTitle("New Version");
                String versionSuggestion = autoIncrementVersion(initialBundleVersion.getVersion());
                versionTextItem.setValue(versionSuggestion);
                wizard.setBundleVersionString(versionSuggestion);
            }
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

            descriptionTextAreaItem = new TextAreaItem("description", "Description");
            if (initialBundleVersion == null) {
                descriptionTextAreaItem.setValue("");
            } else {
                descriptionTextAreaItem.setValue(initialBundleVersion.getDescription());
            }
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

            form.setItems(nameTextItem, previousVersionTextItem, versionTextItem, descriptionTextAreaItem);

            if (wizard.getBundleType() == null) {
                // TODO: we should get all bundle types in a drop down menu and let the user pick
                //       for now assume we always get one (the filetemplate one) and use it
                bundleServer.getAllBundleTypes(new AsyncCallback<ArrayList<BundleType>>() {
                    public void onSuccess(ArrayList<BundleType> result) {
                        wizard.setBundleType(result.get(0));
                        enableNextButtonWhenAppropriate();
                    }

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("No bundle types available", caught);
                    }
                });
            }

            enableNextButtonWhenAppropriate();
        } else {
            if (wizard.getBundleVersion() != null) {
                // we are traversing back to this step - don't allow changes if we've already created the bundle version
                nameTextItem.setDisabled(Boolean.TRUE);
                versionTextItem.setDisabled(Boolean.TRUE);
                descriptionTextAreaItem.setDisabled(Boolean.TRUE);
            }
        }
        return form;
    }

    public boolean nextPage() {
        boolean ok = form.validate();
        return ok;
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
