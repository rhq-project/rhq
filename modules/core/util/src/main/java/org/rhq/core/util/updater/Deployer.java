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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.ZipUtil.ZipEntryVisitor;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamCopyDigest;
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
 * <tr><td>?</td><td>?</td><td>none</td><td>Current file is backed up and deleted</td></tr>
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
 * before the new file is copied over top the current file. Files that need to be
 * backed up will have its backup copied to new deployment's metadata directory, under
 * the backup subdirectory called {@link DeploymentsMetadata#BACKUP_DIR}. If a file
 * that needs to be backed up is referred to via an absolute path (that is, outside the destination
 * directory), it will be copied to the {@link DeploymentsMetadata#EXT_BACKUP_DIR} directory found
 * in the metadata directory.
 *
 * @author John Mazzitelli
 */
public class Deployer {
    private final Log log = LogFactory.getLog(Deployer.class);

    private final DeploymentData deploymentData;
    private final DeploymentsMetadata deploymentsMetadata;

    /**
     * Constructors that prepares this object to deploy content to a destination on the local file system.
     *
     * @param deploymentData the data needed to know what to do for this deployment
     */
    public Deployer(DeploymentData deploymentData) {
        if (deploymentData == null) {
            throw new IllegalArgumentException("deploymentData == null");
        }

        this.deploymentData = deploymentData;
        this.deploymentsMetadata = new DeploymentsMetadata(deploymentData.getDestinationDir());
        return;
    }

    /**
     * @return information about the particular deployment that this deployer will install.
     */
    public DeploymentData getDeploymentData() {
        return deploymentData;
    }

    /**
     * @return <code>true</code> if the deployer is to install the deployment data in a directory
     *         that already has a managed deployment in it. <code>false</code> if there is currently
     *         no managed deployments in the destination directory.
     */
    public boolean isDestinationDirectoryManaged() {
        return deploymentsMetadata.isManaged();
    }

    /**
     * Convienence method that is equivalent to {@link #deploy(DeployDifferences, boolean) deploy(diff, false)}.
     * @see #deploy(DeployDifferences, boolean)
     */
    public FileHashcodeMap deploy(DeployDifferences diff) throws Exception {
        return deploy(diff, false, false);
    }

    /**
     * Convienence method that is equivalent to {@link #deploy(DeployDifferences, boolean) deploy(diff, true)}.
     * @see #deploy(DeployDifferences, boolean)
     */
    public FileHashcodeMap dryRun(DeployDifferences diff) throws Exception {
        return deploy(diff, false, true);
    }

    /**
     * Deploys all files to their destinations. Everything this method has to do is determined
     * by the data passed into this object's constructor.
     *
     * This method allows one to ask for a dry run to be performed; meaning don't actually change
     * the file system, just populate the <code>diff</code> object with what would have been done.
     *
     * The caller can ask that an existing deployment directory be cleaned (i.e. wiped from the file system)
     * before the new deployment is laid down. This is useful if there are ignored files/directories
     * that would be left alone as-is, had a clean not been requested. Note that the <code>clean</code>
     * parameter is ignored if a dry run is being requested.
     *
     * @param diff this method will populate this object with information about what
     *             changed on the file system due to this deployment
     * @param clean if <code>true</code>, the caller is telling this method to first wipe clean
     *              any existing deployment files found in the destination directory before installing the
     *              new deployment. Note that this will have no effect if <code>dryRun</code>
     *              is <code>true</code> since a dry run by definition never affects the file system.
     * @param dryRun if <code>true</code>, the file system won't actually be changed; however,
     *               the <code>diff</code> object will still be populated with information about
     *               what would have occurred on the file system had it not been a dry run
     * @return file/hashcode information for all files that were deployed
     * @throws Exception if the deployment failed for some reason
     */
    public FileHashcodeMap deploy(DeployDifferences diff, boolean clean, boolean dryRun) throws Exception {
        FileHashcodeMap map = null;

        // fail-fast if we don't have enough disk space
        checkDiskUsage();

        if (!this.deploymentsMetadata.isManaged()) {
            // the destination dir has not been used to deploy a bundle yet, therefore, this is the first deployment
            map = performInitialDeployment(diff, dryRun);
        } else {
            // we have metadata in the destination directory, therefore, this deployment is updating a current one
            map = performUpdateDeployment(diff, clean, dryRun);
        }

        return map;
    }

    /**
     * This will first perform a deploy (e.g. {@link #deploy(DeployDifferences, boolean, boolean) deploy(diff, clean, dryRun)})
     * and then, if there are backup files from the previous deployment, those backup files will be restored to their
     * original locations.
     *
     * This is useful when you want to "undeploy" something where "undeploy" infers you want to go back to
     * how the file system looked previously to a subsequent deployment (the one this method is being told
     * to "redeploy"), including manual changes that were made over top the previous deployment.
     *
     * For example, suppose you deployed deployment ID #1 and then the user manually changed some files
     * within that #1 deployment. Later on, you deploy deployment ID #2 (which will not only deploy
     * #2's files but will also backup the files that were manually changed within deployment #1).
     * You find that deployment #2 is bad and you want to revert back to how the file system looked
     * previously. You could opt to rollback to deployment ID #1 minus those manual changes - to do
     * this you simply {@link #deploy(DeployDifferences) deploy} #1 again. However, if you want to
     * go back to the previous content including those manual changes, you first deploy #1 and
     * then restore the backup files - essentially overlaying the manual changes over top #1 files. This
     * method accomplishes that latter task.
     *
     * @param diff see {@link #deploy(DeployDifferences, boolean, boolean)}
     * @param clean see {@link #deploy(DeployDifferences, boolean, boolean)}
     * @param dryRun see {@link #deploy(DeployDifferences, boolean, boolean)}
     * @return see {@link #deploy(DeployDifferences, boolean, boolean)}
     * @throws Exception if either the deployment or backup file restoration failed
     */
    public FileHashcodeMap redeployAndRestoreBackupFiles(DeployDifferences diff, boolean clean, boolean dryRun)
        throws Exception {
        FileHashcodeMap map = null;

        // fail-fast if we don't have enough disk space
        checkDiskUsage();

        if (!this.deploymentsMetadata.isManaged()) {
            // the destination dir has not been used to deploy a bundle yet, therefore, this is the first deployment
            map = performInitialDeployment(diff, dryRun);
        } else {
            // we have metadata in the destination directory, therefore, this deployment is updating a current one.
            // First get the ID of the currently existing deployment - this is where our backup files exist.
            // Then we update the current deployment with the new deployment (which actually should be restoring to the previous one).
            // Finally, we restore the backup files into the new deployment, overlaying them over top the new deployment.
            int id = this.deploymentsMetadata.getCurrentDeploymentProperties().getDeploymentId();
            map = performUpdateDeployment(diff, clean, dryRun);
            restoreBackupFiles(id, map, diff, dryRun);
            if (!dryRun) {
                // if we restored one or more files, we need to persist the new deployment hashcode data with the restored hashcodes
                if (!diff.getRestoredFiles().isEmpty()) {
                    this.deploymentsMetadata.setCurrentDeployment(this.deploymentData.getDeploymentProps(), map, false);
                }
            }
        }

        return map;
    }

    /**
     * Returns an estimated amount of disk space the deployment will need if it gets installed.
     * @return information on the estimated disk usage
     * @throws Exception if cannot determine the estimated disk usage
     */
    public DeploymentDiskUsage estimateDiskUsage() throws Exception {
        final DeploymentDiskUsage diskUsage = new DeploymentDiskUsage();

        File partition = this.deploymentData.getDestinationDir();
        long usableSpace = partition.getUsableSpace();
        while (usableSpace == 0L && partition != null) {
            partition = partition.getParentFile();
            if (partition != null) {
                usableSpace = partition.getUsableSpace();
            }
        }

        diskUsage.setMaxDiskUsable(usableSpace);

        Map<File, File> zipFiles = this.deploymentData.getZipFiles();
        for (File zipFile : zipFiles.keySet()) {
            ZipUtil.walkZipFile(zipFile, new ZipEntryVisitor() {
                public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {
                    if (!entry.isDirectory()) {
                        final long size = entry.getSize();
                        diskUsage.increaseDiskUsage(size > 0 ? size : 0);
                        diskUsage.incrementFileCount();
                    }
                    return true;
                }
            });
        }

        Map<File, File> rawFiles = this.deploymentData.getRawFiles();
        for (File rawFile : rawFiles.keySet()) {
            diskUsage.increaseDiskUsage(rawFile.length());
            diskUsage.incrementFileCount();
        }

        return diskUsage;
    }

    /**
     * This will get an {@link #estimateDiskUsage() estimate} of how much disk space the deployment will need
     * and compare it to the amount of estimated disk space is currently usable. If there does not appear to be
     * enough usable disk space to fit the deployment, this method will thrown an exception. Otherwise, this
     * method will simply return normally.
     *
     * This can be used to fail-fast a deployment - there is no need to process the deployment if there is not
     * enough disk space to start with.
     *
     * @throws Exception if there does not appear to be enough disk space to store the deployment content
     */
    public void checkDiskUsage() throws Exception {
        DeploymentDiskUsage usage;

        try {
            usage = estimateDiskUsage();
        } catch (Exception e) {
            debug("Cannot estimate disk usage - will assume there is enough space and will continue. Cause: ", e);
            return;
        }

        debug("Estimated disk usage for this deployment is [", usage.getDiskUsage(), "] bytes (file count=[",
            usage.getFileCount(), "]). The maximum disk space currently usable is estimated to be [",
            usage.getMaxDiskUsable(), "] bytes.");

        if (usage.getDiskUsage() > usage.getMaxDiskUsable()) {
            throw new Exception(
                "There does not appear to be enough disk space for this deployment. Its estimated disk usage ["
                    + usage.getDiskUsage() + "] is larger than the estimated amount of usable disk space ["
                    + usage.getMaxDiskUsable() + "].");
        }

        return;
    }

    private FileHashcodeMap performInitialDeployment(DeployDifferences diff, boolean dryRun) throws Exception {
        switch (deploymentData.getDeploymentProps().getDestinationCompliance()) {
        case full: {
            // We are to fully manage the deployment dir, so we need to delete everything we find there.
            // Any old files do not belong here - only our bundle files should live here now.
            File dir = this.deploymentData.getDestinationDir();
            backupAndPurgeDirectory(diff, dir, dryRun, null);
        }
            break;
        case filesAndDirectories: {
            // We are not to manage files in the root deployment directory. However, we always manage
            // subdirectories that the bundle wants to deploy. So look in subdirectories that our bundles
            // plan to use and remove files that found there.
            // Note that we do the same thing here as we did above for the root dest dir, only we do it
            // for all the immediate subdirectories that our bundle wants to manage.
            Set<File> managedSubdirs = findManagedSubdirectories();
            for (File managedSubdir : managedSubdirs) {
                File dir = new File(this.deploymentData.getDestinationDir(), managedSubdir.getPath());
                backupAndPurgeDirectory(diff, dir, dryRun, managedSubdir.getPath() + File.separatorChar);
            }
        }
            break;
        default:
            throw new IllegalStateException("Unsupported destination compliance mode.");
        }

        FileHashcodeMap newFileHashcodeMap = extractZipAndRawFiles(new HashMap<String, String>(0), diff, dryRun);

        // this is an initial deployment, so we know every file is new - tell our diff about them all
        if (diff != null) {
            diff.addAddedFiles(newFileHashcodeMap.keySet());
        }

        debug("Initial deployment finished. dryRun=", dryRun);
        return newFileHashcodeMap;
    }

    private FileHashcodeMap performUpdateDeployment(DeployDifferences diff, boolean clean, boolean dryRun)
        throws Exception {
        debug("Analyzing original, current and new files as part of update deployment. dryRun=", dryRun);

        // NOTE: remember that data that goes in diff are only things affecting the current file set such as:
        //       * if a current file will change
        //       * if a current file will be deleted
        //       * if a new file is added to the filesystem
        //       * if a current file is ignored in the latest rescan
        //       * if a file is realized on the filesystem before its stored on the file system
        //       * if a current file is backed up
        boolean reportNewRootFilesAsNew = this.deploymentData.getDeploymentProps().getDestinationCompliance() == DestinationComplianceMode.full;
        FileHashcodeMap original = this.deploymentsMetadata.getCurrentDeploymentFileHashcodes();

        // we need fill original files with directory entries
        // because we're handling directories as well
        // but we don't store them in file-hascodes.dat file (and even if we started storing them now
        // we cannot expect them being there from previous bundle deployments)
        putDirectoryEntries(original);

        ChangesFileHashcodeMap current = original.rescan(this.deploymentData.getDestinationDir(),
            this.deploymentData.getIgnoreRegex(), reportNewRootFilesAsNew);
        FileHashcodeMap newFiles = getNewDeploymentFileHashcodeMap();

        if (current.getUnknownContent() != null) {
            throw new Exception("Failed to properly rescan the current deployment: " + current.getUnknownContent());
        }

        // lets fill new files map with directory entries
        putDirectoryEntries(newFiles);

        if (diff != null) {
            diff.addIgnoredFiles(current.getIgnored());
            for (Map.Entry<String, String> entry : current.entrySet()) {
                String currentPath = entry.getKey();
                String currentHashcode = entry.getValue();
                if (currentHashcode.equals(FileHashcodeMap.DELETED_FILE_HASHCODE)) {
                    if (newFiles.containsKey(currentPath)) {
                        diff.addAddedFile(currentPath);
                    }
                } else if (!newFiles.containsKey(currentPath)) {
                    diff.addDeletedFile(currentPath);
                } else if (!newFiles.get(currentPath).equals(currentHashcode)) {
                    if (!newFiles.get(currentPath).equals(original.get(currentPath))) {
                        diff.addChangedFile(currentPath);
                    }
                }
            }
            for (Map.Entry<String, String> entry : newFiles.entrySet()) {
                if (!current.containsKey(entry.getKey())) {
                    diff.addAddedFile(entry.getKey());
                }
            }
        }

        // backup the current file if either of these are true:
        // * if there is a current file but there was no original file (current.getAdditions())
        // * if the current, original and new files are all different from one another (current.getChanges() compared to new)
        //
        // do nothing (do not backup the current file and do not copy the new file to disk) if:
        // * the original file is not the same as the current file (current.getChanges()) but is the same as the new file
        NavigableSet<String> currentFilesToBackup = new TreeSet<String>(current.getAdditions().keySet());
        Map<String, String> currentFilesToLeaveAlone = new HashMap<String, String>();
        NavigableSet<String> currentFilesToDelete;

        for (Map.Entry<String, String> changed : current.getChanges().entrySet()) {
            String changedFilePath = changed.getKey();
            String newHashcode = newFiles.get(changedFilePath);
            if (newHashcode != null) {
                // Remember, original hash will never be the same as the changed hash (that's why it is considered changed!).
                // If the new file hash is the same as the original, it means the user changed the file but that change
                // should be compatible with the new deployment (this is assumed, but if the new file didn't alter the original file,
                // presumably the new software is backward compatible and should accept the changed original as a valid file).
                // Now however in this case we mark the changed file path with the *original* hashcode - in effect, we continue
                // along the knowlege of the original hashcode (which is the same as the new) thus continuing the knowledge
                // that a file is different from the deployment distribution.
                // If the new is different from the original and if the new is different than the current, we need to backup the
                // current because we will be overwriting the current file with the new.
                // Note that if we were told to "clean", we never leave files alone (since everything must be cleaned) but
                // we will backup the touched file that is being removed
                String changedFileHashcode = changed.getValue();
                String originalFileHashcode = original.get(changedFilePath);
                if (newHashcode.equals(originalFileHashcode) && !clean) {
                    currentFilesToLeaveAlone.put(changedFilePath, originalFileHashcode);
                } else if (!newHashcode.equals(changedFileHashcode)) {
                    currentFilesToBackup.add(changedFilePath);
                }
            } else {
                // the current file is a changed version of original, but there is no new file
                // we should back this up since it will be deleted but was changed from the original for some reason
                currentFilesToBackup.add(changedFilePath);
            }
        }

        // done with this, allow GC to reclaim memory
        original = null;

        // the rescan can only find things in the dest dir.
        // if the new deployment introduced new files with absolute paths ('new' meaning they
        // were not in the original deployment), we need to see if that new absolute path
        // already exists; if it does, we need to back that file up.
        Set<String> newNotFoundByRescan = new HashSet<String>(newFiles.keySet());
        newNotFoundByRescan.removeAll(current.keySet());
        for (String newFileNotScanned : newNotFoundByRescan) {
            File newFileNotScannedFile = new File(newFileNotScanned);
            if (newFileNotScannedFile.isAbsolute()) {
                if (newFileNotScannedFile.exists()) {
                    currentFilesToBackup.add(newFileNotScanned);
                    if (diff != null) {
                        diff.removeAddedFile(newFileNotScanned);
                        diff.addChangedFile(newFileNotScanned);
                    }
                }
            }
        }

        // done with this, allow GC to reclaim memory
        newNotFoundByRescan = null;

        // now remove all the new files from the current map - what's left are files that exist that are not in the new deployment
        // thus, those files left in current are those that need to be deleted from disk
        currentFilesToDelete = new TreeSet<String>(current.keySet()); // the set is backed by the map, changes to it affect the map
        currentFilesToDelete.removeAll(newFiles.keySet());
        currentFilesToDelete.removeAll(current.getDeletions().keySet()); // these are already deleted, no sense trying to delete them again

        // remember what files were skipped so we don't delete them during our purge below (only care about this if we are going to 'clean')
        Set<String> skippedFiles = (clean) ? current.getSkipped() : null;

        // don't use this anymore - its underlying key set has been altered and this no longer is the full current files
        current = null;

        // we now know what to do:
        // 1. backup the files in currentFilesToBackup
        // 2. delete the files in currentFilesToDelete
        // 3. copy all new files in newFiles except for those files that are also in currentFilesToLeaveAlone

        // 1. backup the files we want to retain for the admin to review
        if (!currentFilesToBackup.isEmpty()) {
            DeploymentProperties props = this.deploymentData.getDeploymentProps(); // changed from: deploymentsMetadata.getCurrentDeploymentProperties
            int backupDeploymentId = props.getDeploymentId();
            debug("Backing up files as part of update deployment. dryRun=", dryRun);
            // currentFilesToBackup is ordered, so we go backwards in order to delete/backup
            // files laying deeper in folders first ..then folders
            for (Iterator<String> it = currentFilesToBackup.descendingIterator(); it.hasNext();) {
                String fileToBackupPath = it.next();
                boolean toBeDeleted = currentFilesToDelete.remove(fileToBackupPath);
                backupFile(diff, backupDeploymentId, fileToBackupPath, dryRun, toBeDeleted);
            }
        }

        // 2. delete the obsolete files
        if (!currentFilesToDelete.isEmpty()) {
            debug("Deleting obsolete files as part of update deployment. dryRun=", dryRun);
            // currentFilesToDelete is ordered, so we go backwards in order to delete
            // files laying deeper in folders first ..then folders
            for (Iterator<String> it = currentFilesToDelete.descendingIterator(); it.hasNext();) {
                String fileToDeletePath = it.next();
                File doomedFile = new File(fileToDeletePath);
                if (!doomedFile.isAbsolute()) {
                    doomedFile = new File(this.deploymentData.getDestinationDir(), fileToDeletePath);
                }
                boolean deleted;
                if (!dryRun) {
                    deleted = doomedFile.delete();
                } else {
                    deleted = true;
                }
                if (deleted) {
                    debug("Deleted obsolete file [", doomedFile, "]. dryRun=", dryRun);
                } else {
                    // TODO: what should we do? is it a major failure if we can't remove obsolete files?
                    debug("Failed to delete obsolete file [", doomedFile, "]");
                    if (diff != null) {
                        diff.addError(fileToDeletePath, "File [" + doomedFile.getAbsolutePath() + "] did not delete");
                    }
                }
            }
        }

        // 3. copy all new files except for those to be kept as-is (perform a clean first, if requested)
        if (clean) {
            debug("Cleaning the existing deployment's files found in the destination directory. dryRun=", dryRun);
            if (!dryRun) {
                purgeFileOrDirectory(this.deploymentData.getDestinationDir(), skippedFiles, 0, false);
            }
        }
        diff.setCleaned(clean);

        debug("Copying new files as part of update deployment. dryRun=", dryRun);
        FileHashcodeMap newFileHashCodeMap = extractZipAndRawFiles(currentFilesToLeaveAlone, diff, dryRun);

        debug("Update deployment finished. dryRun=", dryRun);
        return newFileHashCodeMap;
    }

    /**
     * adds directory entries to FilehashcodeMap. Whenever there is a file laying in a directory, this directory (and it's parents)
     * will be added to map as items
     * @param original
     */
    private void putDirectoryEntries(FileHashcodeMap map) {
        Map<String, String> missingDirs = new HashMap<String, String>();
        for (Iterator<String> it = map.keySet().iterator(); it.hasNext();) {
            File keyFile = new File(it.next());
            if (!keyFile.isAbsolute()) {
                File parent = null;
                while ((parent = keyFile.getParentFile()) != null) {
                    missingDirs.put(parent.getPath(), FileHashcodeMap.DIRECTORY_HASHCODE);
                    keyFile = parent;
                }
            }
        }
        map.putAll(missingDirs);
    }

    private Set<File> findManagedSubdirectories() throws Exception {
        final Set<File> managedSubdirs = new HashSet<File>();

        // get top-most subdirectory for any raw file that is to be deployed in a subdirectory under the deploy dir
        for (File rawFile : this.deploymentData.getRawFiles().values()) {
            File parentDir = getTopMostParentDirectory(rawFile);
            if (parentDir != null) {
                managedSubdirs.add(parentDir);
            }
        }

        // Loop through each zip and get all top-most subdirectories that the exploded zip files will have.
        Set<File> zipFilesToAnalyze = new HashSet<File>(this.deploymentData.getZipFiles().keySet());
        // We only have to do this analysis for those zips that we explode - remove from the list those we won't explode
        Iterator<File> iter = zipFilesToAnalyze.iterator();
        while (iter.hasNext()) {
            File zipFile = iter.next();
            // if the zip file is not found in the zips-exploded map (get()==null), we assume true (i.e. we will explode it)
            // if the zip file is found in the zips-exploded map, see if we will explode it (i.e see if the boolean is true)
            if (Boolean.FALSE.equals(this.deploymentData.getZipsExploded().get(zipFile))) {
                iter.remove(); // we won't be exploding this zip, we can skip it from the analysis
            }
        }
        for (File zipFileToAnalyze : zipFilesToAnalyze) {
            ZipUtil.walkZipFile(zipFileToAnalyze, new ZipEntryVisitor() {
                @Override
                public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {
                    String relativePath = entry.getName();
                    int firstPathSep = relativePath.indexOf('/'); // regardless of platform, zip always uses /
                    if (firstPathSep != -1) {
                        String topParentDir = relativePath.substring(0, firstPathSep);
                        managedSubdirs.add(new File(topParentDir));
                    }
                    return true;
                }
            });
        }

        return managedSubdirs;
    }

    /**
     * @param file a file with a relative path - null will be returned for files with absolute paths
     * @return the top-most parent for the given file - null if there is no parent file or file is null itself
     */
    private File getTopMostParentDirectory(File file) {
        if (file != null && !file.isAbsolute() && file.getParentFile() != null) {
            File parentDir = file.getParentFile();
            while (parentDir.getParentFile() != null) { // walk up the parents to get to the top most one
                parentDir = parentDir.getParentFile();
            }
            return parentDir;
        }
        return null;
    }

    private void backupAndPurgeDirectory(DeployDifferences diff, File dir, boolean dryRun, String relativeTo)
        throws Exception {
        log.info(buildLogMessage("Will be managing the directory [", dir,
            "]; backing up and purging any obsolete content existing in there"));
        if (dir.isDirectory()) {
            int deploymentId = this.deploymentData.getDeploymentProps().getDeploymentId();
            backupFiles(diff, deploymentId, dir, dryRun, relativeTo, true);
            if (!dryRun) {
                // we want to purge everything that is originally in here, but we backed up the files in here
                // so make sure we don't delete our metadata directory, which is where the backed up files are
                File[] doomedFiles = dir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return !DeploymentsMetadata.METADATA_DIR.equals(name);
                    }
                });
                for (File doomedFile : doomedFiles) {
                    FileUtil.purge(doomedFile, true);
                }
            }
        }
        return;
    }

    private void backupFile(DeployDifferences diff, int deploymentId, final String fileToBackupPath, boolean dryRun,
        boolean removeFileToBackup) throws Exception {

        File bakFile;

        // we need to play some games if we are on windows and a drive letter is specified
        boolean isWindows = (File.separatorChar == '\\');
        StringBuilder fileToBackupPathNoDriveLetter = null;
        String driveLetter = null;

        if (isWindows) {
            fileToBackupPathNoDriveLetter = new StringBuilder(fileToBackupPath);
            driveLetter = FileUtil.stripDriveLetter(fileToBackupPathNoDriveLetter);
        }

        File fileToBackup = new File(fileToBackupPath);
        if (fileToBackup.isAbsolute()) {
            File backupDir = this.deploymentsMetadata.getDeploymentExternalBackupDirectory(deploymentId);
            if (isWindows && driveLetter != null) {
                String dirName = this.deploymentsMetadata.getExternalBackupDirectoryNameForWindows(driveLetter);
                backupDir = new File(backupDir, dirName);
                bakFile = new File(backupDir, fileToBackupPathNoDriveLetter.toString());
            } else {
                bakFile = new File(backupDir, fileToBackupPath);
            }
        } else {
            File backupDir = this.deploymentsMetadata.getDeploymentBackupDirectory(deploymentId);
            if (isWindows && driveLetter != null) {
                StringBuilder destDirAbsPathBuilder = new StringBuilder(this.deploymentData.getDestinationDir()
                    .getAbsolutePath());
                String destDirDriveLetter = FileUtil.stripDriveLetter(destDirAbsPathBuilder);
                if (destDirDriveLetter == null || driveLetter.equals(destDirDriveLetter)) {
                    bakFile = new File(backupDir, fileToBackupPath);
                    fileToBackup = new File(this.deploymentData.getDestinationDir(),
                        fileToBackupPathNoDriveLetter.toString());
                } else {
                    throw new Exception("Cannot backup relative path [" + fileToBackupPath
                        + "] whose drive letter is different than the destination directory ["
                        + this.deploymentData.getDestinationDir().getAbsolutePath() + "]");
                }
            } else {
                bakFile = new File(backupDir, fileToBackupPath);
                fileToBackup = new File(this.deploymentData.getDestinationDir(), fileToBackupPath);
            }
        }

        boolean deleted = false; // will be true if we were told to delete the file and we actually did delete it

        if (!dryRun) {
            bakFile.getParentFile().mkdirs();
            // try to do a rename first if we are to remove the file, since this is more likely
            // much faster and more efficient.
            // if it fails (perhaps because we are crossing file systems), try a true copy
            if (removeFileToBackup) {
                boolean movedSuccessfully = fileToBackup.renameTo(bakFile);
                if (movedSuccessfully) {
                    deleted = true;
                } else {
                    if (fileToBackup.isDirectory()) {
                        bakFile.mkdir();
                    } else {
                        FileUtil.copyFile(fileToBackup, bakFile);
                    }
                    deleted = fileToBackup.delete();
                    if (deleted == false) {
                        // TODO: what should we do? is it a major failure if we can't remove files here?
                        debug("Failed to delete file [", fileToBackup, "] but it is backed up");
                        if (diff != null) {
                            diff.addError(fileToBackupPath, "File [" + fileToBackup.getAbsolutePath()
                                + "] did not delete");
                        }
                    }
                }
            } else {
                if (fileToBackup.isDirectory()) {
                    bakFile.mkdir();
                } else {
                    FileUtil.copyFile(fileToBackup, bakFile);
                }
            }
        } else {
            deleted = removeFileToBackup; // this is a dry run, pretend we deleted it if we were asked to
        }

        debug("Backed up file [", fileToBackup, "] to [", bakFile, "]. dryRun=", dryRun);
        if (deleted) {
            debug("Deleted file [", fileToBackup, "] after backing it up. dryRun=", dryRun);
        }

        if (diff != null) {
            diff.addBackedUpFile(fileToBackupPath, bakFile.getAbsolutePath());
            if (deleted) {
                diff.addDeletedFile(fileToBackupPath);
            }
        }

        return;
    }

    private void backupFiles(DeployDifferences diff, int deploymentId, File dirToBackup, boolean dryRun,
        String relativeTo, boolean removeSourceFiles) throws Exception {
        File[] files = dirToBackup.listFiles();
        if (files == null) {
            throw new IOException("Failed to get the list of files in source directory [" + dirToBackup + "]");
        }
        if (files.length > 0) {
            this.deploymentsMetadata.getMetadataDirectory().mkdirs(); // make sure we create this, might not be there yet
            for (File file : files) {
                if (file.isDirectory()) {
                    if (file.getName().equals(DeploymentsMetadata.METADATA_DIR)) {
                        continue; // skip the RHQ metadata directory, its where we are putting our backups!
                    }
                    backupFiles(diff, deploymentId, file, dryRun,
                        ((relativeTo == null) ? "" : relativeTo) + file.getName() + File.separatorChar,
                        removeSourceFiles);
                } else {
                    backupFile(diff, deploymentId, ((relativeTo == null) ? "" : relativeTo) + file.getName(), dryRun,
                        removeSourceFiles);
                }
            }

            files = null; // help GC
        }
    }

    private FileHashcodeMap extractZipAndRawFiles(Map<String, String> currentFilesToLeaveAlone, DeployDifferences diff,
        boolean dryRun) throws Exception {

        // NOTE: right now, this only adds "diffs" to the "realized" file set, no need to track "added" or "changed" here
        FileHashcodeMap newFileHashCodeMap = new FileHashcodeMap();

        // get information about the source dir - will be needed if we were told to not explode a zip
        String sourceDirAbsPath = this.deploymentData.getSourceDir().getAbsolutePath();
        int sourceDirLength = sourceDirAbsPath.length();

        // extract all zip files
        ExtractorZipFileVisitor visitor;
        for (Map.Entry<File, File> zipFileEntry : this.deploymentData.getZipFiles().entrySet()) {
            File zipFile = zipFileEntry.getKey();

            Boolean exploded = this.deploymentData.getZipsExploded().get(zipFile);
            if (exploded == null) {
                exploded = Boolean.TRUE; // the default is to explode the archive
            }

            debug("Extracting zip [", zipFile, "] entries. exploded=", exploded, ", dryRun=", dryRun);

            Pattern realizeRegex = null;
            if (this.deploymentData.getZipEntriesToRealizeRegex() != null) {
                realizeRegex = this.deploymentData.getZipEntriesToRealizeRegex().get(zipFile);
            }

            if (exploded.booleanValue()) {
                // EXPLODED

                visitor = new ExtractorZipFileVisitor(this.deploymentData.getDestinationDir(), realizeRegex,
                    this.deploymentData.getTemplateEngine(), currentFilesToLeaveAlone.keySet(), diff, dryRun);
                ZipUtil.walkZipFile(zipFile, visitor);
                newFileHashCodeMap.putAll(visitor.getFileHashcodeMap()); // exploded into individual files

            } else {
                // COMPRESSED

                File destinationDir = zipFileEntry.getValue();
                File compressedFile = null;
                String zipPath = null;

                if (null == destinationDir) {
                    // Note: there is a requirement that all zip files must be located in the sourceDir - this is why. We
                    // need the path of the zip relative to the source dir so we can copy it to the same relative location
                    // under the destination dir. Without doing this, if the zip is in a subdirectory, we won't know where to
                    // put it under the destination dir.
                    String zipRelativePath = zipFile.getAbsolutePath().substring(sourceDirLength);
                    if (zipRelativePath.startsWith("/") || zipRelativePath.startsWith("\\")) {
                        zipRelativePath = zipRelativePath.substring(1);
                    }
                    compressedFile = new File(this.deploymentData.getDestinationDir(), zipRelativePath);
                    zipPath = zipRelativePath;

                } else {
                    if (destinationDir.isAbsolute()) {
                        compressedFile = new File(destinationDir, zipFile.getName());
                        zipPath = compressedFile.getPath();
                    } else {
                        zipPath = new File(destinationDir, zipFile.getName()).getPath();
                        compressedFile = new File(this.deploymentData.getDestinationDir(), zipPath);
                    }
                }

                if (this.deploymentData.getTemplateEngine() != null && realizeRegex != null) {
                    // we need to explode it to perform the realization of templatized variables
                    // TODO: can we do this in another tmp location and build the zip in the dest dir?
                    visitor = new ExtractorZipFileVisitor(this.deploymentData.getDestinationDir(), realizeRegex,
                        this.deploymentData.getTemplateEngine(), currentFilesToLeaveAlone.keySet(), diff, dryRun);
                    ZipUtil.walkZipFile(zipFile, visitor);
                    // we have to compress the file again - our new compressed file will have the new realized files in them
                    if (!dryRun) {
                        createZipFile(compressedFile, this.deploymentData.getDestinationDir(),
                            visitor.getFileHashcodeMap());
                    }
                }

                // Copy the archive to the destination dir if we need to. Generate its hashcode and add it to the new file hashcode map
                MessageDigestGenerator hashcodeGenerator = new MessageDigestGenerator();
                String compressedFileHashcode;
                if (!dryRun) {
                    if (!compressedFile.exists()) {
                        if (compressedFile.getParentFile() != null) {
                            compressedFile.getParentFile().mkdirs();
                        }
                        FileUtil.copyFile(zipFile, compressedFile);
                    }
                    compressedFileHashcode = hashcodeGenerator.calcDigestString(compressedFile);
                } else {
                    // use source zip for hash - should be the same as the would-be compressed file since we aren't realizing files in it
                    compressedFileHashcode = hashcodeGenerator.calcDigestString(zipFile);
                }
                newFileHashCodeMap.put(zipPath, compressedFileHashcode);
            }
        }

        // copy all raw files
        StreamCopyDigest copyDigester = new StreamCopyDigest();
        for (Map.Entry<File, File> rawFile : this.deploymentData.getRawFiles().entrySet()) {
            // determine where the original file is and where it needs to go
            File currentLocationFile = rawFile.getKey();
            File newLocationFile = rawFile.getValue();
            String newLocationPath = rawFile.getValue().getPath();
            newLocationPath = newFileHashCodeMap.convertPath(newLocationPath);
            if (currentFilesToLeaveAlone != null && currentFilesToLeaveAlone.containsKey(newLocationPath)) {
                continue;
            }
            if (!newLocationFile.isAbsolute()) {
                newLocationFile = new File(this.deploymentData.getDestinationDir(), newLocationFile.getPath());
            }

            if (!dryRun) {
                File newLocationParentDir = newLocationFile.getParentFile();
                newLocationParentDir.mkdirs();
                if (!newLocationParentDir.isDirectory()) {
                    throw new Exception("Failed to create new parent directory for raw file [" + newLocationFile + "]");
                }
            }

            String hashcode;

            boolean realize = false;
            if (this.deploymentData.getRawFilesToRealize() != null) {
                realize = this.deploymentData.getRawFilesToRealize().contains(currentLocationFile);
            }

            if (realize) {
                debug("Realizing file [", currentLocationFile, "] to [", newLocationFile, "]. dryRun=", dryRun);

                // this entry needs to be realized, do it now in-memory (we assume realizable files will not be large)
                // note: tempateEngine will never be null if we got here
                FileInputStream in = new FileInputStream(currentLocationFile);
                byte[] rawFileContent = StreamUtil.slurp(in);
                String content = this.deploymentData.getTemplateEngine().replaceTokens(new String(rawFileContent));

                if (diff != null) {
                    diff.addRealizedFile(newLocationPath, content);
                }

                // now write the realized content to the filesystem
                byte[] bytes = content.getBytes();

                if (!dryRun) {
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(newLocationFile));
                    try {
                        out.write(bytes);
                    } finally {
                        out.close();
                    }
                }

                MessageDigestGenerator hashcodeGenerator = copyDigester.getMessageDigestGenerator();
                hashcodeGenerator.add(bytes);
                hashcode = hashcodeGenerator.getDigestString();
            } else {
                debug("Copying raw file [", currentLocationFile, "] to [", newLocationFile, "]. dryRun=", dryRun);

                FileInputStream in = new FileInputStream(currentLocationFile);
                try {
                    if (!dryRun) {
                        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(newLocationFile));
                        try {
                            hashcode = copyDigester.copyAndCalculateHashcode(in, out);
                        } finally {
                            out.close();
                        }
                    } else {
                        hashcode = MessageDigestGenerator.getDigestString(in);
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

        newFileHashCodeMap.putAll(currentFilesToLeaveAlone); // remember that these are still there

        if (!dryRun) {
            this.deploymentsMetadata.setCurrentDeployment(this.deploymentData.getDeploymentProps(), newFileHashCodeMap,
                true);
        }

        return newFileHashCodeMap;
    }

    /**
     * Create a zip file by adding all the files found in the file hashcode map. The
     * relative paths found in the map's key set are relative to the rootDir directory.
     * The files are stored in the given zipFile.
     *
     * @param zipFile where to zip up all the files
     * @param rootDir all relative file paths are relative to this root directory
     * @param fileHashcodeMap the key set tells us all the files that need to be zipped up
     *
     * @throws Exception
     */
    private void createZipFile(File zipFile, File rootDir, FileHashcodeMap fileHashcodeMap) throws Exception {
        if (zipFile.getParentFile() != null) {
            zipFile.getParentFile().mkdirs();
        }

        Set<File> childrenOfRootToDelete = new HashSet<File>();

        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipFile));

            for (String relativeFileToZip : fileHashcodeMap.keySet()) {
                File fileToZip = new File(rootDir, relativeFileToZip);
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(fileToZip);
                    ZipEntry zipEntry = new ZipEntry(relativeFileToZip);
                    zos.putNextEntry(zipEntry);
                    StreamUtil.copy(fis, zos, false);
                } finally {
                    if (fis != null) {
                        fis.close();
                    }

                    // Remember the location to delete it later. To speed up the deletes, we only remember
                    // the locations of the direct children under the rootDir. Since we know all zip content
                    // is located under those locations, we'll recursively delete everything under those
                    // rootDir children. We obviously can't delete everything under rootDir because there
                    // could be files in there not related to our zip file.
                    File childOfRoot = fileToZip;
                    while (childOfRoot != null) {
                        File parent = childOfRoot.getParentFile();
                        if (parent.equals(rootDir)) {
                            childrenOfRootToDelete.add(childOfRoot);
                            break;
                        } else {
                            childOfRoot = parent;
                        }
                    }
                }
            }

        } finally {
            if (zos != null) {
                zos.close();
            }
        }

        // we are done zipping up all files, delete all the individual files that are exploded that are now in the zip
        for (File childOfRootToDelete : childrenOfRootToDelete) {
            FileUtil.purge(childOfRootToDelete, true);
        }

        return;
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
        for (File zipFile : this.deploymentData.getZipFiles().keySet()) {
            debug("Extracting zip [", zipFile, "] in-memory to determine hashcodes for all entries");
            Pattern realizeRegex = null;
            if (this.deploymentData.getZipEntriesToRealizeRegex() != null) {
                realizeRegex = this.deploymentData.getZipEntriesToRealizeRegex().get(zipFile);
            }
            visitor = new InMemoryZipFileVisitor(realizeRegex, this.deploymentData.getTemplateEngine());
            ZipUtil.walkZipFile(zipFile, visitor);
            fileHashcodeMap.putAll(visitor.getFileHashcodeMap());
        }

        MessageDigestGenerator generator = new MessageDigestGenerator();

        // calculate hashcodes for all raw files, perform in-memory realization when necessary
        for (Map.Entry<File, File> rawFile : this.deploymentData.getRawFiles().entrySet()) {
            // determine where the original file is and where it would go if we were writing it to disk
            File currentLocationFile = rawFile.getKey();
            File newLocationFile = rawFile.getValue();
            String newLocationPath = rawFile.getValue().getPath();
            if (!newLocationFile.isAbsolute()) {
                newLocationFile = new File(this.deploymentData.getDestinationDir(), newLocationFile.getPath());
            }

            String hashcode;

            boolean realize = false;
            if (this.deploymentData.getRawFilesToRealize() != null) {
                realize = this.deploymentData.getRawFilesToRealize().contains(currentLocationFile);
            }

            if (realize) {
                debug("Realizing file [", currentLocationFile, "] in-memory to determine its hashcode");

                // this entry needs to be realized, do it now in-memory (we assume realizable files will not be large)
                // note: tempateEngine will never be null if we got here
                FileInputStream in = new FileInputStream(currentLocationFile);
                byte[] rawFileContent = StreamUtil.slurp(in);
                String content = this.deploymentData.getTemplateEngine().replaceTokens(new String(rawFileContent));

                // now calculate the hashcode of the realized content
                generator.add(content.getBytes());
                hashcode = generator.getDigestString();
            } else {
                debug("Streaming file [", currentLocationFile, "] in-memory to determine its hashcode");

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

    /**
     * Takes all backup files found in a previous deployment and restores those files to their
     * original locations.
     *
     * This method is usually called after a {@link #deploy(DeployDifferences, boolean)} has been completed
     * because it would be at that time when the previous deployment's information has been persisted
     * and is available. Rarely will you ever want to restore backup files without first deploying
     * content.
     *
     * @param prevDeploymentId the previous deployment ID which contains the backup files
     * @param map the map containing filenames/hashcodes that will be adjusted to reflect the restored file hashcodes
     * @param diff used to store information about the restored files
     * @param dryRun if <code>true</code>, don't really restore the files, but log the files that would
     *               have been restored; <code>false</code> means you really want to restore the files
     * @throws Exception
     */
    private void restoreBackupFiles(int prevDeploymentId, FileHashcodeMap map, DeployDifferences diff, boolean dryRun)
        throws Exception {

        // do the relative backup files first - these go into the destination directory
        File backupDir = this.deploymentsMetadata.getDeploymentBackupDirectory(prevDeploymentId);
        String backupDirBase = backupDir.getAbsolutePath();
        if (!backupDirBase.endsWith(File.separator)) {
            backupDirBase += File.separator; // make sure it ends with a file separator so our string manipulation works
        }
        File destDir = this.deploymentData.getDestinationDir();
        restoreBackupFilesRecursive(backupDir, backupDirBase, destDir, map, diff, dryRun);

        // now do the external backup files - these go into directories external to the destination directory
        // note that if we are on windows, we have to do special things due to the existence of multiple root
        // directories (i.e. drive letters).
        Map<String, File> extBackupDirs;
        extBackupDirs = this.deploymentsMetadata.getDeploymentExternalBackupDirectoriesForWindows(prevDeploymentId);
        if (extBackupDirs == null) {
            // we are on a non-windows platform; there is only one main root directory - "/"
            File extBackupDir = this.deploymentsMetadata.getDeploymentExternalBackupDirectory(prevDeploymentId);
            extBackupDirs = new HashMap<String, File>(1);
            extBackupDirs.put("/", extBackupDir);
        }

        for (Map.Entry<String, File> entry : extBackupDirs.entrySet()) {
            String rootDir = entry.getKey();
            File extBackupDir = entry.getValue();
            String extBackupDirBase = extBackupDir.getAbsolutePath();
            if (!extBackupDirBase.endsWith(File.separator)) {
                extBackupDirBase += File.separator;
            }
            restoreExternalBackupFilesRecursive(rootDir, extBackupDir, extBackupDirBase, map, diff, dryRun);
        }

        return;
    }

    private void restoreBackupFilesRecursive(File file, String base, File destDir, FileHashcodeMap map,
        DeployDifferences diff, boolean dryRun) throws Exception {

        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    restoreBackupFilesRecursive(child, base, destDir, map, diff, dryRun);
                } else {
                    String childRelativePath = child.getAbsolutePath().substring(base.length());
                    //if (this.deploymentData.isManageRootDir() || new File(childRelativePath).getParent() != null) {
                    File restoredFile = new File(destDir, childRelativePath);
                    debug("Restoring backup file [", child, "] to [", restoredFile, "]. dryRun=", dryRun);
                    if (!dryRun) {
                        restoredFile.getParentFile().mkdirs();
                        String hashcode = copyFileAndCalcHashcode(child, restoredFile);
                        map.put(childRelativePath, hashcode);
                    } else {
                        map.put(childRelativePath, MessageDigestGenerator.getDigestString(child));
                    }
                    diff.addRestoredFile(childRelativePath, child.getAbsolutePath());
                    //} else {
                    //    debug("Skipping the restoration of the backed up file [", childRelativePath,
                    //        "] since this deployment was told to not manage the root directory");
                    //}
                }
            }
        }
        return;
    }

    private void restoreExternalBackupFilesRecursive(String rootDir, File backupDir, String base, FileHashcodeMap map,
        DeployDifferences diff, boolean dryRun) throws Exception {

        File[] children = backupDir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    restoreExternalBackupFilesRecursive(rootDir, child, base, map, diff, dryRun);
                } else {
                    String childRelativePath = child.getAbsolutePath().substring(base.length());
                    File restoredFile = new File(rootDir, childRelativePath);
                    debug("Restoring backup file [", child, "] to external location [", restoredFile, "]. dryRun="
                        + dryRun);
                    if (!dryRun) {
                        restoredFile.getParentFile().mkdirs();
                        String hashcode = copyFileAndCalcHashcode(child, restoredFile);
                        map.put(restoredFile.getAbsolutePath(), hashcode);
                    } else {
                        map.put(restoredFile.getAbsolutePath(), MessageDigestGenerator.getDigestString(child));
                    }
                    diff.addRestoredFile(restoredFile.getAbsolutePath(), child.getAbsolutePath());
                }
            }
        }
        return;
    }

    private String copyFileAndCalcHashcode(File src, File dest) throws Exception {
        BufferedInputStream is = null;
        BufferedOutputStream os = null;
        try {
            is = new BufferedInputStream(new FileInputStream(src));
            os = new BufferedOutputStream(new FileOutputStream(dest));
            StreamCopyDigest copier = new StreamCopyDigest();
            return copier.copyAndCalculateHashcode(is, os);
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (Exception ignore) {
            }

            try {
                if (os != null)
                    os.close();
            } catch (Exception ignore) {
            }
        }
    }

    private void purgeFileOrDirectory(File fileOrDir, Set<String> skippedFiles, int level, boolean deleteIt) {
        // make sure we only purge deployment files, never the metadata directory or its files
        // we also want to leave all skipped files alone - don't delete those since they are unrelated to our deployment
        if (fileOrDir != null && !fileOrDir.getName().equals(DeploymentsMetadata.METADATA_DIR)) {
            if (fileOrDir.isDirectory()) {
                File[] doomedFiles = fileOrDir.listFiles();
                if (doomedFiles != null) {
                    for (File doomedFile : doomedFiles) {
                        // Do not purge any skipped files - we want to skip them.
                        // All our skipped files are always at the top root dir (level 0),
                        // so we can ignore the skipped set if we are at levels 1 or below since there are no skipped files down there
                        if (level != 0 || !skippedFiles.contains(doomedFile.getName())) {
                            purgeFileOrDirectory(doomedFile, skippedFiles, level + 1, true); // call this method recursively
                        }
                    }
                }
            }

            if (deleteIt) {
                fileOrDir.delete();
            }
        }

        return;
    }

    private void debug(Object... objs) {
        if (log.isDebugEnabled()) {
            log.debug(buildLogMessage(objs));
        }
    }

    private String buildLogMessage(Object... objs) {
        String bundleName = this.deploymentData.getDeploymentProps().getBundleName();
        String bundleVersion = this.deploymentData.getDeploymentProps().getBundleVersion();
        int deploymentId = this.deploymentData.getDeploymentProps().getDeploymentId();
        StringBuilder str = new StringBuilder();
        str.append("Bundle [").append(bundleName).append(" v").append(bundleVersion).append(']');
        str.append("; Deployment [").append(deploymentId).append("]: ");
        for (Object o : objs) {
            str.append(o);
        }
        return str.toString();
    }
}
