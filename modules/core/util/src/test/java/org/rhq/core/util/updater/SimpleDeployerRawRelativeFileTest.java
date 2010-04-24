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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * These attempt to mimic the same tests as {@link SimpleDeployerRawFileTest} except it uses
 * relative paths to the dest dir, rather than using absolute paths to external directories.
 *
 * Individually tests these situations for raw, relative path files:
 * 
 * (X, Y, Z, ? represent hashcodes; none means file doesn't exist):
 * 
 *    ORIGINAL CURRENT    NEW   What To Do...
 * a.        X       X      X   New file is installed over current*
 * b.        X       X      Y   New file is installed over current
 * c.        X       Y      X   Current file is left as-is
 * d.        X       Y      Y   New file is installed over current*
 * e.        X       Y      Z   New file is installed over current, current is backed up
 * f.     none       ?      ?   New file is installed over current, current is backed up
 * g.        X    none      ?   New file is installed
 * h.        ?       ?   none   Current file deleted, backed up if different than original
 *
 * (*) means the new and current files will actually be the same content
 * 
 * @author John Mazzitelli
 */
@Test
public class SimpleDeployerRawRelativeFileTest {

    private final String originalContent = "original content";
    private final String originalFileName = "original_file_name.txt";
    private File tmpDir;
    private File deployDir;
    private File sourceRawFile;
    private Map<File, File> sourceRawFiles;
    private DeploymentProperties originalDeployProps;
    private FileHashcodeMap originalFileHashcodeMap;
    private String originalHashcode;
    private File currentFile;
    private String currentRelativePath;
    private DeploymentProperties newDeployProps;
    private DeployDifferences diff;
    private DeploymentsMetadata metadata;

    @BeforeMethod
    public void beforeMethod() throws Exception {
        // tmpDir is where the source files will originally be located
        this.tmpDir = FileUtil.createTempDirectory("simpleDeployer_TMP", ".test", null);
        // deployDir is where the files will be copied - paths are relative to here, metadata will be stored here too
        this.deployDir = FileUtil.createTempDirectory("simpleDeployer", ".test", null);
        this.originalHashcode = MessageDigestGenerator.getDigestString(originalContent);
        this.sourceRawFile = writeFile(originalContent, tmpDir, "original-raw-file.txt");
        this.sourceRawFiles = new HashMap<File, File>(1);
        this.sourceRawFiles.put(sourceRawFile, new File(originalFileName)); // RELATIVE! note we name it different than the source file
        this.originalDeployProps = new DeploymentProperties(1, "simple", "1.0", "original test deployment");
        Deployer deployer = new Deployer(originalDeployProps, null, sourceRawFiles, deployDir, null, null, null);
        this.originalFileHashcodeMap = deployer.deploy(null);
        this.currentFile = sourceRawFiles.get(sourceRawFile);
        this.currentRelativePath = this.currentFile.getPath();

        this.newDeployProps = new DeploymentProperties(2, "simple", "2.0", "new test deployment");
        this.diff = new DeployDifferences();
        this.metadata = new DeploymentsMetadata(this.deployDir);

        // sanity check due to my paranoia
        File currentAbsFile = new File(deployDir, originalFileName);
        assert currentAbsFile.exists();
        assert this.originalHashcode.equals(MessageDigestGenerator.getDigestString(currentAbsFile));
        assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
        assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod() throws Exception {
        FileUtil.purge(this.tmpDir, true);
        FileUtil.purge(this.deployDir, true);
    }

    public void testX_X_X() throws Exception {
        Deployer deployer = new Deployer(newDeployProps, null, sourceRawFiles, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap = deployer.deploy(this.diff);

        // nothing changed!

        assert newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(this.originalContent);
        assert contentHash[1].equals(this.originalHashcode);

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
        assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
    }

    public void testX_X_Y() throws Exception {
        String newContent = "testX_X_Y";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        File newRawFile = writeFile(newContent, tmpDir, "new-content.txt");
        Map<File, File> newRawFiles = new HashMap<File, File>(1);
        newRawFiles.put(newRawFile, this.currentFile);

        Deployer deployer = new Deployer(newDeployProps, null, newRawFiles, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap = deployer.deploy(this.diff);

        // The new file changed the original file. The current file was never touched, so this is a simple upgrade

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(currentRelativePath).equals(newHashcode);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(newContent);
        assert contentHash[1].equals(newHashcode);

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().size() == 1 : this.diff;
        assert this.diff.getChangedFiles().contains(currentRelativePath) : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
        assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
    }

    public void testX_Y_X() throws Exception {
        String newContent = "testX_Y_X";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        writeFile(newContent, this.currentFile);

        Deployer deployer = new Deployer(newDeployProps, null, sourceRawFiles, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap = deployer.deploy(this.diff);

        // very important to understand this - even though the current file is changed, the hashcode
        // stored in the map and the metadata directory is the ORIGINAL hashcode. This is to make it
        // known that the new deployment itself is the same as the original deployment. It is just
        // that we allow the user's manual changes to continue to live on in the filesystem. However,
        // if a newer deployment comes along in the future and changes the new file, this current file
        // must be updated/backed up as appropriate and the only way to know when that happens is if
        // the metadata retains the original/new hashcode and not the current one.

        assert newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(newContent);
        assert contentHash[1].equals(newHashcode);

        // note nothing changed - our current file remains as is
        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
        assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
    }

    public void testX_Y_Y() throws Exception {
        String newContent = "testX_Y_Y";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        writeFile(newContent, this.currentFile);
        File newRawFile = writeFile(newContent, tmpDir, "new-content.txt");
        Map<File, File> newRawFiles = new HashMap<File, File>(1);
        newRawFiles.put(newRawFile, this.currentFile);

        Deployer deployer = new Deployer(newDeployProps, null, newRawFiles, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap = deployer.deploy(this.diff);

        // The new file changed the original, but our current file has already been manually updated
        // to match the new file. Therefore, the current file doesn't have to change its content.
        // Technically, the file could be overwritten, but the content will still be the same.

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(currentRelativePath).equals(newHashcode);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(newContent);
        assert contentHash[1].equals(newHashcode);

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
        assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
    }

    public void testX_Y_Z() throws Exception {
        String newContentY = "testX_Y_Z_YYY";
        writeFile(newContentY, this.currentFile);

        String newContentZ = "testX_Y_Z_ZZZ";
        String newHashcodeZ = MessageDigestGenerator.getDigestString(newContentZ);
        File newRawFile = writeFile(newContentZ, tmpDir, "new-content.txt");
        Map<File, File> newRawFiles = new HashMap<File, File>(1);
        newRawFiles.put(newRawFile, this.currentFile);

        Deployer deployer = new Deployer(newDeployProps, null, newRawFiles, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap = deployer.deploy(this.diff);

        // The new file changed the original, and our current file has been manually updated
        // but that current file's change does not match to new file. Therefore, the current file
        // it out of date. The safest thing to do is backup the current and copy the new file
        // to become the current file.

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(currentRelativePath).equals(newHashcodeZ);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(newContentZ);
        assert contentHash[1].equals(newHashcodeZ);

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().size() == 1 : this.diff;
        assert this.diff.getChangedFiles().contains(currentRelativePath) : this.diff;
        assert this.diff.getBackedUpFiles().size() == 1 : this.diff;
        assert this.diff.getBackedUpFiles().containsKey(currentRelativePath) : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
        assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);

        // verify the backup copy
        File backupFile = new File(this.diff.getBackedUpFiles().get(currentRelativePath));
        assert readFile(backupFile).equals(newContentY) : "did not backup the correct file?";
    }

    public void testNoOriginalNoCurrentWithNew() throws Exception {
        String newContent = "new content";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        String newFileName = "new_filename.new";
        File newRawFile = writeFile(newContent, tmpDir, newFileName);
        Map<File, File> newRawFiles = new HashMap<File, File>(2);
        newRawFiles.put(sourceRawFile, this.currentFile);
        File newDestRawFile = new File(newFileName); // RELATIVE!
        newRawFiles.put(newRawFile, newDestRawFile);

        Deployer deployer = new Deployer(newDeployProps, null, newRawFiles, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap = deployer.deploy(this.diff);

        // the new deployment introduces a new file. This is simple - its just added to the filesystem

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 2;
        assert newFileHashcodeMap.get(currentRelativePath).equals(originalHashcode);
        assert newFileHashcodeMap.get(newFileName).equals(newHashcode);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(originalContent);
        assert contentHash[1].equals(originalHashcode);
        contentHash = getFilenameContentHashcode(newFileName);
        assert contentHash[0].equals(newContent);
        assert contentHash[1].equals(newHashcode);

        assert this.diff.getAddedFiles().size() == 1 : this.diff;
        assert this.diff.getAddedFiles().contains(newFileName) : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
        assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
    }

    public void testNoOriginalWithCurrentWithNew() throws Exception {
        String newContent = "new content";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        String newFileName = "new_filename.new";
        File newRawFile = writeFile(newContent, tmpDir, "new2.txt");
        Map<File, File> newRawFiles = new HashMap<File, File>(2);
        newRawFiles.put(sourceRawFile, this.currentFile);
        File newDestRawFile = new File(newFileName); // RELATIVE!
        newRawFiles.put(newRawFile, newDestRawFile);

        File inTheWayFile = new File(deployDir, newFileName); // puts an existing copy of file in deploy dir
        String inTheWayContent = "this is in the way";
        writeFile(inTheWayContent, inTheWayFile);

        Deployer deployer = new Deployer(newDeployProps, null, newRawFiles, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap = deployer.deploy(this.diff);

        // The new deployment introduces a new file. However, there is already a current file at the new file location.
        // That current file is unknown and in the way - it must be backed up and overwritten.
        // This is considered a "change" not an "addition" since the file system already had the file, it just got changed.

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 2;
        assert newFileHashcodeMap.get(currentRelativePath).equals(originalHashcode);
        assert newFileHashcodeMap.get(newFileName).equals(newHashcode);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(originalContent);
        assert contentHash[1].equals(originalHashcode);
        contentHash = getFilenameContentHashcode(newFileName);
        assert contentHash[0].equals(newContent);
        assert contentHash[1].equals(newHashcode);

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().size() == 1 : this.diff;
        assert this.diff.getChangedFiles().contains(newFileName) : this.diff;
        assert this.diff.getBackedUpFiles().size() == 1 : this.diff;
        assert this.diff.getBackedUpFiles().containsKey(newFileName) : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
        assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);

        // verify the backup copy
        File backupFile = new File(this.diff.getBackedUpFiles().get(newFileName));
        assert readFile(backupFile).equals(inTheWayContent) : "did not backup the correct file?";
    }

    public void testNoCurrent() throws Exception {
        assert new File(deployDir, this.currentFile.getPath()).delete() : "Failed to delete the current file, cannot prepare the test";

        Deployer deployer = new Deployer(newDeployProps, null, sourceRawFiles, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap = deployer.deploy(this.diff);

        // its the same deployment as before, except someone deleted our current file.
        // This adds the new file back (which is the same as the original).

        assert newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(this.originalContent);
        assert contentHash[1].equals(this.originalHashcode);

        assert this.diff.getAddedFiles().size() == 1 : this.diff;
        assert this.diff.getAddedFiles().contains(currentRelativePath) : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
        assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
    }

    public void testNoNew() throws Exception {
        String newContent = "new content";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        String newFileName = "new_filename.new";
        File newRawFile = writeFile(newContent, tmpDir, "new2.txt");
        Map<File, File> newRawFiles = new HashMap<File, File>(1);
        File newDestRawFile = new File(newFileName); // RELATIVE!
        newRawFiles.put(newRawFile, newDestRawFile);

        Deployer deployer = new Deployer(newDeployProps, null, newRawFiles, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap = deployer.deploy(this.diff);

        // The new deployment removes a file that was in the original (it also introduces a new file).
        // There is already a current file at the original file location that is the same as the original, as you would expect. 
        // That current file is to be deleted (since its not in the new deployment) and is not backed up since it is
        // the same as the original.

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(newFileName).equals(newHashcode);
        assert !this.currentFile.exists() : "this should have been deleted";
        String[] contentHash = getFilenameContentHashcode(newFileName);
        assert contentHash[0].equals(newContent);
        assert contentHash[1].equals(newHashcode);

        assert this.diff.getAddedFiles().size() == 1 : this.diff;
        assert this.diff.getAddedFiles().contains(newFileName) : this.diff;
        assert this.diff.getDeletedFiles().size() == 1 : this.diff;
        assert this.diff.getDeletedFiles().contains(currentRelativePath) : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
        assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
    }

    public void testNoNewWithCurrentDifferentThanOriginal() throws Exception {
        String currentContent = "modified content";
        writeFile(currentContent, this.currentFile);

        String newContent = "new content";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        String newFileName = "new_filename.new";
        File newRawFile = writeFile(newContent, tmpDir, "new2.zip");
        Map<File, File> newRawFiles = new HashMap<File, File>(1);
        File newDestRawFile = new File(newFileName); // RELATIVE!
        newRawFiles.put(newRawFile, newDestRawFile);

        Deployer deployer = new Deployer(newDeployProps, null, newRawFiles, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap = deployer.deploy(this.diff);

        // The new deployment removes a file that was in the original (it also introduces a new file).
        // However, there is already a current file at the original file location as you would expect but
        // its different than the original.
        // That current file is to be deleted (since its not in the new deployment) and it must be backed up
        // since it looks modified from the original.

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(newFileName).equals(newHashcode);
        assert !this.currentFile.exists() : "this should have been deleted";
        String[] contentHash = getFilenameContentHashcode(newFileName);
        assert contentHash[0].equals(newContent);
        assert contentHash[1].equals(newHashcode);

        assert this.diff.getAddedFiles().size() == 1 : this.diff;
        assert this.diff.getAddedFiles().contains(newFileName) : this.diff;
        assert this.diff.getDeletedFiles().size() == 1 : this.diff;
        assert this.diff.getDeletedFiles().contains(currentRelativePath) : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().size() == 1 : this.diff;
        assert this.diff.getBackedUpFiles().containsKey(currentRelativePath) : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
        assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);

        // verify the backup copy
        File backupFile = new File(this.diff.getBackedUpFiles().get(currentRelativePath));
        assert readFile(backupFile).equals(currentContent) : "did not backup the correct file?";
    }

    private String[] getOriginalFilenameContentHashcode() throws Exception {
        return getFilenameContentHashcode(new File(deployDir, currentRelativePath).getAbsolutePath());
    }

    private String[] getFilenameContentHashcode(String filename) throws Exception {
        File file = new File(filename);
        if (!file.isAbsolute()) {
            file = new File(deployDir, filename);
        }
        String content = readFile(file);
        String hashcode = MessageDigestGenerator.getDigestString(content);
        String[] contentHash = new String[] { content, hashcode };
        return contentHash;
    }

    private String readFile(File file) throws Exception {
        return new String(StreamUtil.slurp(new FileInputStream(file)));
    }

    private File writeFile(String content, File fileToOverwrite) throws Exception {
        FileOutputStream out = null;

        try {
            if (!fileToOverwrite.isAbsolute()) {
                fileToOverwrite = new File(deployDir, fileToOverwrite.getPath());
            }
            out = new FileOutputStream(fileToOverwrite);
            fileToOverwrite.getParentFile().mkdirs();
            out.write(content.getBytes());
            return fileToOverwrite;
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private File writeFile(String content, File destDir, String fileName) throws Exception {
        File destFile = new File(destDir, fileName);
        return writeFile(content, destFile);
    }
}
