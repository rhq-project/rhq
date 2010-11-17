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

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HiddenItem;

import org.rhq.core.domain.bundle.BundleVersion;

public class BundleFileUploadForm extends FileUploadForm {

    private final BundleVersion bundleVersion;

    public BundleFileUploadForm(String locatorId, BundleVersion bundleVersion, String name, boolean showNameLabel,
        Boolean isAlreadyUploaded) {

        super(locatorId, name, bundleVersion.getVersion(), showNameLabel, true, isAlreadyUploaded);
        this.bundleVersion = bundleVersion;

        setAction(GWT.getModuleBaseURL() + "BundleFileUploadServlet");
    }

    public BundleVersion getBundleVersion() {
        return bundleVersion;
    }

    @Override
    protected List<FormItem> getOnDrawItems() {
        List<FormItem> onDrawItems = super.getOnDrawItems();

        HiddenItem bundleVersionIdField = new HiddenItem("bundleVersionId");
        bundleVersionIdField.setValue(this.bundleVersion.getId());

        onDrawItems.add(bundleVersionIdField);
        return onDrawItems;
    }

    protected boolean processSubmitCompleteResults(String submitCompleteEventResults) {
        return !submitCompleteEventResults.contains(MSG.view_upload_error_file());
    }

}
