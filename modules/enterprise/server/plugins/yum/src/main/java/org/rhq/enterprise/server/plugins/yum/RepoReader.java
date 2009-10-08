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

import java.io.IOException;
import java.io.InputStream;

/**
 * The RepoReader interface provides the API for object used to read the content of yum repos. This abstraction allows
 * for multiple protocols for interacting with both local and remote repos.
 *
 * @author jortel
 */
interface RepoReader {
    /**
     * Validate that the reader is operational.
     *
     * @throws Exception When not valid.
     */
    void validate() throws Exception;

    /**
     * Open and return an input stream for the specified yum repo resource.
     *
     * @param  path The relative path to the resource.
     *
     * @return The requested stream that <b>must</b> be closed by the caller.
     *
     * @throws IOException On IO error.
     */
    InputStream openStream(String path) throws IOException;
}