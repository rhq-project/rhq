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
import java.util.HashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
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
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * @author John Mazzitelli
 */
public class BundleInfoStep implements WizardStep {

    private DynamicForm form;
    private final AbstractBundleCreateWizard wizard;

    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();
    private final HashMap<String, BundleType> knownBundleTypes = new HashMap<String, BundleType>();

    private TextItem nameTextItem;
    private TextItem versionTextItem;
    private TextAreaItem descriptionTextAreaItem;
    private SelectItem bundleTypeDropDownMenu;

    public BundleInfoStep(AbstractBundleCreateWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public Canvas getCanvas() {
        if (form == null) {
            form = new DynamicForm();
            form.setPadding(20);
            form.setWidth100();
            form.setNumCols(2);

            bundleTypeDropDownMenu = new SelectItem("bundleTypeDropDownMenu", "Bundle Type");
            bundleTypeDropDownMenu.setVisible(false);
            bundleTypeDropDownMenu.setDisabled(true);
            bundleTypeDropDownMenu.setTitleAlign(Alignment.LEFT);
            bundleTypeDropDownMenu.setAllowEmptyValue(false);
            bundleTypeDropDownMenu.setMultiple(false);
            bundleTypeDropDownMenu.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    BundleType bundleType = knownBundleTypes.get(event.getValue());
                    wizard.setBundleType(bundleType);
                }
            });

            nameTextItem = new TextItem("name", "Name");
            nameTextItem.setRequired(true);
            nameTextItem.setTitleAlign(Alignment.LEFT);
            nameTextItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    String value = getValueAsString(event.getValue());
                    wizard.setSubtitle(value);
                }
            });

            final TextItem previousVersionTextItem = new TextItem("previousVersion", "Previous Version");
            previousVersionTextItem.setTitleAlign(Alignment.LEFT);

            versionTextItem = new TextItem("version");
            versionTextItem.setRequired(true);
            versionTextItem.setTitleAlign(Alignment.LEFT);

            descriptionTextAreaItem = new TextAreaItem("description", "Description");
            descriptionTextAreaItem.setTitleOrientation(TitleOrientation.TOP);
            descriptionTextAreaItem.setColSpan(2);
            descriptionTextAreaItem.setWidth(300);

            form.setItems(bundleTypeDropDownMenu, nameTextItem, previousVersionTextItem, versionTextItem,
                descriptionTextAreaItem);

            BundleVersion initialBundleVersion = wizard.getBundleVersion();
            if (initialBundleVersion != null) {
                nameTextItem.setValue(initialBundleVersion.getName());
                nameTextItem.setDisabled(true);
                wizard.setSubtitle(initialBundleVersion.getName());

                previousVersionTextItem.setValue(initialBundleVersion.getVersion());
                previousVersionTextItem.setDisabled(true);

                versionTextItem.setTitle("New Version");
                String versionSuggestion = autoIncrementVersion(initialBundleVersion.getVersion());
                versionTextItem.setValue(versionSuggestion);

                descriptionTextAreaItem.setValue(initialBundleVersion.getDescription());

            } else {
                previousVersionTextItem.setVisible(Boolean.FALSE);
                versionTextItem.setTitle("Initial Version");
                versionTextItem.setValue("1.0");
            }

            if (wizard.getBundleType() == null) {
                bundleServer.getAllBundleTypes(new AsyncCallback<ArrayList<BundleType>>() {
                    public void onSuccess(ArrayList<BundleType> result) {
                        if (result == null || result.size() == 0) {
                            wizard.setBundleType(null);
                            CoreGUI.getMessageCenter().notify(
                                new Message("No bundle types are supported", Severity.Error));
                            return;
                        }

                        for (BundleType bundleType : result) {
                            knownBundleTypes.put(bundleType.getName(), bundleType);
                            if (wizard.getBundleType() == null) {
                                wizard.setBundleType(bundleType);
                                bundleTypeDropDownMenu.setDefaultValue(bundleType.getName());
                                bundleTypeDropDownMenu.setValue(bundleType.getName());
                            }
                        }
                        bundleTypeDropDownMenu.setValueMap(knownBundleTypes.keySet().toArray(new String[0]));
                        bundleTypeDropDownMenu.setDisabled(false);
                        // don't bother showing the menu if there is only one item
                        if (knownBundleTypes.size() > 1) {
                            bundleTypeDropDownMenu.setVisible(true);
                            bundleTypeDropDownMenu.show(); // in case we've already been rendered
                        }
                    }

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("No bundle types available", caught);
                    }
                });
            }

        } else {
            if (wizard.getBundleVersion() != null) {
                // we are traversing back to this step - don't allow changes if we've already created the bundle version
                bundleTypeDropDownMenu.setDisabled(true);
                nameTextItem.setDisabled(true);
                versionTextItem.setDisabled(true);
                descriptionTextAreaItem.setDisabled(true);
            }
        }

        return form;
    }

    public boolean nextPage() {
        if (form.validate() && this.wizard.getBundleType() != null) {
            wizard.setBundleName(getValueAsString(nameTextItem.getValue()));
            wizard.setBundleVersionString(getValueAsString(versionTextItem.getValue()));
            wizard.setBundleDescription(getValueAsString(descriptionTextAreaItem.getValue()));
            return true;
        }
        return false;
    }

    public String getName() {
        return "Provide Bundle Information";
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

    private String getValueAsString(Object obj) {
        return (obj != null) ? obj.toString() : "";
    }
}
