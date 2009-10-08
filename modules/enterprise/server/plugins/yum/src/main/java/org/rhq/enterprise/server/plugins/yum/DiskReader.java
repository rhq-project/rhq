/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.plugins.yum;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * The disk reader is a yum repo reader used to read metadata and bits from an existing yum repo that is located on a
 * filesystem.
 *
 * @author jortel
 */
public class DiskReader implements RepoReader {
    /**
     * The base or root directory path of a yum repo.
     */
    private final String basepath;

    /**
     * Constructor.
     *
     * @param basepath The base or root directory path of a yum repo.
     */
    public DiskReader(String basepath) {
        this.basepath = basepath;
    }

    /**
     * Validate the reader. Validates that the base path is an existing directory that is readable.
     *
     * @throws Exception When <i>basepath</i> is not a directory, does not exist, or is not readable.
     */
    public void validate() throws Exception {
        File file = new File(basepath);
        if (file.exists() || file.canRead() || file.isDirectory()) {
            return; // good
        }

        throw new Exception("Path: '" + basepath + "' not found, not a directory or permission denied");
    }

    /**
     * Open an input stream to specifed relative path. Prepends the basepath to the <i>path</i> and opens and opens and
     * input stream.
     *
     * @param  path A relative path to a file within the repo.
     *
     * @return An open input stream that <b>must</b> be closed by the caller.
     *
     * @throws IOException On all errors.
     */
    public InputStream openStream(String path) throws IOException {
        InputStream in = new FileInputStream(basepath + "/" + path);
        if (path.endsWith(".gz")) {
            return new GZIPInputStream(in);
        }

        return in;
    }

    @Override
    public String toString() {
        return "basepath: " + basepath;
    }
}