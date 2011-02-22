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

import org.rhq.enterprise.gui.coregui.client.CoreGUI;

/**
 * Upload a single file and use it to create a new PackageVersion. 
 * 
 * @author Jay Shaughnessy
 * @author Lukas Krejci
 */
public class PackageVersionFileUploadForm extends FileUploadForm {

    private int packageTypeId;
    private Integer archId;
    private Integer repoId;
    private int packageVersionId;
    private int packageId;
    
    public PackageVersionFileUploadForm(String locatorId, int packageTypeId, String packageName, String version,
        Integer archId, Integer repoId, boolean showNameLabel, boolean showUploadButton, Boolean isAlreadyUploaded) {

        super(locatorId, packageName, version, showNameLabel, showUploadButton, isAlreadyUploaded);

        this.packageTypeId = packageTypeId;
        this.archId = archId;
        this.repoId = repoId;
        
        setAction(GWT.getModuleBaseURL() + "PackageVersionFileUploadServlet");
    }

    /**
     * If this component successfully uploaded a package version file, this will return
     * the new package version's ID. Otherwise, 0 is returned.
     * 
     * @return the new package version ID
     */
    public int getPackageVersionId() {
        return this.packageVersionId;
    }

    /**
     * If this component successfully upload a package version file, this will return
     * the id of the package the uploaded package version was associated with.
     * Otherwise, 0 is returned.
     * 
     * @return the package id of the new package version
     */
    public int getPackageId() {
        return packageId;
    }
    
    public int getPackageTypeId() {
        return packageTypeId;
    }
    
    public void setPackageTypeId(int value) {
        packageTypeId = value;
        onDraw();
    }
    
    public Integer getArchitectureId() {
        return archId;
    }
    
    public void setArchitectureId(Integer value) {
        archId = value; 
        onDraw();
    }
    
    public Integer getRepoId() {
        return repoId;
    }
    
    public void setRepoId(Integer value) {
        repoId = value;
        onDraw();
    }
    
    @Override
    protected List<FormItem> getOnDrawItems() {
        List<FormItem> onDrawItems = super.getOnDrawItems();

        HiddenItem packageTypeIdField = new HiddenItem("packageTypeId");
        packageTypeIdField.setDefaultValue(packageTypeId);
        onDrawItems.add(packageTypeIdField);

        if (null != archId) {
            HiddenItem archIdField = new HiddenItem("archId");
            archIdField.setDefaultValue(archId);
            onDrawItems.add(archIdField);
        }

        if (null != repoId) {
            HiddenItem repoIdField = new HiddenItem("repoId");
            repoIdField.setDefaultValue(repoId);
            onDrawItems.add(repoIdField);
        }
        
        return onDrawItems;
    }

    protected boolean processSubmitCompleteResults(String submitCompleteEventResults) {
        parseIdsFromResponse(submitCompleteEventResults);
        return (packageVersionId > 0);
    }

    private void parseIdsFromResponse(String results) {
        packageVersionId = 0;
        packageId = 0;
        
        // the upload servlet will respond with "success [packageVersionId=x,packageId=y]" on success
        
        String successMsgPrefix = "success ["; 
        int startSuccessMsgPrefix = results.indexOf(successMsgPrefix);
        if (startSuccessMsgPrefix < 0) {
            CoreGUI.getErrorHandler().handleError(MSG.view_upload_error_packageVersionFile());
            return; // must mean it wasn't a success - results is probably an error message
        }
        int endSuccessMsgPrefix = startSuccessMsgPrefix + successMsgPrefix.length();
        int startSuccessMsgPostfix = results.indexOf(']', endSuccessMsgPrefix);
        if (startSuccessMsgPostfix < 0) {
            CoreGUI.getErrorHandler().handleError(MSG.view_upload_error_packageVersionFile());
            return; // this should never happen, if we have "success [" we should always have the ending "]" bracket
        }
        
        String[] ids = results.substring(endSuccessMsgPrefix, startSuccessMsgPostfix).split(",");
        if (ids.length != 2) {
            CoreGUI.getErrorHandler().handleError(MSG.view_upload_error_packageVersionFile());
            return;
        }
        
        String packageVersionIdString = ids[0];
        String packageIdString = ids[1];
        
        try {
            int equalsIdx = packageVersionIdString.indexOf('=');
            if (equalsIdx < 0) {
                CoreGUI.getErrorHandler().handleError(MSG.view_upload_error_packageVersionFile());
                return;
            }
            packageVersionId = Integer.parseInt(packageVersionIdString.substring(equalsIdx + 1));
            
            equalsIdx = packageIdString.indexOf('=');
            if (equalsIdx < 0) {
                packageVersionId = 0;
                CoreGUI.getErrorHandler().handleError(MSG.view_upload_error_packageVersionFile());
                return;
            }
            packageId = Integer.parseInt(packageIdString.substring(equalsIdx + 1));
        } catch (Exception e) {
            CoreGUI.getErrorHandler().handleError(MSG.view_upload_error_packageVersionFile(), e);
        }
    }

    @Override
    public void submitForm() {

        // reset the name value from the package type name to the path of the actual uploaded file. The
        // name portion will be parsed out and used for the package name.
        FormItem nameItem = getItem("name");
        FormItem fileUploadItem = getItem("fileUploadItem");
        String uploadFilePath = (String) fileUploadItem.getValue();
        nameItem.setValue(uploadFilePath);

        super.submitForm();
    }
}
