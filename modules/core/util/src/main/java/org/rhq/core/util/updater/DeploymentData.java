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

    // TODO: After removing the deprecated constructors we should go back to making all of these "final", and
    //       remove the init() method in favor of constructor chaining.
    private DeploymentProperties deploymentProps;
    private Map<File, File> zipFiles;
    private Map<File, File> rawFiles;
    private File destinationDir;
    private File sourceDir;
    private Map<File, Pattern> zipEntriesToRealizeRegex;
    private Set<File> rawFilesToRealize;
    private TemplateEngine templateEngine;
    private Pattern ignoreRegex;
    private Map<File, Boolean> zipsExploded;

    /**
     * Equivalent to calling {@link #DeploymentData(DeploymentProperties, Map, Map, File, File, Map, Set, TemplateEngine, Pattern, boolean, Map)}
     * with the zipFiles Map having as a keySet the Set of Files provided here, and null values for each Map entry.  Also
     * makes a call to to {@link DeploymentProperties#setManageRootDir(boolean)}.
     *
     * @deprecated use {@link #DeploymentData(DeploymentProperties, File, File, Map, Set, Map, Map, TemplateEngine, Pattern, Map)
     */
    @Deprecated
    public DeploymentData(DeploymentProperties deploymentProps, Set<File> zipFiles, Map<File, File> rawFiles,
        File sourceDir, File destinationDir, Map<File, Pattern> zipEntriesToRealizeRegex, Set<File> rawFilesToRealize,
        TemplateEngine templateEngine, Pattern ignoreRegex, boolean manageRootDir, Map<File, Boolean> zipsExploded) {

        Map<File, File> zipFilesMap = (null == zipFiles) ? null : new HashMap<File, File>(zipFiles.size());
        if (null != zipFiles) {
            for (File zipFile : zipFiles) {
                zipFilesMap.put(zipFile, null);
            }
        }

        deploymentProps.setManageRootDir(manageRootDir);
        init(deploymentProps, zipFilesMap, rawFiles, sourceDir, destinationDir, zipEntriesToRealizeRegex,
            rawFilesToRealize, templateEngine, ignoreRegex, zipsExploded);
    }

    /**
      * Equivalent to calling {@link #DeploymentData(DeploymentProperties, Map, Map, File, File, Map, Set, TemplateEngine, Pattern, Map)}
      * with the zipFiles map having as a keySet the Set of Files provided here, and null values for each Map entry.
      *
      * @deprecated use {@link #DeploymentData(DeploymentProperties, File, File, Map, Set, Map, Map, TemplateEngine, Pattern, Map)
      *
      * @since 4.9.0
      */
    @Deprecated
    public DeploymentData(DeploymentProperties deploymentProps, Set<File> zipFiles, Map<File, File> rawFiles,
        File sourceDir, File destinationDir, Map<File, Pattern> zipEntriesToRealizeRegex, Set<File> rawFilesToRealize,
        TemplateEngine templateEngine, Pattern ignoreRegex, Map<File, Boolean> zipsExploded) {

        Map<File, File> zipFilesMap = (null == zipFiles) ? null : new HashMap<File, File>(zipFiles.size());
        if (null != zipFiles) {
            for (File zipFile : zipFiles) {
                zipFilesMap.put(zipFile, null);
            }
        }

        init(deploymentProps, zipFilesMap, rawFiles, sourceDir, destinationDir, zipEntriesToRealizeRegex,
            rawFilesToRealize, templateEngine, ignoreRegex, zipsExploded);
    }

    /**
     * Constructor that prepares this object with the data that is necessary in order to deploy archive/file content
     * a destination directory.
     *
     * @param deploymentProps          metadata about this deployment
     * @param destinationDir           the root directory where the content is to be deployed
     * @param sourceDir                the root directory where the source files (zips and raw files) are located
     * @param rawFiles                 files that are to be copied into the destination directory - the keys are the
     *                                 current
     *                                 locations of the files, the values are where the files should be copied (the
     *                                 values may be relative
     *                                 in which case they are relative to destDir and can have subdirectories and/or a
     *                                 different filename
     *                                 than what the file is named currently)
     * @param rawFilesToRealize        identifies the raw files that need to be realized; note that each item in this
     *                                 set
     *                                 must match a key to a <code>rawFiles</code> entry
     * @param zipFiles                 the archives containing the content to be deployed - the keys are the current
     *                                 locations of the zip files and the values are the destinationDir entries. A null
     *                                 value in the Map entry indicates the zip is to be placed in destinationDir.
     * @param zipEntriesToRealizeRegex the patterns of files (whose paths are relative to destDir) that
     *                                 must have replacement variables within them replaced with values
     *                                 obtained via the given template engine. The key is the name of the zip file
     *                                 that the regex must be applied to - in other words, the regex value is only
     *                                 applied
     *                                 to relative file names as found in their associated zip file.
     * @param templateEngine           if one or more filesToRealize are specified, this template engine is used to
     *                                 determine the values that should replace all replacement variables found in those
     *                                 files.
     * @param ignoreRegex              the files/directories to ignore when updating an existing deployment
     * @param zipsExploded             if not <code>null</code>, this is a map keyed on zip files whose values indicate
     *                                 if the zips should be exploded (true) or remain compressed after the deployment
     *                                 is finished (false). If a zip file is not found in this map, true is the
     *                                 default.
     *
     * @since 4.11
     */
    public DeploymentData(DeploymentProperties deploymentProps, File sourceDir, File destinationDir,
        Map<File, File> rawFiles, Set<File> rawFilesToRealize, Map<File, File> zipFiles,
        Map<File, Pattern> zipEntriesToRealizeRegex, TemplateEngine templateEngine, Pattern ignoreRegex,
        Map<File, Boolean> zipsExploded) {

        init(deploymentProps, zipFiles, rawFiles, sourceDir, destinationDir, zipEntriesToRealizeRegex,
            rawFilesToRealize, templateEngine, ignoreRegex, zipsExploded);
    }

    private void init(DeploymentProperties deploymentProps, Map<File, File> zipFiles, Map<File, File> rawFiles,
        File sourceDir, File destinationDir, Map<File, Pattern> zipEntriesToRealizeRegex, Set<File> rawFilesToRealize,
        TemplateEngine templateEngine, Pattern ignoreRegex, Map<File, Boolean> zipsExploded) {

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
            zipFiles = new HashMap<File, File>(0);
        }
        if (rawFiles == null) {
            rawFiles = new HashMap<File, File>(0);
        }
        if ((zipFiles.size() == 0) && (rawFiles.size() == 0)) {
            throw new IllegalArgumentException("No archives or raw files specified - nothing to do");
        }
        if (zipsExploded == null) {
            zipsExploded = new HashMap<File, Boolean>(0);
        }

        this.deploymentProps = deploymentProps;
        this.zipFiles = zipFiles;
        this.rawFiles = rawFiles;

        //specifically do NOT resolve symlinks here. This must be the last thing one needs to do before deploying
        //the files. The problem is that we use the destination dir as root for the paths of the individual files to
        //lay down. If the destinationDir uses symlinks and the individual paths of the files were relative
        // including ..'s, it could happen that the files would be laid down on a different place than expected.
        //Consider this scenario:
        //destinationDir = /opt/my/destination -> /tmp/deployments
        //file = ../conf/some.properties
        //One expects the file to end up in /opt/my/conf/some.properties
        //but if we canonicalized the destination dir upfront, we'd end up with /tmp/conf/some.properties.
        this.destinationDir = FileUtil.normalizePath(destinationDir.getAbsoluteFile());

        this.sourceDir = sourceDir;
        this.ignoreRegex = ignoreRegex;
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

        // We need to "normalize" all file paths (raw and zip) that have ".." in them to ensure everything works properly.
        // Note that any pathnames that are relative but have ".." paths that end up taking the file
        // above the destination directory need to be normalized and will end up being an absolute path
        // (so all log messages will indicate the full absolute path and if the file needs to be backed up it will be
        // backed up as if it was an external file that was specified with an absolute path). If the relative path has
        // ".." but does not take the file above the destination directory it will simply have its ".."
        // normalized out but will still be a relative path (relative to destination directory) (we can't make it absolute
        // otherwise Deployer's update will run into errors while backing up and scanning for deleted files).
        // See BZs 917085 and 917765.
        for (Map.Entry<File, File> entry : this.rawFiles.entrySet()) {
            File rawFile = entry.getValue();
            if (null != rawFile) {
                String rawFilePath = rawFile.getPath();
                entry.setValue(getSafeFile(rawFile, rawFilePath));
            }
        }

        for (Map.Entry<File, File> entry : this.zipFiles.entrySet()) {
            File zipFile = entry.getValue();
            if (null != zipFile) {
                String zipFilePath = zipFile.getPath();
                entry.setValue(getSafeFile(zipFile, zipFilePath));
            }
        }
    }

    private File getSafeFile(File file, String filePath) {
        // finds "/.." or "../" in the string
        boolean doubledot = filePath.replace('\\', '/').matches(".*((/\\.\\.)|(\\.\\./)).*");
        boolean isWindows = (File.separatorChar == '\\');

        if (doubledot) {
            File fileToNormalize;

            if (file.isAbsolute()) {
                fileToNormalize = file;

            } else {
                if (isWindows) {
                    // of course, Windows has to make it enormously difficult to do this right...

                    // determine if the windows file relative path specified a drive (e.g. C:\foobar.txt)
                    StringBuilder filePathBuilder = new StringBuilder(filePath);
                    String fileDriveLetter = FileUtil.stripDriveLetter(filePathBuilder); // filePathBuilder now has drive letter stripped

                    // determine what, if any, drive letter is specified in the destination directory
                    StringBuilder destDirAbsPathBuilder = new StringBuilder(destinationDir.getAbsolutePath());
                    String destDirDriveLetter = FileUtil.stripDriveLetter(destDirAbsPathBuilder);

                    // figure out what the absolute, normalized path is for the file
                    if ((destDirDriveLetter == null || fileDriveLetter == null)
                        || fileDriveLetter.equals(destDirDriveLetter)) {
                        fileToNormalize = new File(destinationDir, filePathBuilder.toString());
                    } else {
                        throw new IllegalArgumentException("Cannot normalize relative path [" + filePath
                            + "]; its drive letter is different than the destination directory ["
                            + destinationDir.getAbsolutePath() + "]");
                    }
                } else {
                    fileToNormalize = new File(destinationDir, filePath);
                }
            }

            fileToNormalize = getNormalizedFile(fileToNormalize);

            if (isPathUnderBaseDir(destinationDir, fileToNormalize)) {
                // we can keep file path relative, but we need to normalize out the ".." paths
                String baseDir = destinationDir.getAbsolutePath();
                String absFilePath = fileToNormalize.getAbsolutePath();
                String relativePath = absFilePath.substring(baseDir.length() + 1); // should always return a valid path; if not, let it throw exception (which likely means there is a bug here)
                return new File(relativePath);
            } else {
                // file path has ".." such that the file is really above destination dir - use an absolute, canonical path
                return fileToNormalize;
            }
        } else if (isWindows && file != null && file.isAbsolute()) {
            // make sure drive letter is normalized
            return getNormalizedFile(file);
        }

        return file;
    }

    private static File getNormalizedFile(File fileToNormalize) {
        return FileUtil.normalizePath(fileToNormalize);
    }

    public DeploymentProperties getDeploymentProps() {
        return deploymentProps;
    }

    /**
     * @return The Set of zipFiles
     * @deprecated Use {@link #getZipFilesMap()} to fully support the destinationDir attribute on archive deployment.
     */
    @Deprecated
    public Set<File> getZipFiles() {
        return zipFiles.keySet();
    }

    public Map<File, File> getZipFilesMap() {
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

    /**
     * As of RHQ 4.9.0, this calls {@link #getDeploymentProps()}.{@link DeploymentProperties#getManageRootDir() getManageRootDir()}
     *
     * @deprecated use {@link #getDeploymentProps()}.{@link org.rhq.core.util.updater.DeploymentProperties#getDestinationCompliance() getDestinationCompliance()}.
     */
    @Deprecated
    public boolean isManageRootDir() {
        return deploymentProps.getManageRootDir();
    }

    public Map<File, Boolean> getZipsExploded() {
        return zipsExploded;
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
