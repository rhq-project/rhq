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
import com.smartgwt.client.widgets.form.fields.UploadItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
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

    private Boolean uploadResult;
    private String uploadError;
    private boolean uploadInProgress;

    private String name;
    private String version;
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
        iconGreen.setSrc(ImageManager.getAvailabilityIcon(Boolean.TRUE));
        iconGreen.setWidth(16);
        iconGreen.setHeight(16);

        iconRed = new FormItemIcon();
        iconRed.setSrc(ImageManager.getAvailabilityIcon(Boolean.FALSE));
        iconRed.setWidth(16);
        iconRed.setHeight(16);

        iconGrey = new FormItemIcon();
        iconGrey.setSrc(ImageManager.getAvailabilityIcon(null));
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

    public void setName(String name) {
        this.name = name;
        onDraw();
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
        onDraw();
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
            String message = MSG.view_upload_inProgress();
            setUploadError(message);
            return;
        }

        Object value = fileUploadItem.getValue();
        if (value == null || value.toString().length() == 0) {
            String message = MSG.view_upload_prompt_1(name);
            changeIcon(iconRed, message);
            setUploadError(message);
            // note - don't even submit this definite failure            
        } else {
            changeIcon(iconLoading, MSG.common_msg_loading());
            uploadInProgress = true;
            super.submitForm();
        }
    }

    protected void changeIcon(FormItemIcon icon, String tooltip) {
        FormItem item = (this.showUploadButton) ? this.uploadButton : this.fileUploadItem;
        item.setIcons(icon);
        item.setIconPrompt(tooltip);
        markForRedraw();
    }

    protected List<FormItem> getOnDrawItems() {
        List<FormItem> onDrawItems = new ArrayList<FormItem>();

        HiddenItem sessionIdField = new HiddenItem("sessionid");
        sessionIdField.setDefaultValue(UserSessionManager.getSessionSubject().getSessionId().intValue());
        onDrawItems.add(sessionIdField);

        HiddenItem nameField = new HiddenItem("name");
        nameField.setDefaultValue(this.name);
        onDrawItems.add(nameField);

        HiddenItem versionField = new HiddenItem("version");
        versionField.setDefaultValue(version);
        onDrawItems.add(versionField);

        fileUploadItem = new UploadItem("fileUploadItem", name);
        fileUploadItem.setShowTitle(showNameLabel);
        fileUploadItem.setWrapTitle(false);
        fileUploadItem.setColSpan(1);
        onDrawItems.add(fileUploadItem);

        if (showUploadButton) {
            // this intercepts an enable request and only allows it if there is a file selected
            class EnableInterceptingButtonItem extends ButtonItem {
                EnableInterceptingButtonItem(String name) {
                    super(name);
                }

                @Override
                public void enable() {
                    String selectedFile = fileUploadItem.getValueAsString();
                    if (selectedFile != null && selectedFile.length() > 0) {
                        super.enable();
                    }
                }
            }

            uploadButton = new EnableInterceptingButtonItem(MSG.view_upload_upload());
            uploadButton.setDisabled(true);
            uploadButton.setColSpan(1);
            uploadButton.setStartRow(false);
            uploadButton.setAutoFit(true);
            uploadButton.setIcon(ImageManager.getUploadIcon());
            uploadButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    submitForm();
                }
            });

            fileUploadItem.setEndRow(false);
            fileUploadItem.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent changeEvent) {
                    uploadButton.setDisabled(false);
                }
            });

            onDrawItems.add(uploadButton);
        }

        if (uploadResult != null) {
            if (uploadResult.booleanValue()) {
                changeIcon(iconGreen, MSG.view_upload_alreadyUploaded());
            } else {
                String message = MSG.view_upload_tooltip_2();
                changeIcon(iconRed, message);
                setUploadError(message);
            }
        } else {
            changeIcon(iconGrey, (showUploadButton) ? MSG.view_upload_tooltip_1a() : MSG.view_upload_tooltip_1b());
        }

        return onDrawItems;
    }

    protected DynamicFormHandler getSubmitCompleteHandler() {
        return new DynamicFormHandler() {
            public void onSubmitComplete(DynamicFormSubmitCompleteEvent event) {
                uploadInProgress = false;

                String results = event.getResults();
                if (processSubmitCompleteResults(results)) {
                    uploadResult = Boolean.TRUE;
                    changeIcon(iconGreen, MSG.view_upload_success());
                    CoreGUI.getMessageCenter().notify(new Message(MSG.view_upload_success(), results, Severity.Info));
                } else {
                    uploadResult = Boolean.FALSE;
                    String cause = "[" + name + "] " + parseCauseFromResponse(results);
                    changeIcon(iconRed, cause);
                    setUploadError(cause);
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_upload_error_fileName(name), results, Severity.Error));
                }
            }
        };
    }

    protected FormSubmitFailedHandler getSubmitFailedHandler() {
        return new FormSubmitFailedHandler() {
            public void onFormSubmitFailed(FormSubmitFailedEvent event) {
                uploadInProgress = false;

                uploadResult = Boolean.FALSE;
                String cause = MSG.view_upload_error_fileName_2(name);
                changeIcon(iconRed, cause);
                setUploadError(cause);
                CoreGUI.getMessageCenter().notify(new Message(cause, Severity.Error));
            }
        };
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        //set the number of columns before we create the items so that
        //they have this value available
        setNumCols(((this.showUploadButton) ? 2 : 1) + ((this.showNameLabel) ? 1 : 0));
        List<FormItem> onDrawItems = getOnDrawItems();
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
            CoreGUI.getErrorHandler().handleError(MSG.view_upload_error_results(results), e);
        }

        return files;
    }

    private String parseCauseFromResponse(String results) {
        int i = (null == results) ? -1 : results.indexOf("\tat ");
        String cause = (-1 == i) ? results : results.substring(0, i);
        return cause;
    }

}
