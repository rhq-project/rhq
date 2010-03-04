/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.enterprise.gui.configuration.resource;

import org.apache.commons.io.FileUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.rhq.enterprise.gui.common.upload.FileUploadUIBean;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

import java.io.IOException;

/**
 * <p>
 * A Seam component that handles file uploads for RichFace's file upload component. This class is a thin wrapper around
 * {@link org.rhq.enterprise.gui.common.upload.FileUploadUIBean} which is not a Seam component.
 * </p>
 * <p>
 * Currently this class is tightly coupled to {@link org.rhq.enterprise.gui.configuration.resource.RawConfigUIBean}
 * which is specific to the raw config editor; however, this class can/should/will be generalized to support any file
 * upload which is implemented using RichFaces and Seam together. 
 * </p>
 */
@Name("fileUploader")
@Scope(ScopeType.PAGE)
public class FileUploadComponent {

    private FileUploadUIBean uploadBean = new FileUploadUIBean();

    /**
     * Represents the currently selected file in the raw config editor
     */
    @In
    private RawConfigUIBean selectedRawUIBean;

    public UploadItem getUploadItem() {
        return uploadBean.getFileItem();
    }

    public void setUploadItem(UploadItem uploadItem) {
        uploadBean.setFileItem(uploadItem);
    }

    /**
     * This is the upload listener that handles consuming the uploaded file.
     *
     * @param event
     * @throws Exception
     */
    public void listener(UploadEvent event) throws Exception {
        uploadBean.fileUploadListener(event);
    }

    public void clear() {
        uploadBean.clear();
    }

    /**
     * Invoked when the upload has completed and updates {@link #selectedRawUIBean} with the contents of the uploaded
     * file.
     */
    public void completeUpload() {
        try {
            if (uploadBean.getFileItem() != null) {
                UploadItem fileItem = uploadBean.getFileItem();
                if (fileItem.isTempFile()) {
                    selectedRawUIBean.setContents(FileUtils.readFileToString(fileItem.getFile()));
                } else {
                    selectedRawUIBean.setContents(new String(fileItem.getData()));
                }
                uploadBean.clear();
            }
        } 
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
