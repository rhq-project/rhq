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
package org.rhq.enterprise.gui.coregui.client.components.wizard;

import java.util.Collections;
import java.util.List;

import com.smartgwt.client.widgets.IButton;

public abstract class AbstractWizard implements Wizard {
    private List<WizardStep> steps;
    private WizardView view;
    private String windowTitle = "";
    private String title = "";
    private String subtitle = "";

    public String getWindowTitle() {
        return windowTitle;
    }

    public void setWindowTitle(String windowTitle) {
        this.windowTitle = windowTitle;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        if (view != null) {
            view.refreshTitleLabelContents();
        }
    }

    public List<WizardStep> getSteps() {
        return steps;
    }

    public void setSteps(List<WizardStep> steps) {
        this.steps = steps;
    }

    public List<IButton> getCustomButtons(int step) {
        return Collections.emptyList();
    }

    public void startWizard() {
        view = new WizardView(this);
        view.displayDialog();
    }

    public WizardView getView() {
        return view;
    }
}
