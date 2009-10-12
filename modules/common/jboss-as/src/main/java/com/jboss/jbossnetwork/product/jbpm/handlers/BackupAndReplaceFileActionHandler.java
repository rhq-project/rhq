 /*
  * Jopr Management Platform
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
package com.jboss.jbossnetwork.product.jbpm.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * JBPM process handler that is responsible for copying new files from a patch into the AS instance. The previous file
 * will be renamed with a timestamp and .old extension to facilitate rolling back the patch.
 *
 * @author Jason Dobies
 */
public class BackupAndReplaceFileActionHandler extends BaseHandler {
    private static final String TRANSITION_ORIGINAL_FILE_NOT_FOUND = "originalFileNotFound";

    /**
     * The location of the new file, which will be copied over the original file.
     */
    private String replacementFileLocation;

    /**
     * The location of the file which will be replaced.
     */
    private String originalFileLocation;

    /**
     * The location the original file will be backed up to. If this is not specified, we will generate a value for this
     * of the format: originalFileLocation + ".#{timestamp}.old".
     */
    private String backupFileLocation;

    /**
     * The location that the replacement file will be written to. This enables us to support renaming the file as it is
     * replaced (one such use case would be a new version of a JAR being pushed out). See JBNADM-618.
     */
    private String destinationFileLocation;

    public void run(ExecutionContext executionContext) {
        try {
            // Verify we can work on the original file
            try {
                HandlerUtils.checkFilenameExists(originalFileLocation);
            } catch (ActionHandlerException e) {
                skip(executionContext, e, "File not replaced, no changes were made in this step.",
                    TRANSITION_ORIGINAL_FILE_NOT_FOUND);
                return;
            }

            HandlerUtils.checkFilenameIsAFile(originalFileLocation);
            HandlerUtils.checkFilenameIsWriteable(originalFileLocation);

            // Verify we can backup the old file
            HandlerUtils.checkFilenameDoesNotExist(backupFileLocation);

            // Verify the new file to use is ok
            HandlerUtils.checkFilenameExists(replacementFileLocation);
            HandlerUtils.checkFilenameIsAFile(replacementFileLocation);
            HandlerUtils.checkFilenameIsWriteable(replacementFileLocation);

            // Support necessary for a file name change in the same step
            updateDestinationFileLocation();

            // If we get this far, all of the verifications have passed, so do the rename
            rename(originalFileLocation, backupFileLocation);
        } catch (Throwable e) {
            error(executionContext, e, getErrorDescription() + " " + MESSAGE_NO_CHANGES, TRANSITION_ERROR);
            return;
        }

        try {
            rename(replacementFileLocation, destinationFileLocation);
        } catch (Throwable e) {
            error(executionContext, e, getErrorDescription() + " Changes were made in this step. "
                + "To return to the previous state you should rename ["
                + HandlerUtils.formatPath(getBackupFileLocation()) + "] to be ["
                + HandlerUtils.formatPath(getOriginalFileLocation()) + "].", TRANSITION_ERROR);
            return;
        }

        complete(executionContext, "Successfully replaced [" + HandlerUtils.formatPath(getOriginalFileLocation())
            + "]. Original file was backed up to [" + HandlerUtils.formatPath(getBackupFileLocation()) + "].");
    }

    public String getDescription() {
        return "Backup and replace [" + HandlerUtils.formatPath(originalFileLocation) + "].";
    }

    public void substituteVariables(ExecutionContext executionContext) throws ActionHandlerException {
        setReplacementFileLocation(substituteVariable(replacementFileLocation, executionContext));
        setOriginalFileLocation(substituteVariable(originalFileLocation, executionContext));
        setBackupFileLocation(substituteVariable(backupFileLocation, executionContext));
        setDestinationFileLocation(substituteVariable(destinationFileLocation, executionContext));
    }

    protected void checkProperties() throws ActionHandlerException {
        HandlerUtils.checkIsSet("originalFileLocation", originalFileLocation);
        HandlerUtils.checkIsSet("replacementFileLocation", replacementFileLocation);
        HandlerUtils.checkIsSet("backupFileLocation", backupFileLocation);
        HandlerUtils.checkIsSet("destinationFileLocation", destinationFileLocation);
    }

    public void setPropertyDefaults() {
        if (getBackupFileLocation() == null) {
            setBackupFileLocation(getOriginalFileLocation() + ".#{timestamp}.old");
        }

        if (getDestinationFileLocation() == null) {
            setDestinationFileLocation(getOriginalFileLocation());
        }
    }

    /**
     * If the file is being renamed as it is being replaced, this method will detect and set the attributes to support
     * it.
     *
     * @throws ActionHandlerException if the original file does not provide enough path information or if the new file
     *                                name generated in this method will overwrite an existing file (unintended).
     */
    private void updateDestinationFileLocation() throws ActionHandlerException {
        File replacementFile = new File(replacementFileLocation);
        String replacementFilename = replacementFile.getName();

        File originalFile = new File(originalFileLocation);
        String originalFilename = originalFile.getName();

        // Checks for the case of the file being renamed. This implementation implies that the files are located in
        // two different directories. This will be the case in practice, as the patch files are unzipped to a
        // temporary location.
        if (!replacementFilename.equalsIgnoreCase(originalFilename)) {
            // This file name can't be relative since we need its parent in the next step
            HandlerUtils.checkFilenameIsAbsolute(originalFileLocation);

            File destinationFile = new File(originalFile.getParentFile().getPath() + File.separator
                + replacementFilename);
            setDestinationFileLocation(destinationFile.getPath());

            // Make sure the newly generated file name isn't going to clash with an existing file
            HandlerUtils.checkFilenameDoesNotExist(destinationFileLocation);
        }
    }

    /**
     * Renames the file indicated in the from attribute to the name in the to attribute.
     *
     * @param  from file being renamed
     * @param  to   new name
     *
     * @throws ActionHandlerException if the rename fails
     * @throws IOException            if the file call to rename fails and we fall back to explicitly using input/output
     *                                streams to perform a copy/delete implementation of rename
     */
    private void rename(String from, String to) throws ActionHandlerException, IOException {
        File fromFile = new File(from);
        File toFile = new File(to);
        if (!fromFile.renameTo(toFile)) {
            // There is an issue with the rename call when the temp directory is on a different file system from
            // the AS instance being patched. The following is to counter that. See JBNADM-927.
            InputStream in = null;
            OutputStream out = null;
            try {
                in = new FileInputStream(from);
                out = new FileOutputStream(to);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                if (toFile.exists()) {
                    fromFile.delete();
                } else {
                    throw new ActionHandlerException("Could not write from [" + HandlerUtils.formatPath(from)
                        + "] to [" + HandlerUtils.formatPath(to) + "].");
                }
            } finally {
                if (in != null) {
                    in.close();
                }

                if (out != null) {
                    out.close();
                }
            }
        }
    }

    private String getErrorDescription() {
        String desc = "Trying to back-up [" + HandlerUtils.formatPath(getOriginalFileLocation()) + "] to ["
            + HandlerUtils.formatPath(getBackupFileLocation()) + "], then replace it with file from ["
            + HandlerUtils.formatPath(getReplacementFileLocation()) + "]";

        File replacementFile = new File(replacementFileLocation);
        File originalFile = new File(originalFileLocation);

        if (!replacementFile.getName().equalsIgnoreCase(originalFile.getName())) {
            desc = desc + ", maintaining the replacement filename.";
        } else {
            desc = desc + ".";
        }

        return desc;
    }

    public String getReplacementFileLocation() {
        return replacementFileLocation;
    }

    public void setReplacementFileLocation(String replacementFileLocation) {
        this.replacementFileLocation = replacementFileLocation;
    }

    public String getOriginalFileLocation() {
        return originalFileLocation;
    }

    public void setOriginalFileLocation(String originalFileLocation) {
        this.originalFileLocation = originalFileLocation;
    }

    public String getBackupFileLocation() {
        return backupFileLocation;
    }

    public void setBackupFileLocation(String backupFileLocation) {
        this.backupFileLocation = backupFileLocation;
    }

    public String getDestinationFileLocation() {
        return destinationFileLocation;
    }

    public void setDestinationFileLocation(String destinationFileLocation) {
        this.destinationFileLocation = destinationFileLocation;
    }
}