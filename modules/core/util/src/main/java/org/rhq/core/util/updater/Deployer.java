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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.rhq.core.template.TemplateEngine;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * This deploys a bundle of files within a zip archive to a managed directory.
 * 
 * @author John Mazzitelli
 */
public class Deployer {
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
            // the destination dir has not been used to deploy a bundle yet (i.e. this is the first deployment)
            map = initialDeployment();
        }

        return map;
    }

    public void undeploy() throws Exception {
        if (!this.deploymentsMetadata.isManaged()) {
            throw new Exception("There are no managed deployments in [" + this.destDir + "], nothing to undeploy");
        }
    }

    private FileHashcodeMap initialDeployment() throws Exception {
        FileHashcodeMap fileHashcodeMap = new FileHashcodeMap();

        // extract all zip files
        ExtractorZipFileVisitor visitor;
        for (File zipFile : this.zipFiles) {
            visitor = new ExtractorZipFileVisitor(this.destDir, this.filesToRealizeRegex, this.templateEngine);
            ZipUtil.walkZipFile(zipFile, visitor);
            fileHashcodeMap.putAll(visitor.getFileHashcodeMap());
        }

        // copy all raw files
        StreamCopyDigest copyDigester = new StreamCopyDigest();
        for (Map.Entry<File, File> rawFile : this.rawFiles.entrySet()) {
            // determine where the original file is and where it needs to go
            File currentLocationFile = rawFile.getKey();
            File newLocationFile = rawFile.getValue();
            String newLocationPath = rawFile.getValue().getPath();
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
                // this entry needs to be realized, do it now
                // note: tempateEngine will never be null if we got here
                int contentSize = (int) currentLocationFile.length();
                ByteArrayOutputStream baos = new ByteArrayOutputStream((contentSize > 0) ? contentSize : 32768);
                FileInputStream in = new FileInputStream(currentLocationFile);
                StreamUtil.copy(in, baos, true);
                String content = this.templateEngine.replaceTokens(baos.toString());
                baos = null;

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
                fileHashcodeMap.put(newLocationFile.getAbsolutePath(), hashcode);
            } else {
                fileHashcodeMap.put(newLocationPath, hashcode);
            }
        }

        this.deploymentsMetadata.initializeLiveDeployment(deploymentProps, fileHashcodeMap);

        return fileHashcodeMap;
    }
}
