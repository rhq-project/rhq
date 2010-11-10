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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.types.Encoding;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedEvent;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedHandler;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.UploadItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * A base form widget for file upload. Uploaded files are uploaded to the server into a temp directory via
 * FileUploadServlet. 
 * 
 * @author Jay Shaughnessy
 */
public class FileUploadForm extends DynamicCallbackForm {

    private UploadItem fileUploadItem;
    private ButtonItem uploadButton;
    private StaticTextItem icon;

    private Boolean uploadResult;
    private String uploadError;
    private boolean uploadInProgress;

    private final String name;
    private final String version;
    private final boolean showNameLabel;
    private final boolean showUploadButton;
    private final FormItemIcon iconLoading;
    private final FormItemIcon iconGreen;
    private final FormItemIcon iconRed;
    private final FormItemIcon iconGrey;

    private List<String> uploadedFilePaths;

    public FileUploadForm(String locatorId, String name, String version, boolean showNameLabel,
        boolean showUploadButton, Boolean isAlreadyUploaded) {

        super(locatorId, name);
        this.name = name;
        this.version = version;
        this.showNameLabel = showNameLabel;
        this.showUploadButton = showUploadButton;
        this.uploadResult = isAlreadyUploaded; // null if unknown, false if error during previous upload attempt, true if already uploaded before
        this.uploadInProgress = false;

        setEncoding(Encoding.MULTIPART);
        setAction(GWT.getModuleBaseURL() + "/FileUploadServlet");

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

        iconGrey = new FormItemIcon();
        iconGrey.setSrc("/images/icons/availability_grey_16.png");
        iconGrey.setWidth(16);
        iconGrey.setHeight(16);
    }

    /**
     * The name of the file that is to be uploaded to the server. The actual client file can
     * be named whatever, but the server will use this name as the name of the file.
     * @return file name
     */
    public String getName() {
        return name;
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

    public boolean isUploadInProgress() {
        return uploadInProgress;
    }

    @Override
    public void submitForm() {
        setUploadError(null);

        if (uploadInProgress) {
            String message = "Can not submit, upload is currently in progress";
            setUploadError(message);
            return;
        }

        Object value = fileUploadItem.getValue();
        if (value == null || value.toString().length() == 0) {
            String message = "[" + name + "] Please select a file to upload";
            icon.setIcons(iconRed);
            icon.setTooltip(message);
            setUploadError(message);
            // note - don't even submit this definite failure            
        } else {
            icon.setIcons(iconLoading);
            icon.setTooltip("Loading...");
            uploadInProgress = true;
            super.submitForm();
        }
    }

    protected List<FormItem> getOnDrawItems() {
        List<FormItem> onDrawItems = new ArrayList<FormItem>();

        HiddenItem sessionIdField = new HiddenItem("sessionid");
        sessionIdField.setValue(UserSessionManager.getSessionSubject().getSessionId().intValue());
        onDrawItems.add(sessionIdField);

        HiddenItem nameField = new HiddenItem("name");
        nameField.setValue(this.name);
        onDrawItems.add(nameField);

        HiddenItem versionField = new HiddenItem("version");
        versionField.setValue(version);
        onDrawItems.add(versionField);

        fileUploadItem = new UploadItem("fileUploadItem", name);
        fileUploadItem.setEndRow(false);
        fileUploadItem.setShowTitle(showNameLabel);
        onDrawItems.add(fileUploadItem);

        uploadButton = new ButtonItem("Upload");
        uploadButton.setVisible(this.showUploadButton);
        uploadButton.setStartRow(false);
        uploadButton.setEndRow(false);
        uploadButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                submitForm();
            }
        });
        onDrawItems.add(uploadButton);

        icon = new StaticTextItem("icon");
        icon.setStartRow(false);
        icon.setShowTitle(false);
        if (uploadResult != null) {
            if (uploadResult.booleanValue()) {
                icon.setIcons(iconGreen);
                icon.setTooltip("File has already been uploaded");
            } else {
                String message = "File upload has previously failed";
                icon.setIcons(iconRed);
                icon.setTooltip(message);
                setUploadError(message);
            }
        } else {
            icon.setIcons(iconGrey);
            icon.setTooltip("Select a file to upload, then click the 'Upload' button or 'Next'");
        }
        icon.setShowIcons(true);
        onDrawItems.add(icon);

        return onDrawItems;
    }

    protected DynamicFormHandler getSubmitCompleteHandler() {
        return new DynamicFormHandler() {
            public void onSubmitComplete(DynamicFormSubmitCompleteEvent event) {
                uploadInProgress = false;

                String results = event.getResults();
                if (processSubmitCompleteResults(results)) {
                    uploadResult = Boolean.TRUE;
                    icon.setIcons(iconGreen);
                    icon.setTooltip("Uploaded file successfully");
                    CoreGUI.getMessageCenter()
                        .notify(new Message("Uploaded file successfully", results, Severity.Info));
                    icon.hide();
                    icon.show();
                } else {
                    uploadResult = Boolean.FALSE;
                    String cause = "[" + name + "] " + parseCauseFromResponse(results);
                    icon.setIcons(iconRed);
                    icon.setTooltip(cause);
                    setUploadError(cause);
                    icon.setTooltip(cause);
                    CoreGUI.getMessageCenter().notify(
                        new Message("File [" + name + "] upload failed", results, Severity.Error));
                    icon.hide();
                    icon.show();
                }
            }
        };
    }

    protected FormSubmitFailedHandler getSubmitFailedHandler() {
        return new FormSubmitFailedHandler() {
            public void onFormSubmitFailed(FormSubmitFailedEvent event) {
                uploadInProgress = false;

                uploadResult = Boolean.FALSE;
                String cause = "File [" + name + "] upload failed, check for invalid file path.";
                icon.setIcons(iconRed);
                icon.setTooltip(cause);
                setUploadError(cause);
                CoreGUI.getMessageCenter().notify(new Message(cause, Severity.Error));
                icon.hide();
                icon.show();
            }
        };
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        List<FormItem> onDrawItems = getOnDrawItems();
        setNumCols(onDrawItems.size());
        setItems(onDrawItems.toArray(new FormItem[onDrawItems.size()]));

        // push the form handler so it executes first if the form creator has also added a handler
        DynamicFormHandler submitCompleteHandler = getSubmitCompleteHandler();
        if (null != submitCompleteHandler) {
            pushFormHandler(submitCompleteHandler);
        }
        FormSubmitFailedHandler submitFailedHandler = getSubmitFailedHandler();
        if (null != submitFailedHandler) {
            this.addFormSubmitFailedHandler(submitFailedHandler);
        }
    }

    public List<String> getUploadedFilePaths() {
        return uploadedFilePaths;
    }

    protected boolean processSubmitCompleteResults(String submitCompleteEventResults) {
        if (null == uploadedFilePaths) {
            uploadedFilePaths = this.parseFilePathsFromResponse(submitCompleteEventResults);
        }
        return !(null == uploadedFilePaths || uploadedFilePaths.isEmpty());
    }

    private List<String> parseFilePathsFromResponse(String results) {
        List<String> files = null;

        try {
            files = new ArrayList<String>();
            int begin = 0, end;
            while (null != results && -1 != (end = results.indexOf("\n"))) {
                String line = results.substring(begin, end);
                if (!line.endsWith("html>")) {
                    files.add(line);
                }
                begin = end + 1;
            }
        } catch (Exception e) {
            CoreGUI.getErrorHandler().handleError("Unexpected file upload results: " + results, e);
        }

        return files;
    }

    private String parseCauseFromResponse(String results) {
        int i = (null == results) ? -1 : results.indexOf("\tat ");
        String cause = (-1 == i) ? results : results.substring(0, i);
        return cause;
    }

}
