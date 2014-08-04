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
package org.rhq.coregui.client.inventory.resource.factory;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedEvent;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedHandler;

import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.upload.DynamicFormHandler;
import org.rhq.coregui.client.components.upload.DynamicFormSubmitCompleteEvent;
import org.rhq.coregui.client.components.upload.PackageVersionFileUploadForm;
import org.rhq.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * @author Jay Shaughnessy
 */
public class ResourceFactoryPackageStep extends AbstractWizardStep {

    private PackageVersionFileUploadForm uploadForm;
    private ResourceFactoryCreateWizard wizard;
    private boolean isUploadComplete = false;

    public ResourceFactoryPackageStep(ResourceFactoryCreateWizard wizard) {
        this.wizard = wizard;
    }

    public Canvas getCanvas() {
        if (null == uploadForm) {

            uploadForm = new PackageVersionFileUploadForm(wizard.getNewResourcePackageType().getId(),
                ResourceTypeUtility.displayName(wizard.getChildType()), wizard.getNewResourceVersion(),
                wizard.getNewResourceArchitectureId(), null,
                true, true, null);
            uploadForm.setPadding(20);
            uploadForm.addFormHandler(new DynamicFormHandler() {
                public void onSubmitComplete(DynamicFormSubmitCompleteEvent event) {
                    processUpload();
                }
            });
            uploadForm.addFormSubmitFailedHandler(new FormSubmitFailedHandler() {
                public void onFormSubmitFailed(FormSubmitFailedEvent event) {
                    return;
                }
            });
        }

        return uploadForm;
    }

    private void processUpload() {
        if (Boolean.TRUE.equals(uploadForm.getUploadResult())) {
            isUploadComplete = true;
            this.wizard.setNewResourcePackageVersionId(uploadForm.getPackageVersionId());
        } else {
            String errorMessage = uploadForm.getUploadError();
            handleUploadError(errorMessage, true);
        }
    }

    public boolean nextPage() {
        wizard.getView().hideMessage();

        if (uploadForm.isUploadInProgress()) {
            handleUploadError(MSG.widget_resourceFactoryWizard_uploadInProgress(), false);
            return false;
        }

        if (!isUploadComplete) {
            uploadForm.submitForm();
            // on certain errors the form may never be submitted, report these errors outside submit handlers
            handleUploadError(uploadForm.getUploadError(), false);
            return false;
        }

        return true;
    }

    public String getName() {
        return MSG.widget_resourceFactoryWizard_uploadFileStepName();
    }

    private void handleUploadError(String errorMessage, boolean sendToMessageCenter) {
        if (null != errorMessage) {
            wizard.getView().showMessage(errorMessage);
        } else {
            errorMessage = "";
        }

        if (sendToMessageCenter) {
            CoreGUI.getMessageCenter().notify(
                new Message(MSG.widget_resourceFactoryWizard_uploadFailure() + ": " + errorMessage, Severity.Error));
        }
    }

}