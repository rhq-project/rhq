/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.enterprise.client.script;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.scripting.ScriptSourceProvider;

/**
 * @author Lukas Krejci
 */
public class FileSystemScriptSourceProvider implements ScriptSourceProvider {

    private static final Log LOG = LogFactory.getLog(FileSystemScriptSourceProvider.class);
    private static final String DEFAULT_SCHEME = "file";

    private final String scheme;
    
    public FileSystemScriptSourceProvider() {
        this(DEFAULT_SCHEME);
    }
    
    public FileSystemScriptSourceProvider(String scheme) {
        this.scheme = scheme;
    }
    
    @Override
    public Reader getScriptSource(URI location) {
        String scheme = location.getScheme();

        //return early if we can't handle this URI
        if (scheme == null || !this.scheme.equals(scheme)) {
            return null;
        }

        File f = getFile(location);

        try {
            if (f.exists() && f.isFile() && f.canRead()) {
                return new FileReader(f);
            }
        } catch (FileNotFoundException e) {
            LOG.debug("File '" + f.getAbsolutePath() + "' seems to have disappeared while we were trying to open it.",
                e);
        }

        return null;
    }

    protected File getFile(URI location) {
        String path = location.getPath();

        return new File(path);
    }
}
