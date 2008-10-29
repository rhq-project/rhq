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
package org.rhq.enterprise.agent.update;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Compares two files, an old one and a new one and if they differ, one will win and be kept as-is
 * in the new file's parent directory while the other one will get copied under a new name in
 * the new file's parent directory as a backup copy.
 * If the two files are identical, this task is a no-op and does nothing.
 * If backupextension is not specified, the file that is not to be kept will not be backed up.
 * 
 * <copy-with-backup olddir="old" newdir="new" filename="file" keep="new" backupextension=".default" />
 * 
 * That will compare old/file to new/file and if they are different, the new one will be kept
 * as is in directory new and the old one will be copied into directory
 * 'new' but with a name of "file" appended with the backup extension ".default".
 * 
 * The "old" file is always copied to the directory where the "new" file is located; the "keep" attribute
 * determines which file gets renamed with the backup extension and which keeps its file name intact.
 * 
 * This allows us to tell which customized files from an old install should override the new install's
 * default files and which of the new default files should override any previous customizations made
 * to an old install.
 * 
 * This loads in the content of both files to generate the MD5 - do not use this task on large binary
 * files. If we need to do this kind of task on files other than small text files, we need to refactor
 * the way the MD5's are calculated.
 *
 * @author John Mazzitelli
 */
public class CopyWithBackup extends Task {
    private File oldDirectory;
    private File newDirectory;
    private String filename;
    private String keep; // must be either "old" or "new"
    private String backupextension;
    private Boolean failonerror = Boolean.FALSE;

    public void setOlddir(File dir) {
        this.oldDirectory = dir;
    }

    public void setNewdir(File dir) {
        this.newDirectory = dir;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setKeep(String keep) {
        this.keep = keep;
    }

    public void setBackupextension(String ext) {
        this.backupextension = ext;
    }

    public void setFailonerror(Boolean flag) {
        this.failonerror = flag;
    }

    /**
     * @see org.apache.tools.ant.Task#execute()
     */
    @Override
    public void execute() throws BuildException {
        validateAttributes();

        try {
            MessageDigest messageDigest;

            try {
                messageDigest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new BuildException(e); // should never occur; this would be bad - JRE has this builtin
            }

            File oldFile = new File(oldDirectory, filename);
            File newFile = new File(newDirectory, filename);

            log("* old file=" + oldFile, Project.MSG_DEBUG);
            log("* old file exists=" + oldFile.exists(), Project.MSG_DEBUG);
            log("* new file=" + newFile, Project.MSG_DEBUG);
            log("* new file exists=" + newFile.exists(), Project.MSG_DEBUG);
            log("* backup extension=" + backupextension, Project.MSG_DEBUG);
            log("* keep=" + keep, Project.MSG_DEBUG);

            // handle the special cases when one or both do not exist
            if (!oldFile.exists()) {
                if (!newFile.exists()) {
                    log("old file and new file do not exist - nothing to be done");
                    return;
                }
                if (keep.equals("new")) {
                    log("old file does not exist, new file does, keeping new file as-is");
                } else {
                    if (backupextension == null) {
                        log("old file does not exist, new file does, removing new file");
                        if (!newFile.delete()) {
                            throw new BuildException("Cannot remove new file [" + newFile + "]");
                        }
                    } else {
                        log("old file does not exist, new file does, backing up new file");
                        if (!newFile.renameTo(new File(newDirectory, filename + backupextension))) {
                            throw new BuildException("Cannot backup new file [" + newFile + "]");
                        }
                    }
                }
            } else if (!newFile.exists()) {
                if (keep.equals("new")) {
                    if (backupextension == null) {
                        log("old file exists, new file does not, do nothing");
                    } else {
                        log("old file exists, new file does not, backup old");
                        copy(oldFile, new File(newDirectory, filename + backupextension));
                    }
                } else {
                    log("old file exists, new file does not, copying old file");
                    copy(oldFile, newFile); // doesn't matter what backupextension is, there is no new file to care
                }
            } else {
                // both files exist - we need to compare them and do our thing if they are different
                byte[] oldMD5Bytes = messageDigest.digest(slurp(oldFile));
                messageDigest.reset();
                byte[] newMD5Bytes = messageDigest.digest(slurp(newFile));

                if (MessageDigest.isEqual(oldMD5Bytes, newMD5Bytes)) {
                    log("old file and new file are the same - nothing to be done");
                    return;
                }

                if (keep.equals("new")) {
                    if (backupextension == null) {
                        log("files differ, keeping new, not backing up old");
                    } else {
                        log("files differ, keeping new, backing up old");
                        copy(oldFile, new File(newDirectory, filename + backupextension));
                    }
                } else {
                    if (backupextension == null) {
                        log("files differ, keeping old, not backing up new");
                        newFile.delete();
                        copy(oldFile, newFile);
                    } else {
                        log("files differ, keeping old, backing up new");
                        newFile.renameTo(new File(newDirectory, filename + backupextension));
                        copy(oldFile, newFile);
                    }
                }
            }
        } catch (BuildException e) {
            if (failonerror.booleanValue()) {
                throw e;
            } else {
                log("got a failure but will not exit: " + e);
            }
        }
        return;
    }

    private byte[] slurp(File file) throws BuildException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            copy(new FileInputStream(file), out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BuildException("Stream data cannot be slurped", e);
        }
    }

    private void copy(File src, File dest) throws BuildException {
        if (dest.exists()) {
            throw new BuildException("Cannot copy [" + src + "] to [" + dest + "] because the latter exists");
        }
        try {
            copy(new FileInputStream(src), new FileOutputStream(dest));
        } catch (Exception e) {
            throw new BuildException("Cannot copy [" + src + "] to [" + dest + "]", e);
        }
    }

    private long copy(InputStream input, OutputStream output) throws RuntimeException {
        long numBytesCopied = 0;
        int bufferSize = 32768;
        try {
            // make sure we buffer the input
            input = new BufferedInputStream(input, bufferSize);
            byte[] buffer = new byte[bufferSize];
            for (int bytesRead = input.read(buffer); bytesRead != -1; bytesRead = input.read(buffer)) {
                output.write(buffer, 0, bytesRead);
                numBytesCopied += bytesRead;
            }
            output.flush();
        } catch (Exception e) {
            throw new BuildException("Stream data cannot be copied", e);
        } finally {
            try {
                output.close();
            } catch (IOException ioe) {
            }
            try {
                input.close();
            } catch (IOException ioe) {
            }
        }
        return numBytesCopied;
    }

    private void validateAttributes() throws BuildException {
        if (filename == null) {
            throw new BuildException("Must specify 'filename'");
        }

        if (oldDirectory == null) {
            throw new BuildException("Must specify 'olddir' directory");
        }

        if (newDirectory == null) {
            throw new BuildException("Must specify 'newdir' directory");
        } else if (!newDirectory.exists()) {
            throw new BuildException("'newdir' directory must exist: " + newDirectory);
        } else if (!newDirectory.isDirectory()) {
            throw new BuildException("'newdir' must be a directory: " + newDirectory);
        }

        if (keep == null) {
            throw new BuildException("Must specify 'keep' as either 'old' or 'new'");
        } else if (!"old".equals(keep) && !"new".equals(keep)) {
            throw new BuildException("'keep' must be one of: [old, new]");
        }

        // an empty extension is the same as not specifying it at all
        if (backupextension != null && backupextension.trim().length() == 0) {
            backupextension = null;
        }
    }
}