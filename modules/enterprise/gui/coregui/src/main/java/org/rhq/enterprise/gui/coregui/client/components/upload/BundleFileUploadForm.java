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
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

public class BundleFileUploadForm extends DynamicCallbackForm {

    private UploadItem bundleUploadItem;
    private ButtonItem uploadButton;
    private StaticTextItem icon;

    private Boolean uploadResults;

    private final BundleVersion bundleVersion;
    private final String name;
    private final boolean showNameLabel;

    public BundleFileUploadForm(BundleVersion bundleVersion, String name, boolean showNameLabel,
        Boolean isAlreadyUploaded) {

        super(name);
        this.bundleVersion = bundleVersion;
        this.name = name;
        this.showNameLabel = showNameLabel;
        this.uploadResults = isAlreadyUploaded; // null if unknown, false if error during previous upload attempt, true if already uploaded before

        setEncoding(Encoding.MULTIPART);
        setAction(GWT.getModuleBaseURL() + "/BundleFileUploadServlet");
    }

    public BundleVersion getBundleVersion() {
        return bundleVersion;
    }

    /**
     * The name of the file that is to be uploaded to the server. The actual client file can
     * be named whatever, but the server will use this name as the name of the bundle file.
     * @return bundle file name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns true if the file was successfully uploaded, false if an error occurred.
     * Returns null if this upload form has not be submitted yet (see {@link #submitForm()}).
     * @return status of the upload request
     */
    public Boolean getUploadResults() {
        return uploadResults;
    }

    @Override
    public void submitForm() {
        icon.setShowIcons(true);
        uploadButton.setDisabled(true);
        markForRedraw();
        super.submitForm();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        HiddenItem sessionIdField = new HiddenItem("sessionid");
        sessionIdField.setValue(CoreGUI.getSessionSubject().getSessionId().intValue());

        HiddenItem bundleVersionIdField = new HiddenItem("bundleVersionId");
        bundleVersionIdField.setValue(this.bundleVersion.getId());

        HiddenItem nameField = new HiddenItem("name");
        nameField.setValue(this.name);

        HiddenItem versionField = new HiddenItem("version");
        versionField.setValue("1.0");

        setNumCols(7);

        bundleUploadItem = new UploadItem("bundleFileUploadItem", name);
        bundleUploadItem.setEndRow(false);
        bundleUploadItem.setShowTitle(showNameLabel);

        uploadButton = new ButtonItem("Upload");
        uploadButton.setStartRow(false);
        uploadButton.setEndRow(false);
        uploadButton.setDisabled(true);
        uploadButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                submitForm();
            }
        });

        icon = new StaticTextItem("icon");
        icon.setStartRow(false);
        icon.setShowTitle(false);

        FormItemIcon loadingIcon = new FormItemIcon();
        loadingIcon.setSrc("ajax-loader.gif");
        loadingIcon.setWidth(16);
        loadingIcon.setHeight(16);
        icon.setIcons(loadingIcon);
        icon.setShowIcons(false);

        bundleUploadItem.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent changeEvent) {
                if (uploadResults != null) {
                    retrievalStatus(uploadResults.booleanValue());
                } else {
                    uploadButton.setDisabled(false);
                    icon.setShowIcons(false);
                }
            }
        });

        if (uploadResults != null) {
            retrievalStatus(uploadResults.booleanValue());
        }

        setItems(sessionIdField, bundleVersionIdField, nameField, versionField, bundleUploadItem, uploadButton, icon);

        addFormHandler(new DynamicFormHandler() {
            public void onSubmitComplete(DynamicFormSubmitCompleteEvent event) {
                String results = event.getResults();
                if (!results.contains("Failed to upload bundle file")) {
                    CoreGUI.getMessageCenter().notify(new Message("Uploaded bundle file successfully", Severity.Info));
                    retrievalStatus(true);
                } else {
                    CoreGUI.getMessageCenter().notify(new Message("Bundle file upload failed", Severity.Error));
                    retrievalStatus(false);
                }
            }
        });

        addFormSubmitFailedHandler(new FormSubmitFailedHandler() {
            public void onFormSubmitFailed(FormSubmitFailedEvent event) {
                CoreGUI.getMessageCenter().notify(new Message("Bundle file upload failed", Severity.Error));
                retrievalStatus(false);
            }
        });
    }

    /**
     * Call this when the last file retrieval status is known. <code>true</code> means successful,
     * <code>false</code> means an error occurred.
     * @param ok status
     */
    private void retrievalStatus(boolean ok) {
        uploadResults = Boolean.valueOf(ok);

        if (uploadButton != null) {
            FormItemIcon loadedIcon = new FormItemIcon();
            if (ok) {
                loadedIcon.setSrc("/images/icons/availability_green_16.png");
            } else {
                loadedIcon.setSrc("/images/icons/availability_red_16.png");
            }
            loadedIcon.setWidth(16);
            loadedIcon.setHeight(16);
            icon.setIcons(loadedIcon);
            icon.setShowIcons(true);
            uploadButton.setDisabled(uploadResults);
        }
    }
}
