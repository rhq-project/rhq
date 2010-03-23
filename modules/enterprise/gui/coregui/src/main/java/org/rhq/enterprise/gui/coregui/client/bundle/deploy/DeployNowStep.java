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

import java.util.LinkedHashMap;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;

public class DeployNowStep implements WizardStep {

    static private final String DEPLOY_LATER = "later";
    static private final String DEPLOY_NOW = "now";

    private final BundleDeployWizard wizard;
    private DynamicForm form;

    public DeployNowStep(BundleDeployWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public String getName() {
        return "Deploy Now or Later";
    }

    public Canvas getCanvas() {
        if (form == null) {
            form = new DynamicForm();
            form.setWidth100();
            form.setNumCols(2);
            form.setColWidths("50%", "*");

            RadioGroupItem radioGroupItem = new RadioGroupItem("deployTime", "Deployment Time");
            LinkedHashMap<String, String> radioGroupValues = new LinkedHashMap<String, String>();
            radioGroupValues.put(DEPLOY_NOW, "Deploy Now");
            radioGroupValues.put(DEPLOY_LATER, "Save Definition and Deploy Later");
            radioGroupItem.setRequired(true);
            radioGroupItem.setValueMap(radioGroupValues);
            radioGroupItem.setValue(DEPLOY_NOW);
            wizard.setDeployNow(true);
            radioGroupItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    wizard.setDeployNow(DEPLOY_NOW.equals(event.getValue()));
                }
            });

            form.setItems(radioGroupItem);
        }
        return form;
    }

    public boolean nextPage() {
        return form.validate();
    }

    public boolean isPreviousEnabled() {
        return true;
    }
}
