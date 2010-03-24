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

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedEvent;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.upload.BundleFileUploadForm;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicFormHandler;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicFormSubmitCompleteEvent;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

public class BundleUploadDataStep implements WizardStep {

    private final AbstractBundleCreateWizard wizard;
    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();
    private DynamicForm form;

    public BundleUploadDataStep(AbstractBundleCreateWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public Canvas getCanvas() {
        form = new DynamicForm();

        final VLayout layout = new VLayout();
        layout.setMargin(Integer.valueOf(20));
        layout.setAlign(Alignment.CENTER);

        bundleServer.getAllBundleVersionFilenames(this.wizard.getBundleVersion().getId(),
            new AsyncCallback<HashMap<String, Boolean>>() {

                public void onSuccess(HashMap<String, Boolean> result) {
                    wizard.setAllBundleFilesStatus(result);
                    prepareForm(layout);
                }

                public void onFailure(Throwable caught) {
                    wizard.setAllBundleFilesStatus(null);
                    CoreGUI.getErrorHandler().handleError("Cannot obtain bundle file information from server", caught);
                }
            });

        form.addChild(layout);
        return form;
    }

    public boolean nextPage() {
        return isFinished();
    }

    public String getName() {
        return "Upload Bundle Files";
    }

    private boolean isFinished() {
        if (wizard.getAllBundleFilesStatus() == null) {
            return false;
        }
        if (wizard.getAllBundleFilesStatus().containsValue(Boolean.FALSE)) {
            return false;
        }
        return true;
    }

    private void prepareForm(VLayout layout) {
        // if there are no files to upload, immediately skip this step
        final HashMap<String, Boolean> allFilesStatus = wizard.getAllBundleFilesStatus();

        if (allFilesStatus != null && allFilesStatus.size() == 0) {
            // TODO: do something to tell the user they don't have to do anything for this step
            return;
        }

        for (Map.Entry<String, Boolean> entry : allFilesStatus.entrySet()) {
            HLayout formLayout = new HLayout();
            layout.addMember(formLayout);

            String fileToBeUploaded = entry.getKey();
            Boolean isAlreadyUploaded = entry.getValue();

            if (isAlreadyUploaded) {
                Label nameLabel = new Label(fileToBeUploaded + ": ");
                formLayout.addMember(nameLabel);
                Img img = new Img("/images/icons/availability_green_16.png", 16, 16);
                formLayout.addMember(img);
            } else {
                final BundleFileUploadForm uploadForm = new BundleFileUploadForm(this.wizard.getBundleVersion(),
                    fileToBeUploaded);
                uploadForm.addFormHandler(new DynamicFormHandler() {
                    public void onSubmitComplete(DynamicFormSubmitCompleteEvent event) {
                        String results = event.getResults();
                        if (!results.contains("Failed to upload bundle file")) {
                            uploadForm.retrievalStatus(true);
                            allFilesStatus.put(uploadForm.getName(), Boolean.TRUE);
                        } else {
                            uploadForm.retrievalStatus(false);
                            allFilesStatus.put(uploadForm.getName(), Boolean.FALSE);
                            CoreGUI.getMessageCenter().notify(
                                new Message("Failed to upload bundle file", results, Message.Severity.Error));
                        }
                    }
                });
                uploadForm.addFormSubmitFailedHandler(new FormSubmitFailedHandler() {
                    public void onFormSubmitFailed(FormSubmitFailedEvent event) {
                        uploadForm.retrievalStatus(false);
                        allFilesStatus.put(uploadForm.getName(), Boolean.FALSE);
                        CoreGUI.getMessageCenter().notify(
                            new Message("Failed to upload file", null, Message.Severity.Error));
                    }
                });

                formLayout.addMember(uploadForm);
            }
        }
    }
}
