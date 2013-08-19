/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * The disk reader is a yum repo reader used to read metadata and bits from an existing yum repo that is located on a
 * filesystem.
 *
 * @author jortel
 */
public class DiskReader extends UrlReader {

    public DiskReader(URL baseUrl) {
        super(baseUrl);
    }

    /**
     * Validate the reader. Validates that the base path is an existing directory that is readable.
     *
     * @throws IOException When <i>baseUrl</i> is not a directory, does not exist, or is not readable.
     */
    @Override
    public void validate() throws IOException, URISyntaxException {
        File file = new File(baseUrl.toURI().getSchemeSpecificPart());
        if (file.exists() && file.canRead() && file.isDirectory()) {
            return; // good
        }

        throw new IOException("Path: '" + baseUrl + "' not found, not a directory or permission denied");
    }
}
