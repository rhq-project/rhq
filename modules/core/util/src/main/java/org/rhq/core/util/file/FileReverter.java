/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.core.util.file;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.rhq.core.util.stream.StreamUtil;

/**
 * This will slurp in the original content of a file and store it in memory. If, later, you need to revert
 * that file back to its original content, you can call #revert.
 * 
 * NOTE: do not use this with large files. It will store the content in memory; large files will result in
 * out-of-memory exceptions.
 *
 * @author John Mazzitelli
 *
 */
public class FileReverter {
    private final File file;
    private final byte[] originalContent;

    public FileReverter(File file) {
        this.file = file;
        try {
            this.originalContent = StreamUtil.slurp(new FileInputStream(file));
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve content from file: " + file);
        }
    }

    public void revert() {
        try {
            StreamUtil.copy(new ByteArrayInputStream(this.originalContent), new FileOutputStream(this.file));
        } catch (Exception e) {
            throw new RuntimeException("Failed to revert file: " + file);
        }
    }
}
