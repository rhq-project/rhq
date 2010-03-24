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

import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.smartgwt.client.widgets.Canvas;
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
        bundleTypeLabel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        bundleTypeLabel.setWordWrap(false);
        Label bundleTypeValue = new Label(wizard.getBundleType().getName());
        bundleTypeValue.setWidth("90%");
        bundleTypeValue.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        bundleTypeBox.addMember(bundleTypeLabel);
        bundleTypeBox.addMember(bundleTypeValue);
        top.addMember(bundleTypeBox);

        HLayout nameBox = new HLayout();
        nameBox.setWidth100();
        nameBox.setExtraSpace(10);
        Label nameLabel = new Label("Name:");
        nameLabel.setWidth("10%");
        nameLabel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        nameLabel.setWordWrap(false);
        Label nameValue = new Label(wizard.getBundleName());
        nameValue.setWidth("90%");
        nameValue.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        nameBox.addMember(nameLabel);
        nameBox.addMember(nameValue);
        top.addMember(nameBox);
        top.addMember(new LayoutSpacer());

        HLayout versionBox = new HLayout();
        versionBox.setWidth100();
        versionBox.setExtraSpace(10);
        Label versionLabel = new Label("Version:");
        versionLabel.setWidth("10%");
        versionLabel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        versionLabel.setWordWrap(false);
        Label versionValue = new Label(wizard.getBundleVersionString());
        versionValue.setWidth("90%");
        versionValue.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        versionBox.addMember(versionLabel);
        versionBox.addMember(versionValue);
        top.addMember(versionBox);
        top.addMember(new LayoutSpacer());

        HLayout descriptionBox = new HLayout();
        descriptionBox.setWidth100();
        descriptionBox.setExtraSpace(10);
        Label descriptionLabel = new Label("Description:");
        descriptionLabel.setWidth("10%");
        descriptionLabel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        descriptionLabel.setWordWrap(false);
        Label descriptionValue = new Label(wizard.getBundleDescription());
        descriptionValue.setWidth("90%");
        descriptionValue.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        descriptionBox.addMember(descriptionLabel);
        descriptionBox.addMember(descriptionValue);
        top.addMember(descriptionBox);
        top.addMember(new LayoutSpacer());

        HLayout filesBox = new HLayout();
        filesBox.setWidth100();
        filesBox.setExtraSpace(10);
        Label filesLabel = new Label("Files:");
        filesLabel.setWidth("10%");
        filesLabel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        filesLabel.setWordWrap(false);
        VLayout filesValues = new VLayout();
        filesValues.setWidth("90%");
        filesValues.setAutoHeight();
        for (String filename : wizard.getAllBundleFilesStatus().keySet()) {
            Label fileNameValue = new Label(filename);
            fileNameValue.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
            fileNameValue.setWordWrap(false);
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
