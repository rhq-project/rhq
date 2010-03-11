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

import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * @author Greg Hinkle
 */
public class BundleUploadDataStep implements WizardStep {

    private final BundleCreationWizard wizard;
    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();
    private DynamicForm form;
    private Map<String, Boolean> allFilesStatus;

    public BundleUploadDataStep(BundleCreationWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public Canvas getCanvas() {
        form = new DynamicForm();

        final HLayout layout = new HLayout();
        layout.setWidth100();
        layout.setHeight100();

        bundleServer.getAllBundleVersionFilenames(this.wizard.getBundleVersion().getId(),
            new AsyncCallback<Map<String, Boolean>>() {

                public void onSuccess(Map<String, Boolean> result) {
                    allFilesStatus = result;
                    prepareForm(layout);
                    enableNextButtonWhenAppropriate();
                }

                public void onFailure(Throwable caught) {
                    allFilesStatus = null;
                    CoreGUI.getErrorHandler().handleError("Cannot obtain bundle file information from server", caught);
                }
            });

        form.addChild(layout);
        return form;
    }

    public boolean nextPage() {
        return true; // TODO: Implement this method.
    }

    public String getName() {
        return "Upload Bundle Files";
    }

    public boolean isNextEnabled() {
        return this.allFilesStatus != null; // TODO && when all files are available
    }

    public boolean isPreviousEnabled() {
        return false;
    }

    private void enableNextButtonWhenAppropriate() {
        this.wizard.getView().getNextButton().setDisabled(!isNextEnabled());
    }

    private void prepareForm(HLayout layout) {
        for (Map.Entry<String, Boolean> entry : this.allFilesStatus.entrySet()) {
            VLayout vlayout = new VLayout();
            layout.addChild(vlayout);

            Label label = new Label(entry.getKey());
            vlayout.addMember(label);

            if (entry.getValue()) {
                Img img = new Img("/images/status_complete.gif", 50, 15);
                vlayout.addMember(img);
            } else {
                // TODO I really want a file upload component here
                Img img = new Img("/images/status_error.gif", 50, 15);
                vlayout.addMember(img);
            }
        }
    }
}
