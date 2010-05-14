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
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.rhq.core.template.TemplateEngine;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * A visitor object that will perform some in-memory work for each zip entry it visits.
 * 
 * @author John Mazzitelli
 */
public class InMemoryZipFileVisitor implements ZipUtil.ZipEntryVisitor {
    private final FileHashcodeMap fileHashcodeMap = new FileHashcodeMap();
    private final Pattern filesToRealizeRegex;
    private final TemplateEngine templateEngine;
    private final MessageDigestGenerator hashcodeGenerator;

    /**
     * Creates the visitor. When the visitor hits a zip entry whose name matches
     * filesToRealizeRegex, that zip entry will be realized via the template engine prior
     * to its hashcode being computed. In other words the file's hashcode will be computed
     * on the content after its replacement variables have been replaced.
     * If you just want this visitor to walk a zip file without realizing any files, pass in
     * a null pattern or pass in a null template engine. This will, in effect,
     * have this visitor collect all zip file entry names and calculate their hashcodes based on
     * all content within the zip file.
     * 
     * @param filesToRealizeRegex pattern of files that are to be realized prior to hashcodes being computed
     * @param templateEngine the template engine that replaces replacement variables in files to be realized
     */
    public InMemoryZipFileVisitor(Pattern filesToRealizeRegex, TemplateEngine templateEngine) {
        if (filesToRealizeRegex == null || templateEngine == null) {
            filesToRealizeRegex = null;
            templateEngine = null;
        }
        this.filesToRealizeRegex = filesToRealizeRegex;
        this.templateEngine = templateEngine;
        this.hashcodeGenerator = new MessageDigestGenerator();
    }

    /**
     * Returns the file/hashcode data this visitor has collected.
     * @return map containing filenames (zip file entry names) and their hashcodes
     */
    public FileHashcodeMap getFileHashcodeMap() {
        return fileHashcodeMap;
    }

    public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {

        if (entry.isDirectory()) {
            return true; // skip directory entries, we only care about the files
        }

        String pathname = entry.getName();
        String hashcode;

        if (this.filesToRealizeRegex != null && this.filesToRealizeRegex.matcher(pathname).matches()) {
            // this entry needs to be realized, do it now, then calc the hashcode 
            // note: tempateEngine will never be null if we got here
            int contentSize = (int) entry.getSize();
            ByteArrayOutputStream baos = new ByteArrayOutputStream((contentSize > 0) ? contentSize : 32768);
            StreamUtil.copy(stream, baos, false);
            String content = this.templateEngine.replaceTokens(baos.toString());
            baos = null;
            hashcode = this.hashcodeGenerator.calcDigestString(content);
        } else {
            hashcode = this.hashcodeGenerator.calcDigestString(stream);
        }

        this.fileHashcodeMap.put(pathname, hashcode);
        return true;
    }
}
