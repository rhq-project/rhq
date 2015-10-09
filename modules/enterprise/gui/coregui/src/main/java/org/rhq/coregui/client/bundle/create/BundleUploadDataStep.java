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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedEvent;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.HeaderLabel;
import org.rhq.coregui.client.components.upload.BundleFileUploadForm;
import org.rhq.coregui.client.components.upload.DynamicFormHandler;
import org.rhq.coregui.client.components.upload.DynamicFormSubmitCompleteEvent;
import org.rhq.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

public class BundleUploadDataStep extends AbstractWizardStep {

    private final AbstractBundleCreateWizard wizard;
    private ArrayList<BundleFileUploadForm> uploadForms;
    private Boolean noFilesNeedToBeUploaded = null; // will be non-null when we know the answer

    public BundleUploadDataStep(AbstractBundleCreateWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public Canvas getCanvas() {
        final EnhancedVLayout mainLayout = new EnhancedVLayout();
        mainLayout.setMargin(20);
        mainLayout.setWidth100();
        mainLayout.setHeight(10);

        GWTServiceLookup.getBundleService().getAllBundleVersionFilenames(this.wizard.getBundleVersion().getId(),
            new AsyncCallback<HashMap<String, Boolean>>() {

                public void onSuccess(HashMap<String, Boolean> result) {
                    wizard.setAllBundleFilesStatus(result);
                    prepareForm(mainLayout);
                    // If no files need to be uploaded, we could skip immediately to the next step.
                    // I commented this out because if we do this, it prohibits someone
                    // going from the summary step back (via previous button) to here
                    // without it immediately going forward again to summary. I think its
                    // OK for the user to see a page that says, "no more files need to be uploaded"
                    // and have to hit the Next button to see the summary.
                    /*
                    if (noFilesNeedToBeUploaded) {
                        wizard.getView().incrementStep();
                    }
                    */
                }

                public void onFailure(Throwable caught) {
                    wizard.setAllBundleFilesStatus(null);
                    CoreGUI.getErrorHandler().handleError(MSG.view_bundle_createWizard_loadBundleFileFailure(), caught);
                }
            });

        return mainLayout;
    }

    public boolean nextPage() {
        wizard.getView().hideMessage();

        return isFinished();
    }

    public boolean previousPage() {
        wizard.getView().hideMessage();

        if (this.uploadForms != null) {
            for (BundleFileUploadForm uploadForm : this.uploadForms) {
                if (uploadForm.isUploadInProgress()) {
                    handleUploadError(
                        "[" + uploadForm.getName() + "] " + MSG.view_bundle_createWizard_uploadInProgress(), false);
                    return false;
                }
            }
        }

        return true;
    }

    public String getName() {
        return MSG.view_bundle_createWizard_uploadStepName();
    }

    private boolean isFinished() {
        if (noFilesNeedToBeUploaded != null && noFilesNeedToBeUploaded.booleanValue()) {
            return true;
        }

        if (wizard.getAllBundleFilesStatus() == null) {
            return false;
        }

        boolean needToUpload = false;
        for (BundleFileUploadForm uploadForm : this.uploadForms) {
            if (uploadForm.isUploadInProgress()) {
                handleUploadError("[" + uploadForm.getName() + "] " + MSG.view_bundle_createWizard_uploadInProgress(),
                    false);
                needToUpload = true;
            } else if (uploadForm.getUploadResult() == null) {
                needToUpload = true;
                uploadForm.submitForm();
                // on certain errors the form may never be submitted, report these errors outside submit handlers
                handleUploadError(uploadForm.getUploadError(), false);
            }
        }
        if (needToUpload) {
            return false;
        }

        if (wizard.getAllBundleFilesStatus().containsValue(Boolean.FALSE)) {
            return false;
        }
        return true;
    }

    private void prepareForm(VLayout mainLayout) {

        final HashMap<String, Boolean> allFilesStatus = wizard.getAllBundleFilesStatus();
        noFilesNeedToBeUploaded = Boolean.TRUE;

        if (null != allFilesStatus && !allFilesStatus.isEmpty()) {
            for (Map.Entry<String, Boolean> entry : allFilesStatus.entrySet()) {
                if (!entry.getValue()) {
                    noFilesNeedToBeUploaded = Boolean.FALSE;
                    break;
                }
            }
        }

        if (noFilesNeedToBeUploaded) {
            HeaderLabel label = new HeaderLabel(MSG.view_bundle_createWizard_noAdditionalFilesNeeded());
            label.setWidth100();
            mainLayout.addMember(label);
            uploadForms = null;
            return;
        }

        noFilesNeedToBeUploaded = Boolean.FALSE;
        uploadForms = new ArrayList<BundleFileUploadForm>();

        for (Map.Entry<String, Boolean> entry : allFilesStatus.entrySet()) {
            String fileToBeUploaded = entry.getKey();
            Boolean isAlreadyUploaded = entry.getValue();

            HLayout indivLayout = new HLayout();
            indivLayout.setWidth100();
            indivLayout.setAutoHeight();

            Label nameLabel = new Label(fileToBeUploaded + ": ");
            nameLabel.setWidth("*");
            nameLabel.setAlign(Alignment.RIGHT);
            nameLabel.setLayoutAlign(VerticalAlignment.CENTER);
            indivLayout.addMember(nameLabel);

            final BundleFileUploadForm uploadForm = new BundleFileUploadForm(this.wizard.getBundleVersion(),
                fileToBeUploaded, false, (isAlreadyUploaded) ? Boolean.TRUE : null);
            uploadForm.setWidth("75%");
            indivLayout.addMember(uploadForm);

            uploadForm.addFormHandler(new DynamicFormHandler() {
                public void onSubmitComplete(DynamicFormSubmitCompleteEvent event) {
                    String results = event.getResults();
                    if (!results.contains("Failed to upload bundle file")) { // this is looking for an error message coming from the server, its not i18n'ed
                        allFilesStatus.put(uploadForm.getName(), Boolean.TRUE);
                    } else {
                        allFilesStatus.put(uploadForm.getName(), Boolean.FALSE);
                        String errorMessage = uploadForm.getUploadError();
                        handleUploadError(errorMessage, true);
                    }
                }
            });
            uploadForm.addFormSubmitFailedHandler(new FormSubmitFailedHandler() {
                public void onFormSubmitFailed(FormSubmitFailedEvent event) {
                    allFilesStatus.put(uploadForm.getName(), Boolean.FALSE);
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_bundle_createWizard_failedToUploadFile(), Message.Severity.Error));
                }
            });

            uploadForms.add(uploadForm);

            mainLayout.addMember(indivLayout);
        }

        return;
    }

    private void handleUploadError(String errorMessage, boolean sendToMessageCenter) {
        if (null != errorMessage) {
            wizard.getView().showMessage(errorMessage);
        } else {
            errorMessage = "";
        }

        if (sendToMessageCenter) {
            CoreGUI.getMessageCenter().notify(
                new Message(MSG.view_bundle_createWizard_failedToUploadDistroFile(), errorMessage, Severity.Error));
        }
    }

}
