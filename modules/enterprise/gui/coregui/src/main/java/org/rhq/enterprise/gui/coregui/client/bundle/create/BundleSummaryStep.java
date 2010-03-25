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
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;

/**
 * @author John Mazzitelli
 */
public class BundleSummaryStep implements WizardStep {

    private final AbstractBundleCreateWizard wizard;

    public BundleSummaryStep(AbstractBundleCreateWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public Canvas getCanvas() {

        VLayout top = new VLayout();
        top.setAutoHeight();
        top.setPadding(20);
        top.setWidth100();

        HLayout bundleTypeBox = new HLayout();
        bundleTypeBox.setWidth100();
        bundleTypeBox.setExtraSpace(10);
        Label bundleTypeLabel = new Label("Type:");
        bundleTypeLabel.setWidth("10%");
        bundleTypeLabel.setAlign(Alignment.LEFT);
        bundleTypeLabel.setWrap(false);
        Label bundleTypeValue = new Label(wizard.getBundleType().getName());
        bundleTypeValue.setWidth("90%");
        bundleTypeValue.setAlign(Alignment.LEFT);
        bundleTypeBox.addMember(bundleTypeLabel);
        bundleTypeBox.addMember(bundleTypeValue);
        top.addMember(bundleTypeBox);

        HLayout nameBox = new HLayout();
        nameBox.setWidth100();
        nameBox.setExtraSpace(10);
        Label nameLabel = new Label("Name:");
        nameLabel.setWidth("10%");
        nameLabel.setAlign(Alignment.LEFT);
        nameLabel.setWrap(false);
        Label nameValue = new Label(wizard.getBundleName());
        nameValue.setWidth("90%");
        nameValue.setAlign(Alignment.LEFT);
        nameBox.addMember(nameLabel);
        nameBox.addMember(nameValue);
        top.addMember(nameBox);
        top.addMember(new LayoutSpacer());

        HLayout versionBox = new HLayout();
        versionBox.setWidth100();
        versionBox.setExtraSpace(10);
        Label versionLabel = new Label("Version:");
        versionLabel.setWidth("10%");
        versionLabel.setAlign(Alignment.LEFT);
        versionLabel.setWrap(false);
        Label versionValue = new Label(wizard.getBundleVersionString());
        versionValue.setWidth("90%");
        versionValue.setAlign(Alignment.LEFT);
        versionBox.addMember(versionLabel);
        versionBox.addMember(versionValue);
        top.addMember(versionBox);
        top.addMember(new LayoutSpacer());

        HLayout descriptionBox = new HLayout();
        descriptionBox.setWidth100();
        descriptionBox.setExtraSpace(10);
        Label descriptionLabel = new Label("Description:");
        descriptionLabel.setWidth("10%");
        descriptionLabel.setAlign(Alignment.LEFT);
        descriptionLabel.setWrap(false);
        Label descriptionValue = new Label(wizard.getBundleDescription());
        descriptionValue.setWidth("90%");
        descriptionValue.setAlign(Alignment.LEFT);
        descriptionBox.addMember(descriptionLabel);
        descriptionBox.addMember(descriptionValue);
        top.addMember(descriptionBox);
        top.addMember(new LayoutSpacer());

        HLayout filesBox = new HLayout();
        filesBox.setWidth100();
        filesBox.setExtraSpace(10);
        Label filesLabel = new Label("Files:");
        filesLabel.setWidth("10%");
        filesLabel.setAlign(Alignment.LEFT);
        filesLabel.setValign(VerticalAlignment.TOP);
        filesLabel.setWrap(false);
        VLayout filesValues = new VLayout();
        filesValues.setWidth("90%");
        filesValues.setAutoHeight();
        for (String filename : wizard.getAllBundleFilesStatus().keySet()) {
            Label fileNameValue = new Label(filename);
            fileNameValue.setAlign(Alignment.LEFT);
            fileNameValue.setValign(VerticalAlignment.TOP);
            fileNameValue.setWrap(false);
            fileNameValue.setHeight(10);
            filesValues.addMember(fileNameValue);
        }
        filesBox.addMember(filesLabel);
        filesBox.addMember(filesValues);
        top.addMember(filesBox);

        return top;
    }

    public boolean nextPage() {
        return true; // this is the last page, we are done
    }

    public String getName() {
        return "Summary";
    }
}
