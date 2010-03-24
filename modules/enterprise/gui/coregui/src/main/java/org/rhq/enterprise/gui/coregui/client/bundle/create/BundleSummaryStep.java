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

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;

import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;

/**
 * @author John Mazzitelli
 */
public class BundleSummaryStep implements WizardStep {

    private DynamicForm form;
    private final AbstractBundleCreateWizard wizard;

    public BundleSummaryStep(AbstractBundleCreateWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public Canvas getCanvas() {
        TextItem nameTextItem;
        TextItem versionTextItem;
        TextAreaItem descriptionTextAreaItem;
        TextItem bundleTypeTextItem;

        form = new DynamicForm();
        form.setPadding(20);
        form.setWidth100();
        form.setNumCols(2);

        bundleTypeTextItem = new TextItem("bundleTypeItem", "Bundle Type");
        bundleTypeTextItem.setValue(wizard.getBundleType().getName());
        bundleTypeTextItem.setDisabled(true);
        bundleTypeTextItem.setTitleAlign(Alignment.LEFT);

        nameTextItem = new TextItem("name", "Name");
        nameTextItem.setValue(wizard.getBundleName());
        nameTextItem.setDisabled(true);
        nameTextItem.setTitleAlign(Alignment.LEFT);

        versionTextItem = new TextItem("version", "Version");
        versionTextItem.setValue(wizard.getBundleVersionString());
        versionTextItem.setDisabled(true);
        versionTextItem.setTitleAlign(Alignment.LEFT);

        descriptionTextAreaItem = new TextAreaItem("description", "Description");
        descriptionTextAreaItem.setValue(wizard.getBundleDescription());
        descriptionTextAreaItem.setDisabled(true);
        descriptionTextAreaItem.setTitleOrientation(TitleOrientation.TOP);
        descriptionTextAreaItem.setColSpan(2);
        descriptionTextAreaItem.setWidth(300);

        form.setItems(bundleTypeTextItem, nameTextItem, versionTextItem, descriptionTextAreaItem);

        return form;
    }

    public boolean nextPage() {
        return true; // this is the last page, we are done
    }

    public String getName() {
        return "Summary";
    }
}
