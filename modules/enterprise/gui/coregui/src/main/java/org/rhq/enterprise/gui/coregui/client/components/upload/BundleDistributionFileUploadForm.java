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
package org.rhq.enterprise.gui.coregui.client.components.upload;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.types.Encoding;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedEvent;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedHandler;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.UploadItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

public class BundleDistributionFileUploadForm extends DynamicCallbackForm {

    private UploadItem bundleUploadItem;
    private ButtonItem uploadButton;
    private StaticTextItem icon;

    private Boolean uploadResult;
    private String uploadError;
    private int bundleVersionId;

    private final FormItemIcon iconLoading;
    private final FormItemIcon iconGreen;
    private final FormItemIcon iconRed;
    private final boolean showUploadButton;

    public BundleDistributionFileUploadForm(boolean showUploadButton) {

        super("Distribution File");
        this.showUploadButton = showUploadButton;

        setEncoding(Encoding.MULTIPART);
        setAction(GWT.getModuleBaseURL() + "BundleDistributionFileUploadServlet");

        iconLoading = new FormItemIcon();
        iconLoading.setSrc("ajax-loader.gif");
        iconLoading.setWidth(16);
        iconLoading.setHeight(16);

        iconGreen = new FormItemIcon();
        iconGreen.setSrc("/images/icons/availability_green_16.png");
        iconGreen.setWidth(16);
        iconGreen.setHeight(16);

        iconRed = new FormItemIcon();
        iconRed.setSrc("/images/icons/availability_red_16.png");
        iconRed.setWidth(16);
        iconRed.setHeight(16);
    }

    /**
     * If this component successfully uploaded a bundle distribution file, this will return
     * the new bundle version's ID. Otherwise, 0 is returned.
     * 
     * @return the new bundle version ID
     */
    public int getBundleVersionId() {
        return this.bundleVersionId;
    }

    /**
     * Returns true if the file was successfully uploaded, false if an error occurred.
     * Returns null if this upload form has not be submitted yet (see {@link #submitForm()}).
     * @return status of the upload request
     */
    public Boolean getUploadResult() {
        return uploadResult;
    }

    /** 
     * @return Error text if {@link #getUploadResult()} returns false, otherwise null
     */
    public String getUploadError() {
        return uploadError;
    }

    private void setUploadError(String uploadError) {
        this.uploadError = uploadError;
    }

    @Override
    public void submitForm() {
        Object value = bundleUploadItem.getValue();
        if (value == null || value.toString().length() == 0) {
            icon.setIcons(iconRed);
            icon.setTooltip("Please select a bundle distribution file to upload");
        } else {
            icon.setIcons(iconLoading);
            icon.setTooltip("Processing...");
            super.submitForm();
        }
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        HiddenItem sessionIdField = new HiddenItem("sessionid");
        sessionIdField.setValue(CoreGUI.getSessionSubject().getSessionId().intValue());

        setNumCols(4);

        bundleUploadItem = new UploadItem("bundleFileUploadItem", "Distribution File");
        bundleUploadItem.setEndRow(false);
        bundleUploadItem.setShowTitle(false);

        uploadButton = new ButtonItem("Upload");
        uploadButton.setVisible(this.showUploadButton);
        uploadButton.setStartRow(false);
        uploadButton.setEndRow(false);
        uploadButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                submitForm();
            }
        });

        icon = new StaticTextItem("icon");
        icon.setStartRow(false);
        icon.setShowTitle(false);
        if (uploadResult != null) {
            if (uploadResult.booleanValue()) {
                icon.setIcons(iconGreen);
                icon.setTooltip("Bundle distribution file has already been uploaded");
            } else {
                icon.setIcons(iconRed);
                icon.setTooltip("Bundle distribution file upload has previously failed");
            }
        } else {
            icon.setIcons((FormItemIcon) null);
            icon.setTooltip("Select a file to upload, then click the 'Upload' button or 'Next'");
        }
        icon.setShowIcons(true);

        setItems(sessionIdField, bundleUploadItem, uploadButton, icon);

        // make sure this handler is executed first in case the creator has also added a handler
        pushFormHandler(new DynamicFormHandler() {
            public void onSubmitComplete(DynamicFormSubmitCompleteEvent event) {
                String results = event.getResults();
                bundleVersionId = parseIdFromResponse(results);
                if (bundleVersionId > 0) {
                    uploadResult = Boolean.TRUE;
                    icon.setIcons(iconGreen);
                    icon.setTooltip("Uploaded bundle distribution file successfully");
                    CoreGUI.getMessageCenter().notify(
                        new Message("Uploaded bundle distribution file successfully", results, Severity.Info));
                    icon.hide();
                    icon.show();
                } else {
                    uploadResult = Boolean.FALSE;
                    String cause = parseCauseFromResponse(results);
                    icon.setIcons(iconRed);
                    icon.setTooltip(cause);
                    setUploadError(cause);
                    CoreGUI.getMessageCenter().notify(
                        new Message("Bundle distribution file upload failed", results, Severity.Error));
                    icon.hide();
                    icon.show();
                }
            }
        });

        addFormSubmitFailedHandler(new FormSubmitFailedHandler() {
            public void onFormSubmitFailed(FormSubmitFailedEvent event) {
                uploadResult = Boolean.FALSE;
                String cause = "Bundle Distribution file upload failed, check for invalid file path.";
                icon.setIcons(iconRed);
                icon.setTooltip(cause);
                setUploadError(cause);
                CoreGUI.getMessageCenter().notify(new Message(cause, Severity.Error));
                icon.hide();
                icon.show();
            }
        });
    }

    private int parseIdFromResponse(String results) {
        String successMsgPrefix = "success ["; // the upload servlet will respond with "success [bundleVersionId]" on success
        int startSuccessMsgPrefix = results.indexOf(successMsgPrefix);
        if (startSuccessMsgPrefix < 0) {
            return 0; // must mean it wasn't a success - results is probably an error message
        }
        int endSuccessMsgPrefix = startSuccessMsgPrefix + successMsgPrefix.length();
        int startSuccessMsgPostfix = results.indexOf(']', endSuccessMsgPrefix);
        if (startSuccessMsgPostfix < 0) {
            return 0; // this should never happen, if we have "success [" we should always have the ending "]" bracket
        }
        String bundleVersionIdString = results.substring(endSuccessMsgPrefix, startSuccessMsgPostfix);
        int id = 0;
        try {
            id = Integer.parseInt(bundleVersionIdString);
        } catch (Exception e) {
            CoreGUI.getErrorHandler().handleError("Bad distribution file upload results", e);
        }
        return id;
    }

    private String parseCauseFromResponse(String results) {
        int i = (null == results) ? -1 : results.indexOf("\tat ");
        String cause = (-1 == i) ? results : results.substring(0, i);
        return cause;
    }

}