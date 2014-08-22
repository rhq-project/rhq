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
package org.rhq.coregui.client.components.upload;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.types.Encoding;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.form.fields.UploadItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * @author Greg Hinkle
 */
public class TextFileRetrieverForm extends DynamicCallbackForm {

    private ButtonItem uploadButton = null;
    private UploadItem textFile;

    public TextFileRetrieverForm() {
        super("textFileRetriever");
        setNumCols(8);
        setEncoding(Encoding.MULTIPART);
        setAction(GWT.getModuleBaseURL() + "/FileUploadServlet");
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        // the "retrieve" indicates to FileUploadServlet to expect a single text file and return its contents
        // in the response.
        HiddenItem retrieveField = new HiddenItem("retrieve");
        retrieveField.setDefaultValue(true);

        HiddenItem sessionIdField = new HiddenItem("sessionid");
        sessionIdField.setDefaultValue(UserSessionManager.getSessionSubject().getSessionId().intValue());

        setNumCols(4);

        textFile = new UploadItem("textFile", MSG.view_upload_prompt_2());
        textFile.setColSpan(1);
        textFile.setEndRow(false);
        textFile.setWrapTitle(false);

        uploadButton = new ButtonItem(MSG.view_upload_upload());
        uploadButton.setIcon(ImageManager.getUploadIcon());
        uploadButton.setStartRow(false);

        uploadButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                String selectedFile = textFile.getValueAsString();
                if (selectedFile != null && selectedFile.length() > 0) {
                    uploadButton.setShowIcons(true);
                    markForRedraw();
                    submitForm();
                } else {
                    uploadButton.disable(); // no file selected, it shouldn't be able to be pressed
                    uploadButton.setShowIcons(false);
                }
            }
        });

        FormItemIcon loadingIcon = new FormItemIcon();
        loadingIcon.setSrc("ajax-loader.gif");
        loadingIcon.setWidth(16);
        loadingIcon.setHeight(16);
        uploadButton.setIcons(loadingIcon);
        uploadButton.setShowIcons(false);

        setItems(retrieveField, sessionIdField, textFile, uploadButton);

    }

    /**
     * Call this when the file retrieval finished. <code>true</code> means successful,
     * <code>false</code> means an error occurred.
     * @param ok status
     */
    public void retrievalStatus(boolean ok) {
        if (uploadButton != null) {
            String iconSrc;
            Message msg;

            if (ok) {
                iconSrc = ImageManager.getAvailabilityIcon(Boolean.TRUE);
                msg = new Message(MSG.view_upload_success(), Severity.Info);
            } else {
                iconSrc = ImageManager.getAvailabilityIcon(Boolean.FALSE);
                msg = new Message(MSG.view_upload_error_file(), Severity.Error);
            }

            FormItemIcon loadedIcon = new FormItemIcon();
            loadedIcon.setSrc(iconSrc);
            loadedIcon.setWidth(16);
            loadedIcon.setHeight(16);
            uploadButton.setIcons(loadedIcon);
            uploadButton.setShowIcons(true);
            markForRedraw();

            CoreGUI.getMessageCenter().notify(msg);
        }
    }
}
