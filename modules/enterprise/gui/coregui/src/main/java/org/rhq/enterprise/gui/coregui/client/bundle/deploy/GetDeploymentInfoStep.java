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

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;

/**
 * @author Jay Shaughnessy
 *
 */
public class GetDeploymentInfoStep implements WizardStep {

    private DynamicForm form;
    private final BundleDeployWizard wizard;

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

            final StaticTextItem nameTextItem = new StaticTextItem("name", "Deployment Name");
            nameTextItem.setWidth(300);
            nameTextItem.setRequired(true);
            wizard.setNewDeploymentName(getDeploymentName());
            wizard.setSubtitle(wizard.getNewDeploymentName());
            nameTextItem.setValue(wizard.getNewDeploymentName());

            final TextAreaItem descriptionTextAreaItem = new TextAreaItem("description", "Deployment Description");
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

        return form;
    }

    private String getDeploymentName() {
        String deploymentName = "none";

        int deploy = 1;
        String version = wizard.getBundleVersion().getVersion();
        String dest = wizard.getDestination().getName();

        if (wizard.isInitialDeployment()) {
            deploymentName = "Deployment [" + deploy + "] of Version [" + version + "] to [" + dest + "]";
        } else {
            String liveName = wizard.getLiveDeployment().getName();
            String liveVersion = wizard.getLiveDeployment().getBundleVersion().getVersion();
            if (liveVersion.equals(version)) {
                // redeploy
                int iStart = liveName.indexOf("["), iEnd = liveName.indexOf("]");
                deploy = Integer.valueOf(liveName.substring(iStart, iEnd) + 1);
                deploymentName = "Deployment [" + deploy + "] of Version [" + version + "] to [" + dest + "]";
            } else {
                // upgrade
                deploymentName = "Deployment [" + deploy + "] of Version [" + version + "] to [" + dest
                    + "]. Upgrade from Version [" + liveVersion + "]";
            }
        }

        return deploymentName;
    }

    public boolean nextPage() {
        return form.validate();
    }
}
