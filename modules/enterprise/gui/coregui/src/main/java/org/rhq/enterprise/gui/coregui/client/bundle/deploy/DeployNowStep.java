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
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

public class DeployNowStep implements WizardStep {

    private DynamicForm form;
    private final BundleDeployWizard wizard;

    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();

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

            LinkedHashMap<String, Boolean> values = new LinkedHashMap<String, Boolean>();
            values.put("Deploy Now", Boolean.TRUE);
            values.put("Save Definition and Deploy Later", Boolean.FALSE);
            final RadioGroupItem radioGroupItem = new RadioGroupItem("options", "Deploy Options");
            radioGroupItem.setRequired(true);
            radioGroupItem.setValueMap(values);
            radioGroupItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    wizard.setDeployNow((Boolean) event.getValue());
                    enableNextButtonWhenAppropriate();
                }
            });

            form.setItems(radioGroupItem);
        }
        return form;
    }

    public boolean nextPage() {
        return form.validate();
    }

    public boolean isNextEnabled() {
        return (null != this.wizard.getDeployNow());
    }

    public boolean isPreviousEnabled() {
        return true;
    }

    private void enableNextButtonWhenAppropriate() {
        this.wizard.getView().getNextButton().setDisabled(!isNextEnabled());
    }

    private boolean isNotEmpty(String s) {
        return (s != null && s.trim().length() > 0);
    }
}
