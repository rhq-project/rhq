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
package org.rhq.coregui.client.bundle.create;

import java.util.Set;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.bundle.BundleGroup;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.wizard.AbstractWizardStep;

/**
 * @author John Mazzitelli
 */
public class BundleSummaryStep extends AbstractWizardStep {

    private final AbstractBundleCreateWizard wizard;

    public BundleSummaryStep(AbstractBundleCreateWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public Canvas getCanvas() {

        DynamicForm form = new DynamicForm();
        form.setNumCols(2);
        form.setMargin(20);

        StaticTextItem bundleTypeLabel = new StaticTextItem("bundleType", MSG.view_bundle_bundleType());
        bundleTypeLabel.setTitleAlign(Alignment.LEFT);
        bundleTypeLabel.setAlign(Alignment.LEFT);
        bundleTypeLabel.setWrap(false);
        bundleTypeLabel.setValue(wizard.getBundleVersion().getBundle().getBundleType().getName());

        StaticTextItem nameLabel = new StaticTextItem("name", MSG.common_title_name());
        nameLabel.setTitleAlign(Alignment.LEFT);
        nameLabel.setAlign(Alignment.LEFT);
        nameLabel.setWrap(false);
        nameLabel.setValue(wizard.getBundleVersion().getBundle().getName());

        StaticTextItem versionLabel = new StaticTextItem("Version", MSG.common_title_version());
        versionLabel.setTitleAlign(Alignment.LEFT);
        versionLabel.setTitleAlign(Alignment.LEFT);
        versionLabel.setAlign(Alignment.LEFT);
        versionLabel.setWrap(false);
        versionLabel.setValue(wizard.getBundleVersion().getVersion());

        StaticTextItem descriptionLabel = new StaticTextItem("Description", MSG.common_title_description());
        descriptionLabel.setTitleAlign(Alignment.LEFT);
        descriptionLabel.setAlign(Alignment.LEFT);
        descriptionLabel.setWrap(false);
        descriptionLabel.setValue(wizard.getBundleVersion().getBundle().getDescription());

        StaticTextItem filesLabel = new StaticTextItem("Files", MSG.view_bundle_files());
        filesLabel.setTitleVAlign(VerticalAlignment.TOP);
        filesLabel.setTitleAlign(Alignment.LEFT);
        filesLabel.setAlign(Alignment.LEFT);
        filesLabel.setWrap(false);
        StringBuilder filesValueStr = new StringBuilder();
        for (String filename : wizard.getAllBundleFilesStatus().keySet()) {
            filesValueStr.append(filename + "<br/>\n");
        }
        filesLabel.setValue(filesValueStr.toString());

        StaticTextItem bundleGroupsLabel = new StaticTextItem("BundleGroups", MSG.common_title_bundleGroups());
        bundleGroupsLabel.setTitleVAlign(VerticalAlignment.TOP);
        bundleGroupsLabel.setTitleAlign(Alignment.LEFT);
        bundleGroupsLabel.setAlign(Alignment.LEFT);
        bundleGroupsLabel.setWrap(false);
        Set<BundleGroup> initialBundleGroups = wizard.getInitialBundleGroups();
        if (null == initialBundleGroups || initialBundleGroups.isEmpty()) {
            bundleGroupsLabel.setValue(MSG.view_bundle_createWizard_unassigned());
        } else {
            StringBuilder bundleGroupsValueStr = new StringBuilder();
            for (BundleGroup bundleGroup : wizard.getInitialBundleGroups()) {
                bundleGroupsValueStr.append(bundleGroup.getName() + "<br/>\n");
            }
            bundleGroupsLabel.setValue(bundleGroupsValueStr.toString());
        }

        form.setFields(bundleTypeLabel, nameLabel, versionLabel, descriptionLabel, filesLabel, bundleGroupsLabel);

        return form;
    }

    public boolean nextPage() {
        CoreGUI.refresh();

        return true; // this is the last page, we are done
    }

    public String getName() {
        return MSG.common_title_summary();
    }
}
