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
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;

public class SelectPlatformsStep implements WizardStep {

    private final BundleDeployWizard wizard;
    private DynamicForm form;

    public SelectPlatformsStep(BundleDeployWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public String getName() {
        return "Select Target Platforms";
    }

    public Canvas getCanvas() {
        if (form == null) {
            form = new DynamicForm();
            form.setWidth100();
            form.setNumCols(2);
            form.setColWidths("50%", "*");

            // TODO: this is temporary until we have a resource selection step
            final TextItem platformIdTextItem = new TextItem("platformId", "Platform Resource Id");
            platformIdTextItem.setRequired(true);
            platformIdTextItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    Object value = event.getValue();
                    if (value == null) {
                        value = "";
                    }
                    wizard.setPlatformId(Integer.valueOf(value.toString()));
                }
            });

            form.setItems(platformIdTextItem);
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
