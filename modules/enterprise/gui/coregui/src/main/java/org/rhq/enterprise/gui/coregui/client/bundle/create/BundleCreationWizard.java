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

import org.rhq.enterprise.gui.coregui.client.components.wizard.Wizard;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardView;

/**
 * @author Greg Hinkle
 */
public class BundleCreationWizard implements Wizard {

    private ArrayList<WizardStep> steps;

    private WizardView view;

    public BundleCreationWizard() {

        steps = new ArrayList<WizardStep>();

        steps.add(new BundleInfoStep());
        steps.add(new BundleUploadRecipeStep());
        steps.add(new BundleUploadDataStep());
        steps.add(new BundleVerificationStep());

    }

    public String getWindowTitle() {
        return "Bundle Creation Wizard";
    }

    public String getTitle() {
        return "Create Bundle";
    }

    public String getSubtitle() {
        return "";
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
}
