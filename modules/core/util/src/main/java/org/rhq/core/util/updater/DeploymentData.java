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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.rhq.core.template.TemplateEngine;
import org.rhq.core.util.file.FileUtil;

/**
 * Data that describes a particular deployment. In effect, this provides the
 * data needed to fully deploy something.
 * 
 * @author John Mazzitelli
 */
public class DeploymentData {

    private final DeploymentProperties deploymentProps;
    private final Set<File> zipFiles;
    private final Map<File, File> rawFiles;
    private final File destinationDir;
    private final File sourceDir;
    private final Map<File, Pattern> zipEntriesToRealizeRegex;
    private final Set<File> rawFilesToRealize;
    private final TemplateEngine templateEngine;
    private final Pattern ignoreRegex;
    private final boolean manageRootDir;
    private final Map<File, Boolean> zipsExploded;

    /**
     * Constructors that prepares this object with the data that is necessary in order to deploy archive/file content
     * a destination directory.
     *  
     * @param deploymentProps metadata about this deployment
     * @param zipFiles the archives containing the content to be deployed
     * @param rawFiles files that are to be copied into the destination directory - the keys are the current
     *                 locations of the files, the values are where the files should be copied (the values may be relative
     *                 in which case they are relative to destDir and can have subdirectories and/or a different filename
     *                 than what the file is named currently)
     * @param destinationDir the root directory where the content is to be deployed
     * @param sourceDir the root directory where the source files (zips and raw files) are located
     * @param zipEntriesToRealizeRegex the patterns of files (whose paths are relative to destDir) that
     *                                 must have replacement variables within them replaced with values
     *                                 obtained via the given template engine. The key is the name of the zip file
     *                                 that the regex must be applied to - in other words, the regex value is only applied
     *                                 to relative file names as found in their associated zip file.
     * @param rawFilesToRealize identifies the raw files that need to be realized; note that each item in this set
     *                          must match a key to a <code>rawFiles</code> entry
     * @param templateEngine if one or more filesToRealize are specified, this template engine is used to determine
     *                       the values that should replace all replacement variables found in those files
     * @param ignoreRegex the files/directories to ignore when updating an existing deployment
     * @param manageRootDir if false, the top directory where the files will be deployed (i.e. the destinationDir)
     *                      will be left alone. That is, if files already exist there, they will not be removed or
     *                      otherwise merged with this deployment's root files. If true, this top root directory
     *                      will be managed just as any subdirectory within the deployment will be managed.
     *                      The purpose of this is to be able to write files to an existing directory that has other
     *                      unrelated files in it that need to remain intact. e.g. the deploy/ directory of JBossAS. 
     *                      Note: regardless of this setting, all subdirectories under the root dir will be managed.
     * @param zipsExploded if not <code>null</code>, this is a map keyed on zip files whose values indicate
     *                     if the zips should be exploded (true) or remain compressed after the deployment
     *                     is finished (false). If a zip file is not found in this map, true is the default.
     */
    public DeploymentData(DeploymentProperties deploymentProps, Set<File> zipFiles, Map<File, File> rawFiles,
        File sourceDir, File destinationDir, Map<File, Pattern> zipEntriesToRealizeRegex, Set<File> rawFilesToRealize,
        TemplateEngine templateEngine, Pattern ignoreRegex, boolean manageRootDir, Map<File, Boolean> zipsExploded) {

        if (deploymentProps == null) {
            throw new IllegalArgumentException("deploymentProps == null");
        }
        if (destinationDir == null) {
            throw new IllegalArgumentException("destinationDir == null");
        }
        if (sourceDir == null) {
            throw new IllegalArgumentException("sourceDir == null");
        }

        if (zipFiles == null) {
            zipFiles = new HashSet<File>(0);
        }
        if (rawFiles == null) {
            rawFiles = new HashMap<File, File>(0);
        }
        if ((zipFiles.size() == 0) && (rawFiles.size() == 0)) {
            throw new IllegalArgumentException("zipFiles/rawFiles are empty - nothing to do");
        }
        if (zipsExploded == null) {
            zipsExploded = new HashMap<File, Boolean>(0);
        }

        this.deploymentProps = deploymentProps;
        this.zipFiles = zipFiles;
        this.rawFiles = rawFiles;
        this.destinationDir = getCanonicalFile(destinationDir);
        this.sourceDir = sourceDir;
        this.ignoreRegex = ignoreRegex;
        this.manageRootDir = manageRootDir;
        this.zipsExploded = zipsExploded;

        // if there is nothing to realize or we have no template engine to obtain replacement values, then we null things out
        if (templateEngine == null || (zipEntriesToRealizeRegex == null && rawFilesToRealize == null)) {
            this.zipEntriesToRealizeRegex = null;
            this.rawFilesToRealize = null;
            this.templateEngine = null;
        } else {
            this.zipEntriesToRealizeRegex = zipEntriesToRealizeRegex;
            this.rawFilesToRealize = rawFilesToRealize;
            this.templateEngine = templateEngine;
        }

        // We need to "normalize" all raw file paths that have ".." in them to ensure everything works properly.
        // Any raw file pathname (the values in this.rawFiles) that needs to be normalized will be converted to
        // a canonical path. Note that any pathname that is relative but have ".." paths that end up taking the file
        // above the destination directory needs to be normalized and will end up being an absolute path
        // (so all log messages will indicate the full absolute path and if the file
        // needs to be backed up it will be backed up as if it was an external file that was specified with an absolute path).
        // If the relative path has ".." but does not take the file above the destination directory will simply have its ".."
        // normalized out but will still be a relative path (relative to destination directory) (we can't make it absolute
        // otherwise Deployer's update will run into errors while backing up and scanning for deleted files).
        // See BZ 917085.
        for (Map.Entry<File, File> entry : this.rawFiles.entrySet()) {
            File rawFile = entry.getValue();
            String rawFilePath = rawFile.getPath();

            boolean doubledot = rawFilePath.replace('\\', '/').matches(".*((/\\.\\.)|(\\.\\./)).*"); // finds "/.." or "../" in the string

            if (doubledot) {
                File fileToCanonicalize;

                if (rawFile.isAbsolute()) {
                    fileToCanonicalize = rawFile;
                } else {
                    boolean isWindows = (File.separatorChar == '\\');
                    if (isWindows) {
                        // of course, Windows has to make it enormously difficult to do this right...

                        // determine if the windows rawFile relative path specified a drive (e.g. C:foobar.txt)
                        StringBuilder rawFilePathBuilder = new StringBuilder(rawFilePath);
                        String rawFileDriveLetter = FileUtil.stripDriveLetter(rawFilePathBuilder); // rawFilePathBuilder now has drive letter stripped

                        // determine what, if any, drive letter is specified in the destination directory
                        StringBuilder destDirAbsPathBuilder = new StringBuilder(this.destinationDir.getAbsolutePath());
                        String destDirDriveLetter = FileUtil.stripDriveLetter(destDirAbsPathBuilder);

                        // figure out what the absolute, normalized path is for the raw file
                        if ((destDirDriveLetter == null || rawFileDriveLetter == null)
                            || rawFileDriveLetter.equals(destDirDriveLetter)) {
                            fileToCanonicalize = new File(this.destinationDir, rawFilePathBuilder.toString());
                        } else {
                            throw new IllegalArgumentException("Cannot normalize relative path [" + rawFilePath
                                + "]; its drive letter is different than the destination directory ["
                                + this.destinationDir.getAbsolutePath() + "]");
                        }
                    } else {
                        fileToCanonicalize = new File(this.destinationDir, rawFilePath);
                    }
                }

                fileToCanonicalize = getCanonicalFile(fileToCanonicalize);

                if (isPathUnderBaseDir(this.destinationDir, fileToCanonicalize)) {
                    // we can keep rawFile path relative, but we need to normalize out the ".." paths
                    String baseDir = this.destinationDir.getAbsolutePath();
                    String absRawFilePath = fileToCanonicalize.getAbsolutePath();
                    String canonicalRelativePath = absRawFilePath.substring(baseDir.length() + 1); // should always return a valid path; if not, let it throw exception (which likely means there is a bug here)
                    entry.setValue(new File(canonicalRelativePath));
                } else {
                    // raw file path has ".." such that the file is really above destination dir - use an absolute, canonical path
                    entry.setValue(fileToCanonicalize);
                }
            }
        }

        return;
    }

    public DeploymentProperties getDeploymentProps() {
        return deploymentProps;
    }

    public Set<File> getZipFiles() {
        return zipFiles;
    }

    public Map<File, File> getRawFiles() {
        return rawFiles;
    }

    public File getDestinationDir() {
        return destinationDir;
    }

    public File getSourceDir() {
        return sourceDir;
    }

    public Map<File, Pattern> getZipEntriesToRealizeRegex() {
        return zipEntriesToRealizeRegex;
    }

    public Set<File> getRawFilesToRealize() {
        return rawFilesToRealize;
    }

    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }

    public Pattern getIgnoreRegex() {
        return ignoreRegex;
    }

    public boolean isManageRootDir() {
        return manageRootDir;
    }

    public Map<File, Boolean> getZipsExploded() {
        return zipsExploded;
    }

    private File getCanonicalFile(File file) {
        try {
            file = file.getCanonicalFile();
        } catch (Exception e) {
            // ignore this - this really should never happen, but if it does,
            // we want to continue and hope using the non-normalized file is ok;
            file = file.getAbsoluteFile();
        }
        return file;
    }

    private boolean isPathUnderBaseDir(File base, File path) {
        // this method assumes base and path are absolute and canonical
        if (base == null) {
            return false;
        }

        while (path != null) {
            if (base.equals(path)) {
                return true;
            }
            path = path.getParentFile();
        }
        return false;
    }
}
