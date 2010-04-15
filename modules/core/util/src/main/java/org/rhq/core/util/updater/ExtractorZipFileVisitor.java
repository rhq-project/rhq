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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.rhq.core.template.TemplateEngine;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * A visitor object that will extract each zip entry it visits, realizing files that
 * need to have their replacement variables replaced.
 * 
 * @author John Mazzitelli
 */
public class ExtractorZipFileVisitor implements ZipUtil.ZipEntryVisitor {
    private final FileHashcodeMap fileHashcodeMap = new FileHashcodeMap();
    private final Set<String> filesToRealize;
    private final TemplateEngine templateEngine;
    private final File rootDir;

    /**
     * Creates the visitor. When the visitor hits a zip entry whose name matches one in
     * the filesToRealize set, that zip entry will be realized via the template engine prior
     * to its hashcode being computed and its file created.
     * If you just want this visitor to walk a zip file without realizing any files, pass in
     * a null or empty set of files or pass in a null template engine. This will, in effect,
     * have this visitor extract all file entries as-is.
     * 
     * @param rootDir the top level directory where all zip file entries will be extracted to.
     *                In other words, all zip file entries' paths are relative to this directory.
     * @param filesToRealize set of files that are to be realized prior to hashcodes being computed
     * @param templateEngine the template engine that replaces replacement variables in files to be realized
     */
    public ExtractorZipFileVisitor(File rootDir, Set<String> filesToRealize, TemplateEngine templateEngine) {
        this.rootDir = rootDir;

        if (filesToRealize == null || filesToRealize.size() == 0 || templateEngine == null) {
            filesToRealize = null;
            templateEngine = null;
        }
        this.filesToRealize = filesToRealize;
        this.templateEngine = templateEngine;
    }

    /**
     * Returns the file/hashcode data this visitor has collected.
     * @return map containing filenames (zip file entry names) and their hashcodes
     */
    public FileHashcodeMap getFileHashcodeMap() {
        return fileHashcodeMap;
    }

    @Override
    public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {

        String pathname = entry.getName();
        File entryFile = new File(this.rootDir, pathname);

        if (entry.isDirectory()) {
            entryFile.mkdirs();
            return true;
        }

        if (this.filesToRealize != null && this.filesToRealize.contains(pathname)) {
            // this entry needs to be realized, do it now
            // note: tempateEngine will never be null if we got here
            int contentSize = (int) entry.getSize();
            ByteArrayOutputStream baos = new ByteArrayOutputStream((contentSize > 0) ? contentSize : 32768);
            StreamUtil.copy(stream, baos, false);
            String content = this.templateEngine.replaceTokens(baos.toString());

            // now write the realized content to the filesystem
            FileOutputStream fos = new FileOutputStream(entryFile);
            try {
                fos.write(content.getBytes());
            } finally {
                fos.close();
            }
        } else {
            FileOutputStream fos = new FileOutputStream(entryFile);
            try {
                StreamUtil.copy(stream, fos, false);
            } finally {
                fos.close();
            }
        }

        String hashcode = MessageDigestGenerator.getDigestString(entryFile);
        this.fileHashcodeMap.put(pathname, hashcode);
        return true;
    }
}
