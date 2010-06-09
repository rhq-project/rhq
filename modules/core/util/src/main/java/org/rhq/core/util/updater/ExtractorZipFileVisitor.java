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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.rhq.core.template.TemplateEngine;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.stream.StreamCopyDigest;
import org.rhq.core.util.stream.StreamUtil;

/**
 * A visitor object that will extract each zip entry it visits, realizing files that
 * need to have their replacement variables replaced.
 *
 * @author John Mazzitelli
 */
public class ExtractorZipFileVisitor implements ZipUtil.ZipEntryVisitor {
    private final FileHashcodeMap fileHashcodeMap = new FileHashcodeMap();
    private final Pattern filesToRealizeRegex;
    private final TemplateEngine templateEngine;
    private final File rootDir;
    private final Set<String> filesToNotExtract;
    private final StreamCopyDigest copierAndHashcodeGenerator;
    private final DeployDifferences diff;
    private final boolean dryRun;

    /**
     * Creates the visitor. When the visitor hits a zip entry whose name matches
     * filesToRealizeRegex set, that zip entry will be realized via the template engine prior
     * to its hashcode being computed and its file created.
     * If you just want this visitor to walk a zip file without realizing any files, pass in
     * a null pattern or pass in a null template engine. This will, in effect,
     * have this visitor extract all file entries as-is.
     *
     * @param rootDir the top level directory where all zip file entries will be extracted to.
     *                In other words, all zip file entries' paths are relative to this directory.
     * @param filesToRealizeRegex pattern of files that are to be realized prior to hashcodes being computed
     * @param templateEngine the template engine that replaces replacement variables in files to be realized
     * @param filesToNotExtract set of files that are not to be extracted from the zip and stored; these are to be skipped
     * @param diff optional object that is told when files are realized
     * @param dryRun if <code>true</code>, this won't actually write files to the filesystem
     */
    public ExtractorZipFileVisitor(File rootDir, Pattern filesToRealizeRegex, TemplateEngine templateEngine,
        Set<String> filesToNotExtract, DeployDifferences diff, boolean dryRun) {

        this.rootDir = rootDir;

        if (filesToRealizeRegex == null || templateEngine == null) {
            filesToRealizeRegex = null;
            templateEngine = null;
        }
        this.filesToRealizeRegex = filesToRealizeRegex;
        this.templateEngine = templateEngine;

        if (filesToNotExtract != null && filesToNotExtract.size() == 0) {
            filesToNotExtract = null;
        }
        this.filesToNotExtract = filesToNotExtract;
        this.copierAndHashcodeGenerator = new StreamCopyDigest();
        this.diff = diff;
        this.dryRun = dryRun;
    }

    /**
     * Returns the file/hashcode data this visitor has collected.
     * @return map containing filenames (zip file entry names) and their hashcodes
     */
    public FileHashcodeMap getFileHashcodeMap() {
        return fileHashcodeMap;
    }

    public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {

        String pathname = entry.getName();

        if (this.filesToNotExtract != null && this.filesToNotExtract.contains(pathname)) {
            return true;
        }

        File entryFile = new File(this.rootDir, pathname);

        if (entry.isDirectory()) {
            if (!dryRun) {
                entryFile.mkdirs();
            }
            return true;
        }

        // make sure all parent directories are created
        if (!dryRun) {
            entryFile.getParentFile().mkdirs();
        }

        String hashcode;

        if (this.filesToRealizeRegex != null && this.filesToRealizeRegex.matcher(pathname).matches()) {
            // this entry needs to be realized, do it now
            // note: tempateEngine will never be null if we got here
            int contentSize = (int) entry.getSize();
            ByteArrayOutputStream baos = new ByteArrayOutputStream((contentSize > 0) ? contentSize : 32768);
            StreamUtil.copy(stream, baos, false);
            String content = this.templateEngine.replaceTokens(baos.toString());
            baos = null;
            if (this.diff != null) {
                this.diff.addRealizedFile(pathname, content);
            }

            // now write the realized content to the filesystem
            byte[] bytes = content.getBytes();

            if (!dryRun) {
                FileOutputStream fos = new FileOutputStream(entryFile);
                try {
                    fos.write(bytes);
                } finally {
                    fos.close();
                }
            }

            MessageDigestGenerator hashcodeGenerator = this.copierAndHashcodeGenerator.getMessageDigestGenerator();
            hashcodeGenerator.add(bytes);
            hashcode = hashcodeGenerator.getDigestString();
        } else {
            if (!dryRun) {
                FileOutputStream fos = new FileOutputStream(entryFile);
                try {
                    hashcode = this.copierAndHashcodeGenerator.copyAndCalculateHashcode(stream, fos);
                } finally {
                    fos.close();
                }
            } else {
                hashcode = MessageDigestGenerator.getDigestString(stream);
            }
        }

        this.fileHashcodeMap.put(pathname, hashcode);
        return true;
    }
}
