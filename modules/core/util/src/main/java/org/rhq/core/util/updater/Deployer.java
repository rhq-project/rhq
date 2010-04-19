/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.util.updater;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.template.TemplateEngine;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * This deploys a bundle of files within a zip archive to a managed directory.
 * 
 * This follows rpm-like rules when updating a deployment and files already exist
 * in previous deployments that need to be re-installed in an updated deployment.
 * The rules are as follows, where the first three columns represent a file's
 * hashcode:
 * <table>
 * <tr><th>ORIGINAL</th><th>CURRENT</th><th>NEW</th><th>What To Do...</th></tr>
 * <tr><td>X</td><td>X</td><td>X</td><td>New file is installed over current*</td></tr>
 * <tr><td>X</td><td>X</td><td>Y</td><td>New file is installed over current</td></tr>
 * <tr><td>X</td><td>Y</td><td>X</td><td>Current file is left as-is</td></tr>
 * <tr><td>X</td><td>Y</td><td>Y</td><td>New file is installed over current*</td></tr>
 * <tr><td>X</td><td>Y</td><td>Z</td><td>New file is installed over current, current is backed up</td></tr>
 * <tr><td>none</td><td>?</td><td>?</td><td>New file is installed over current, current is backed up</td></tr>
 * <tr><td>X</td><td>none</td><td>?</td><td>New file is installed</td></tr>
 * <tr><td>?</td><td>?</td><td>none</td><td>Current file is deleted</td></tr>
 * </table>
 * (*) denotes that the current file could have been left as-is. If, in the future, we can
 * provide Java with a way to change file permissions or ownership, we would want to
 * copy the new file over the current file and perform the chown/chmod.
 * 
 * Here you can see that there is only one non-trivial case where the new file is <b>not</b> installed, and that
 * is when the original file and the new file have the same hashcode (i.e. was not changed) but
 * the current version of that file was changed (in the trivial case, where there is no new file, nothing is
 * installed and the current file is uninstalled). In this case, since the original file and the new
 * file are identical, the file with the modifications made to the original version (called the "current" version)
 * is presumed to still be valid for the new version, thus we leave the current, modified file in place.
 * 
 * In the case where there is ambiguity as to what the right thing to do is, the current file is
 * backed up prior to being overwritten with the new file. This can occur in two cases: the first
 * is when the current file is a modified version of the original and the new file is different
 * from the original; the second is when there was no original file, but there is now a current
 * file on disk and a new file to be installed. In either case, the current file is backed up
 * before the new file is copied over top the current file. Note in the files to be
 * backed up will be copied with the {@link #BACKUP_EXTENSION} extension added to the end
 * of the backup file's filename. 
 * 
 * @author John Mazzitelli
 */
public class Deployer {
    private final Log log = LogFactory.getLog(Deployer.class);

    private static final String BACKUP_EXTENSION = ".rhqbackup";

    private final DeploymentProperties deploymentProps;
    private final Set<File> zipFiles;
    private final Map<File, File> rawFiles;
    private final File destDir;
    private final Pattern filesToRealizeRegex;
    private final TemplateEngine templateEngine;
    private final Pattern ignoreRegex;
    private final DeploymentsMetadata deploymentsMetadata;

    /**
     * Constructors that prepares this object to deploy the given archive's content to the destination directory.
     *  
     * @param deploymentProps metadata about this deployment
     * @param zipFiles the archives containing the content to be deployed
     * @param rawFiles files that are to be copied into the destination directory - the keys are the current
     *                 locations of the files, the values are where the files should be copied (the values may be relative
     *                 in which case they are relative to destDir and can have subdirectories and/or a different filename
     *                 than what the file is named currently)
     * @param destDir the root directory where the content is to be deployed
     * @param filesToRealizeRegex the patterns of files (whose paths are relative to destDir) that
     *                            must have replacement variables within them replaced with values
     *                            obtained via the given template engine
     * @param templateEngine if one or more filesToRealize are specified, this template engine is used to determine
     *                       the values that should replace all replacement variables found in those files
     * @param ignoreRegex the files/directories to ignore when updating an existing deployment
     */
    public Deployer(DeploymentProperties deploymentProps, Set<File> zipFiles, Map<File, File> rawFiles, File destDir,
        Pattern filesToRealizeRegex, TemplateEngine templateEngine, Pattern ignoreRegex) {

        if (deploymentProps == null) {
            throw new IllegalArgumentException("deploymentProps == null");
        }
        if (destDir == null) {
            throw new IllegalArgumentException("destDir == null");
        }

        if (zipFiles == null) {
            zipFiles = new HashSet<File>();
        }
        if (rawFiles == null) {
            rawFiles = new HashMap<File, File>();
        }
        if ((zipFiles.size() == 0) && (rawFiles.size() == 0)) {
            throw new IllegalArgumentException("zipFiles/rawFiles are empty - nothing to do");
        }

        this.deploymentProps = deploymentProps;
        this.zipFiles = zipFiles;
        this.rawFiles = rawFiles;
        this.destDir = destDir;
        this.ignoreRegex = ignoreRegex;

        if (filesToRealizeRegex == null || templateEngine == null) {
            // we don't need these if there is nothing to realize or we have no template engine to obtain replacement values
            this.filesToRealizeRegex = null;
            this.templateEngine = null;
        } else {
            this.filesToRealizeRegex = filesToRealizeRegex;
            this.templateEngine = templateEngine;
        }

        this.deploymentsMetadata = new DeploymentsMetadata(destDir);
        return;
    }

    public FileHashcodeMap deploy() throws Exception {
        FileHashcodeMap map = null;

        if (!this.deploymentsMetadata.isManaged()) {
            // the destination dir has not been used to deploy a bundle yet, therefore, this is the first deployment
            map = performInitialDeployment();
        } else {
            // we have metadata in the destination directory, therefore, this deployment is updating a current one
            map = performUpdateDeployment();
        }

        return map;
    }

    private FileHashcodeMap performInitialDeployment() throws Exception {
        FileHashcodeMap newFileHashcodeMap = extractZipAndRawFiles(null);
        debug("Initial deployment finished.");
        return newFileHashcodeMap;
    }

    private FileHashcodeMap performUpdateDeployment() throws Exception {
        debug("Analyzing original, current and new files as part of update deployment");

        FileHashcodeMap original = this.deploymentsMetadata.getCurrentDeploymentFileHashcodes();
        ChangesFileHashcodeMap current = original.rescan(this.destDir, this.ignoreRegex);
        FileHashcodeMap newFiles = getNewDeploymentFileHashcodeMap();

        // backup the current file if either of these are true:
        // * if there is a current file but there was no original file (current.getAdditions())
        // * if the current, original and new files are all different from one another (current.getChanges() compared to new)
        //
        // do nothing (do not backup the current file and do not copy the new file to disk) if:
        // * the original file is not the same as the current file (current.getChanges()) but is the same as the new file
        Set<String> currentFilesToBackup = new HashSet<String>(current.getAdditions().keySet());
        Set<String> currentFilesToLeaveAlone = new HashSet<String>();
        Set<String> currentFilesToDelete;

        for (Map.Entry<String, String> changed : current.getChanges().entrySet()) {
            String changedFilePath = changed.getKey();
            String newHashcode = newFiles.get(changedFilePath);
            if (newHashcode != null) {
                // remember, original hash will never be the same as the changed hash (that's why it is considered changed!)
                if (newHashcode.equals(original.get(changedFilePath))) {
                    currentFilesToLeaveAlone.add(changedFilePath);
                } else if (!newHashcode.equals(changed.getValue())) {
                    currentFilesToBackup.add(changedFilePath);
                }
            }
        }

        original = null; // done with this, allow GC to reclaim its memory

        // now remove all the new files from the current map - what's left are files that exist that are not in the new deployment
        // thus, those files left in current are those that need to be deleted from disk
        currentFilesToDelete = current.keySet(); // the set is backed by the map, changes to it affect the map
        currentFilesToDelete.removeAll(newFiles.keySet());
        currentFilesToDelete.removeAll(current.getDeletions().keySet()); // these are already deleted, no sense trying to delete them again

        // we now know what to do:
        // 1. backup the files in currentFilesToBackup
        // 2. delete the files in currentFilesToDelete
        // 3. copy all new files in newFiles except for those files that are also in currentFilesToLeaveAlone

        // 1. backup the files we want to retain for the admin to review
        debug("Backing up files as part of update deployment");
        for (String fileToBackupPath : currentFilesToBackup) {
            String backupFilePath = fileToBackupPath + BACKUP_EXTENSION;
            File backupFile = new File(backupFilePath);
            File fileToBackup;
            if (!backupFile.isAbsolute()) {
                backupFile = new File(this.destDir, backupFilePath);
                fileToBackup = new File(this.destDir, fileToBackupPath);
            } else {
                fileToBackup = new File(fileToBackupPath);
            }
            FileUtil.copyFile(fileToBackup, backupFile);
        }

        // 2. delete the obsolete files
        debug("Deleting obsolete files as part of update deployment");
        for (String fileToDeletePath : currentFilesToDelete) {
            File doomedFile = new File(fileToDeletePath);
            if (!doomedFile.isAbsolute()) {
                doomedFile = new File(this.destDir, fileToDeletePath);
            }
            boolean deleted = doomedFile.delete();
            // TODO: what should we do if deleted=false? is it a major failure if we can't remove obsolete files?
        }

        // 3. copy all new files except for those to be kept as-is
        debug("Copying new files as part of update deployment");
        FileHashcodeMap newFileHashCodeMap = extractZipAndRawFiles(currentFilesToLeaveAlone);

        debug("Update deployment finished.");
        return newFileHashCodeMap;
    }

    private FileHashcodeMap extractZipAndRawFiles(Set<String> currentFilesToLeaveAlone) throws Exception {
        FileHashcodeMap newFileHashCodeMap = new FileHashcodeMap();

        // extract all zip files
        ExtractorZipFileVisitor visitor;
        for (File zipFile : this.zipFiles) {
            debug("Extracting zip [" + zipFile + "] entries");
            visitor = new ExtractorZipFileVisitor(this.destDir, this.filesToRealizeRegex, this.templateEngine,
                currentFilesToLeaveAlone);
            ZipUtil.walkZipFile(zipFile, visitor);
            newFileHashCodeMap.putAll(visitor.getFileHashcodeMap());
        }

        // copy all raw files
        StreamCopyDigest copyDigester = new StreamCopyDigest();
        for (Map.Entry<File, File> rawFile : this.rawFiles.entrySet()) {
            // determine where the original file is and where it needs to go
            File currentLocationFile = rawFile.getKey();
            File newLocationFile = rawFile.getValue();
            String newLocationPath = rawFile.getValue().getPath();
            if (currentFilesToLeaveAlone != null && currentFilesToLeaveAlone.contains(newLocationPath)) {
                continue;
            }
            if (!newLocationFile.isAbsolute()) {
                newLocationFile = new File(this.destDir, newLocationFile.getPath());
            }
            File newLocationParentDir = newLocationFile.getParentFile();
            newLocationParentDir.mkdirs();
            if (!newLocationParentDir.isDirectory()) {
                throw new Exception("Failed to create new parent directory for raw file [" + newLocationFile + "]");
            }

            String hashcode;

            if (this.filesToRealizeRegex != null && this.filesToRealizeRegex.matcher(newLocationPath).matches()) {
                debug("Realizing file [" + currentLocationFile + "] to [" + newLocationFile + "]");

                // this entry needs to be realized, do it now in-memory (we assume realizable files will not be large)
                // note: tempateEngine will never be null if we got here
                FileInputStream in = new FileInputStream(currentLocationFile);
                byte[] rawFileContent = StreamUtil.slurp(in);
                String content = this.templateEngine.replaceTokens(new String(rawFileContent));

                // now write the realized content to the filesystem
                byte[] bytes = content.getBytes();

                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(newLocationFile));
                try {
                    out.write(bytes);
                } finally {
                    out.close();
                }

                MessageDigestGenerator hashcodeGenerator = copyDigester.getMessageDigestGenerator();
                hashcodeGenerator.add(bytes);
                hashcode = hashcodeGenerator.getDigestString();
            } else {
                debug("Copying raw file [" + currentLocationFile + "] to [" + newLocationFile + "]");

                FileInputStream in = new FileInputStream(currentLocationFile);
                try {
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(newLocationFile));
                    try {
                        hashcode = copyDigester.copyAndCalculateHashcode(in, out);
                    } finally {
                        out.close();
                    }
                } finally {
                    in.close();
                }
            }

            // remember where the file is now and what its hashcode is
            if (rawFile.getValue().isAbsolute()) {
                newFileHashCodeMap.put(newLocationFile.getAbsolutePath(), hashcode);
            } else {
                newFileHashCodeMap.put(newLocationPath, hashcode);
            }
        }

        this.deploymentsMetadata.setCurrentDeployment(deploymentProps, newFileHashCodeMap);
        return newFileHashCodeMap;
    }

    /**
     * Performs in-memory-only calculations to determine where new files would go and what
     * their new hashcodes would be.
     * 
     * @return map of file/hashcodes if there were to be written to disk
     * @throws Exception
     */
    private FileHashcodeMap getNewDeploymentFileHashcodeMap() throws Exception {
        FileHashcodeMap fileHashcodeMap = new FileHashcodeMap();

        // perform in-memory extraction and calculate hashcodes for all zip files 
        InMemoryZipFileVisitor visitor;
        for (File zipFile : this.zipFiles) {
            debug("Extracting zip [" + zipFile + "] in-memory to determine hashcodes for all entries");
            visitor = new InMemoryZipFileVisitor(this.filesToRealizeRegex, this.templateEngine);
            ZipUtil.walkZipFile(zipFile, visitor);
            fileHashcodeMap.putAll(visitor.getFileHashcodeMap());
        }

        MessageDigestGenerator generator = new MessageDigestGenerator();

        // calculate hashcodes for all raw files, perform in-memory realization when necessary
        for (Map.Entry<File, File> rawFile : this.rawFiles.entrySet()) {
            // determine where the original file is and where it would go if we were writing it to disk
            File currentLocationFile = rawFile.getKey();
            File newLocationFile = rawFile.getValue();
            String newLocationPath = rawFile.getValue().getPath();
            if (!newLocationFile.isAbsolute()) {
                newLocationFile = new File(this.destDir, newLocationFile.getPath());
            }

            String hashcode;

            if (this.filesToRealizeRegex != null && this.filesToRealizeRegex.matcher(newLocationPath).matches()) {
                debug("Realizing file [" + currentLocationFile + "] in-memory to determine its hashcode");

                // this entry needs to be realized, do it now in-memory (we assume realizable files will not be large)
                // note: tempateEngine will never be null if we got here
                FileInputStream in = new FileInputStream(currentLocationFile);
                byte[] rawFileContent = StreamUtil.slurp(in);
                String content = this.templateEngine.replaceTokens(new String(rawFileContent));

                // now calculate the hashcode of the realized content
                generator.add(content.getBytes());
                hashcode = generator.getDigestString();
            } else {
                debug("Streaming file [" + currentLocationFile + "] in-memory to determine its hashcode");

                BufferedInputStream in = new BufferedInputStream(new FileInputStream(currentLocationFile));
                try {
                    hashcode = generator.calcDigestString(in);
                } finally {
                    in.close();
                }
            }

            // store where the file will be and what its hashcode would be
            if (rawFile.getValue().isAbsolute()) {
                fileHashcodeMap.put(newLocationFile.getAbsolutePath(), hashcode);
            } else {
                fileHashcodeMap.put(newLocationPath, hashcode);
            }
        }

        return fileHashcodeMap;
    }

    private void debug(String msg) {
        if (log.isDebugEnabled()) {
            StringBuilder str = new StringBuilder();
            String bundleName = this.deploymentProps.getBundleName();
            String bundleVersion = this.deploymentProps.getBundleVersion();
            int deploymentId = this.deploymentProps.getDeploymentId();
            str.append("Bundle [").append(bundleName).append(" v").append(bundleVersion).append(']');
            str.append("; Deployment [").append(deploymentId).append("]: ");
            str.append(msg);
            log.debug(str.toString());
        }
    }
}
