/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.common.upload;

import java.io.File;
import java.io.FileNotFoundException;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.file.FileUtil;

/**
 * Session bean used to maintain information about an uploaded file.
 * For use with RichFaces fileUpload component.
 *
 * @author John Mazzitelli
 */
public class FileUploadUIBean {

    private final Log log = LogFactory.getLog(FileUploadUIBean.class);

    private UploadItem fileItem;

    public UploadItem getFileItem() {
        return this.fileItem;
    }

    public void setFileItem(UploadItem fileItem) {
        this.fileItem = fileItem;
    }

    public boolean isFileUploaded() {
        return getFileItem() != null;
    }

    /**
     * Deletes any temporary file containing a previously uploaded file (if one exists).
     * This {@link #setFileItem(UploadItem) sets the file to null} so it appears no file
     * has been uploaded.
     */
    public void clear() {
        try {
            if (isFileUploaded()) {
                getFileItem().getFile().delete();
            }
        } catch (Exception ignore) {
        } finally {
            setFileItem(null);
        }
    }

    public void fileUploadListener(UploadEvent event) {
        try {
            clear(); // clean up any old file that was previously uploaded, we can only handle one at a time
            setFileItem(event.getUploadItem());

            File uploadedFile = getFileItem().getFile();

            if (uploadedFile == null || !uploadedFile.exists()) {
                throw new FileNotFoundException("The uploaded file [" + uploadedFile + "] does not exist!");
            }

            String uploadedFilename = getFileItem().getFileName(); // careful, IE and Chrome passes in the full absolute path here
            uploadedFilename = FileUtil.getFileName(FileUtil.useForwardSlash(uploadedFilename), "/"); // strip path, get just filename
            long uploadedFileSize = uploadedFile.length();
            log.info("A file named [" + uploadedFilename + "] with a size of [" + uploadedFileSize
                + "] has been uploaded to [" + uploadedFile + "]");

            onSuccess();
        } catch (Throwable t) {
            onFailure(t, event.getUploadItem().getFileSize());
        }

        return;
    }

    @Override
    public String toString() {
        String fileInfo = "no file has been uploaded yet";
        if (this.fileItem != null) {
            fileInfo = "fileName=" + this.fileItem.getFileName() + ", size=" + this.fileItem.getFileSize() + ", file="
                + this.fileItem.getFile();
        }
        return this.getClass().getSimpleName() + ": " + fileInfo;
    }

    /**
     * Adds a generic info message to the faces context.
     * If you want to customize this, write a subclass and override this method.
     */
    protected void onSuccess() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Content has been received");
    }

    /**
     * Adds a generic error message to the faces context and logs the error.
     * If you want to customize this, write a subclass and override this method.
     * 
     * @param t the error that occurred
     */
    protected void onFailure(Throwable t, int fileSize) {
        String msgPattern = "Failed to process uploaded file. Cause: %s";
        if(fileSize > 250000000) {
            msgPattern = "Failed to process uploaded file. The file size was larger than the allowed maximum of 250MB. Cause: %s";
        }
        String msg = String.format(msgPattern, ThrowableUtil.getAllMessages(t));
        log.error(msg);
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, msg, t);
    }
}