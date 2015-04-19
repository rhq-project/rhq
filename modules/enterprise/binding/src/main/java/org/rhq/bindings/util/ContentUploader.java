/*
 * RHQ Management Platform
 * Copyright (C) 2005-2015 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.bindings.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.content.ContentManagerRemote;

/**
 * Content upload utility.
 * <br>
 * It will send the content in fragments because sending the whole file at once is memory hungry (the CLI is started
 * with low maximum heap by default).
 * <br>
 * See Bug 955363 - Uploading of file content using remote API/CLI requires many times more heap then file size
 * https://bugzilla.redhat.com/show_bug.cgi?id=955363
 *
 * @author Thomas Segismont
 */
public class ContentUploader {
    private static final int SIZE_32K = 1024 * 32;

    private final Subject subject;
    private final ContentManagerRemote contentManager;

    public ContentUploader(Subject subject, ContentManagerRemote contentManager) {
        this.subject = subject;
        this.contentManager = contentManager;
    }

    /**
     * Uploads the file specified by its absolute <code>filename</code>.
     *
     * @param filename absolute path of the file to upload
     *
     * @return a temporary content handle
     * @see org.rhq.enterprise.server.content.ContentManagerRemote#createTemporaryContentHandle(org.rhq.core.domain.auth.Subject)
     * @see ContentManagerRemote#uploadContentFragment(org.rhq.core.domain.auth.Subject, String, byte[], int, int)
     * @throws IllegalArgumentException if <code>filename</code> is empty, if the file does not exist or if it is a
     * directory
     */
    public String upload(String filename) {
        if (StringUtil.isBlank(filename)) {
            throw new IllegalArgumentException("Empty path");
        }
        return upload(new File(filename));
    }

    /**
     * Uploads a file.
     *
     * @param file the file to upload
     *
     * @return a temporary content handle
     * @see org.rhq.enterprise.server.content.ContentManagerRemote#createTemporaryContentHandle(org.rhq.core.domain.auth.Subject)
     * @see ContentManagerRemote#uploadContentFragment(org.rhq.core.domain.auth.Subject, String, byte[], int, int)
     * @throws IllegalArgumentException if <code>file</code> is null, if the file does not exist or if it is a directory
     */
    public String upload(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File is null");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + file.getAbsolutePath());
        }
        if (file.isDirectory()) {
            throw new IllegalArgumentException("File expected, found directory: " + file.getAbsolutePath());
        }
        String temporaryContentHandle = contentManager.createTemporaryContentHandle(subject);
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream, SIZE_32K);
            int len;
            byte[] bytes = new byte[SIZE_32K];
            while ((len = bufferedInputStream.read(bytes, 0, bytes.length)) != -1) {
                contentManager.uploadContentFragment(subject, temporaryContentHandle, bytes, 0, len);
            }
            return temporaryContentHandle;
        } catch (IOException e) {
            throw new RuntimeException("Could not upload content fragment", e);
        } finally {
            StreamUtil.safeClose(fileInputStream);
        }
    }

}
