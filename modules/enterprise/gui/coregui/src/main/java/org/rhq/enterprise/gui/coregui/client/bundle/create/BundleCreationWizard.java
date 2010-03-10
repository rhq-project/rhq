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
import java.util.Collections;
import java.util.List;

import com.smartgwt.client.widgets.IButton;

import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.enterprise.gui.coregui.client.components.wizard.Wizard;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardView;

/**
 * @author Greg Hinkle
 */
public class BundleCreationWizard implements Wizard {

    private ArrayList<WizardStep> steps;
    private WizardView view;
    private String subtitle = "";

    // the things we build up in the wizard
    private BundleType bundleType;
    private String bundleName;
    private String bundleVersionString;
    private String bundleDescription;
    private String recipe;
    private BundleVersion bundleVersion;

    public BundleCreationWizard() {

        steps = new ArrayList<WizardStep>();

        steps.add(new BundleInfoStep(this));
        steps.add(new BundleUploadRecipeStep(this));
        steps.add(new BundleVerificationStep(this));
        steps.add(new BundleUploadDataStep(this));

    }

    public String getWindowTitle() {
        return "Bundle Creation Wizard";
    }

    public String getTitle() {
        return "Create Bundle";
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        this.view.refreshTitleLabelContents();
    }

    public List<WizardStep> getSteps() {
        return steps;
    }

    public List<IButton> getCustomButtons(int step) {
        return Collections.emptyList();
    }

    public void startBundleCreateWizard() {
        view = new WizardView(this);
        view.displayDialog();
    }

    public BundleType getBundleType() {
        return bundleType;
    }

    public void setBundleType(BundleType bundleType) {
        this.bundleType = bundleType;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public String getBundleVersionString() {
        return bundleVersionString;
    }

    public void setBundleVersionString(String bundleVersionString) {
        this.bundleVersionString = bundleVersionString;
    }

    public String getBundleDescription() {
        return bundleDescription;
    }

    public void setBundleDescription(String desc) {
        this.bundleDescription = desc;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public BundleVersion getBundleVersion() {
        return bundleVersion;
    }

    public void setBundleVersion(BundleVersion bv) {
        this.bundleVersion = bv;
    }
}
