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
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.form.fields.UploadItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;

public class BundleFileUploadForm extends DynamicCallbackForm {

    private BundleVersion bundleVersion;
    private String name;

    public BundleFileUploadForm(BundleVersion bundleVersion, String name) {
        super("bundleFileUpload");
        this.bundleVersion = bundleVersion;
        this.name = name;

        setNumCols(8);
        setEncoding(Encoding.MULTIPART);
        setAction(GWT.getModuleBaseURL() + "/BundleFileUploadServlet");
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

        final UploadItem bundleUploadItem = new UploadItem("bundleFile", "Upload File");
        bundleUploadItem.setEndRow(false);

        final ButtonItem uploadButton = new ButtonItem("Upload");
        uploadButton.setDisabled(true);

        bundleUploadItem.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent changeEvent) {
                uploadButton.setDisabled(false);
            }
        });

        uploadButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                uploadButton.setShowIcons(true);
                markForRedraw();
                submitForm();
            }
        });

        FormItemIcon loadingIcon = new FormItemIcon();
        loadingIcon.setSrc("ajax-loader.gif");
        loadingIcon.setWidth(16);
        loadingIcon.setHeight(16);
        uploadButton.setIcons(loadingIcon);
        uploadButton.setShowIcons(false);

        setItems(sessionIdField, bundleVersionIdField, nameField, bundleUploadItem, uploadButton);
    }
}
