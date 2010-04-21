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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.rhq.core.template.TemplateEngine;
import org.rhq.core.util.stream.StreamUtil;

/**
 * This class will realize a zip file entry in memory. Note that this object is
 * intended to be used for small zip file entries (like configuration files).
 * Do not attempt to use this on large entries, as out-of-memory errors will
 * be sure to follow. Note that this doesn't limit the size of the zip file itself,
 * the only size restriction is on the size of the zip entry inside the zip file.
 *
 * @author John Mazzitelli
 */
public class InMemoryZipEntryRealizer {
    private final File file;
    private final TemplateEngine templateEngine;

    /**
     * This object will realize entries found in the given zip file using
     * replacement values provided by the given template engine.
     * 
     * @param zipFile the zip file where the entries are to be found
     * @param templateEngine the engine used to replace the replacement variables in the zip entry.
     *                       if this is <code>null</code>, this realizer object will only extract
     *                       the zip file content but will not actually realize any replacement variables within
     *                       that zip file content
     */
    public InMemoryZipEntryRealizer(File zipFile, TemplateEngine templateEngine) {
        this.file = zipFile;
        this.templateEngine = templateEngine;
    }

    /**
     * Returns a string containing the content of the zip entry with its content realized; meaning
     * all replacement variables have been replaced with values provided by the template engine.
     *
     * @param zipEntryName the zip entry that is to be extracted in memory and realized 
     * 
     * @return the realized content of the zip entry
     *
     * @throws Exception 
     */
    public String realize(String zipEntryName) throws Exception {
        ZipFile zipFile = new ZipFile(this.file);
        try {
            ZipEntry zipEntry = zipFile.getEntry(zipEntryName);
            // slurp the content of the zip entry into memory - if this is a large file, watch out for OOMs
            String content = new String(StreamUtil.slurp(zipFile.getInputStream(zipEntry)));
            if (this.templateEngine != null) {
                content = this.templateEngine.replaceTokens(content);
            }
            return content;
        } finally {
            zipFile.close();
        }
    }
}
