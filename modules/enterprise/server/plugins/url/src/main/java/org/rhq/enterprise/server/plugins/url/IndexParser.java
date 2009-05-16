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
package org.rhq.enterprise.server.plugins.url;

import java.io.InputStream;
import java.util.Map;

/**
 * Parses an index file that provides the metadata about content found in a content source.
 * 
 * @author John Mazzitelli
 */
public interface IndexParser {

    /**
     * Given an input stream containing the index data, this will parse that data and return
     * the parsed information.
     *
     * @param indexStream stream containing the index data (it is not necessary for this method to close this)
     * @param contentSource the content source where the index file came from
     *
     * @return map containing the package metadata info, keyed on each package's location
     *
     * @throws Exception if failed to parse the index file
     */
    public Map<String, RemotePackageInfo> parse(InputStream indexStream, UrlSource contentSource) throws Exception;
}
